package net.mineacle.core.teams.listener;

import net.kyori.adventure.text.Component;
import net.mineacle.core.Core;
import net.mineacle.core.teams.gui.TeamConfirmGui;
import net.mineacle.core.teams.gui.TeamsMainGui;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.metadata.FixedMetadataValue;

public final class TeamsGuiListener implements Listener {

    private static final String META_TEAM_ACTION = "mt_action";
    private static final String META_TEAM_CONFIRM = "mt_confirm";

    private final Core core;
    private final TeamService teamService;
    private final TeamInviteService inviteService;
    private final TeamHomeService teamHomeService;

    public TeamsGuiListener(Core core, TeamService teamService, TeamInviteService inviteService, TeamHomeService teamHomeService) {
        this.core = core;
        this.teamService = teamService;
        this.inviteService = inviteService;
        this.teamHomeService = teamHomeService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        int slot = event.getRawSlot();

        if (title.equals(TeamsMainGui.NO_TEAM_TITLE)) {
            event.setCancelled(true);

            if (slot == 11) {
                player.closeInventory();
                player.sendMessage("§7Use §d/team create <name> §7to create a team.");
                return;
            }

            if (slot == 15) {
                player.closeInventory();

                if (inviteService.hasInvite(player.getUniqueId())) {
                    player.sendMessage("§7You have a pending invite.");
                    player.sendMessage("§7Use §d/team accept §7or §d/team deny");
                } else {
                    player.sendMessage("§7You do not have a pending invite.");
                }
            }

            return;
        }

        if (title.equals(TeamsMainGui.TEAM_TITLE)) {
            event.setCancelled(true);

            TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
            if (team == null) {
                player.closeInventory();
                return;
            }

            switch (slot) {
                case 45 -> {
                    player.closeInventory();

                    org.bukkit.Location home = teamHomeService.getTeamHome(team.teamId());
                    if (home == null) {
                        player.sendMessage("§cYour team does not have a home set.");
                        return;
                    }

                    player.teleport(home);
                    player.sendMessage("§7Teleported to §dTeam Home");
                }
                case 46 -> {
                    if (!teamService.isAdmin(player.getUniqueId())) {
                        player.sendMessage("§cYou do not have permission to toggle friendly fire.");
                        return;
                    }

                    boolean newValue = !team.friendlyFire();
                    teamService.setFriendlyFire(team.teamId(), newValue);
                    player.sendMessage(newValue ? "§cFriendly fire enabled." : "§aFriendly fire disabled.");
                    Bukkit.getScheduler().runTask(core, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                }
                case 47 -> {
                    player.closeInventory();
                    player.sendMessage("§7Use §d/team invite <player>");
                }
                case 51 -> {
                    if (teamService.isFounder(player.getUniqueId())) {
                        player.setMetadata(META_TEAM_ACTION, new FixedMetadataValue(core, "DISBAND:" + team.teamId()));
                        player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
                        TeamConfirmGui.openDisband(player, team.name());
                    } else {
                        player.setMetadata(META_TEAM_ACTION, new FixedMetadataValue(core, "LEAVE:" + team.teamId()));
                        player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
                        TeamConfirmGui.openLeave(player, team.name());
                    }
                }
                case 53 -> {
                    if (!teamService.isFounder(player.getUniqueId())) {
                        return;
                    }

                    if (!teamHomeService.hasTeamHome(team.teamId())) {
                        player.sendMessage("§cYour team does not have a home set.");
                        return;
                    }

                    player.setMetadata(META_TEAM_ACTION, new FixedMetadataValue(core, "DELETE_HOME:" + team.teamId()));
                    player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
                    TeamConfirmGui.openDeleteHome(player);
                }
                default -> {
                }
            }
            return;
        }

        if (title.equals(TeamConfirmGui.LEAVE_TITLE)
                || title.equals(TeamConfirmGui.DISBAND_TITLE)
                || title.equals(TeamConfirmGui.DELETE_HOME_TITLE)) {
            event.setCancelled(true);
            handleConfirmClick(player, slot);
        }
    }

    private void handleConfirmClick(Player player, int slot) {
        if (!player.hasMetadata(META_TEAM_ACTION)) {
            player.closeInventory();
            return;
        }

        String actionValue = player.getMetadata(META_TEAM_ACTION).get(0).asString();
        boolean confirmed = player.hasMetadata(META_TEAM_CONFIRM)
                && player.getMetadata(META_TEAM_CONFIRM).get(0).asBoolean();

        if (slot == 11) {
            clearConfirmMeta(player);
            player.closeInventory();

            TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
            if (team != null) {
                TeamsMainGui.open(core, player, teamService, inviteService);
            }

            player.sendActionBar(Component.text("§cAction cancelled."));
            player.sendMessage("§cAction cancelled.");
            return;
        }

        if (slot != 15) {
            return;
        }

        if (!confirmed) {
            player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, true));
            player.sendActionBar(Component.text("§cClick confirm again to continue."));
            player.sendMessage("§cClick confirm again to continue.");

            core.getServer().getScheduler().runTaskLater(core, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (!player.hasMetadata(META_TEAM_ACTION) || !player.hasMetadata(META_TEAM_CONFIRM)) {
                    return;
                }

                String currentAction = player.getMetadata(META_TEAM_ACTION).get(0).asString();
                boolean currentConfirmed = player.getMetadata(META_TEAM_CONFIRM).get(0).asBoolean();

                if (currentAction.equals(actionValue) && currentConfirmed) {
                    player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
                    player.sendActionBar(Component.text("§cAction confirmation timed out."));
                    player.sendMessage("§cAction confirmation timed out.");
                }
            }, 20L * 5);

            return;
        }

        clearConfirmMeta(player);
        player.closeInventory();

        String[] split = actionValue.split(":", 2);
        if (split.length != 2) {
            return;
        }

        String action = split[0];
        String teamId = split[1];

        switch (action) {
            case "LEAVE" -> {
                if (!teamService.isFounder(player.getUniqueId())) {
                    teamService.removeMember(player.getUniqueId());
                    player.sendMessage("§cYou left your team.");
                }
            }
            case "DISBAND" -> {
                if (teamService.isFounder(player.getUniqueId())) {
                    teamService.disbandTeam(teamId);
                    player.sendMessage("§cTeam disbanded.");
                }
            }
            case "DELETE_HOME" -> {
                if (teamService.isFounder(player.getUniqueId())) {
                    teamHomeService.deleteTeamHome(teamId);
                    player.sendMessage("§cTeam Home deleted.");
                }
            }
            default -> {
            }
        }
    }

    private void clearConfirmMeta(Player player) {
        player.removeMetadata(META_TEAM_ACTION, core);
        player.removeMetadata(META_TEAM_CONFIRM, core);
    }
}