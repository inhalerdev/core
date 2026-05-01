package net.mineacle.core.teams.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.mineacle.core.Core;
import net.mineacle.core.common.gui.MenuHistory;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.stats.PlayerStatisticsGui;
import net.mineacle.core.teams.gui.TeamConfirmGui;
import net.mineacle.core.teams.gui.TeamInviteGui;
import net.mineacle.core.teams.gui.TeamMemberGui;
import net.mineacle.core.teams.gui.TeamsMainGui;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.model.TeamRole;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;
import java.util.UUID;

public final class TeamsGuiListener implements Listener {

    private static final String META_TARGET = "simple_team_target";
    private static final String META_ACTION = "simple_team_action";
    private static final String META_CONFIRM = "simple_team_confirm";

    private final Core core;
    private final TeamService teamService;
    private final TeamInviteService inviteService;
    private final TeamHomeService teamHomeService;
    private final TeleportService teleportService;
    private final PlayerStatisticsGui playerStatisticsGui;

    public TeamsGuiListener(
            Core core,
            TeamService teamService,
            TeamInviteService inviteService,
            TeamHomeService teamHomeService,
            TeleportService teleportService,
            PlayerStatisticsGui playerStatisticsGui
    ) {
        this.core = core;
        this.teamService = teamService;
        this.inviteService = inviteService;
        this.teamHomeService = teamHomeService;
        this.teleportService = teleportService;
        this.playerStatisticsGui = playerStatisticsGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        if (slot < 0 || slot >= topSize) {
            return;
        }

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title == null) {
            return;
        }

        if (isTeamMainMenu(title)) {
            event.setCancelled(true);
            handleMainClick(player, slot);
            return;
        }

        if (title.equals(ChatColor.stripColor(TeamInviteGui.TITLE))) {
            event.setCancelled(true);
            handleInviteClick(player, slot);
            return;
        }

        if (title.startsWith(TeamMemberGui.TITLE_PREFIX)) {
            event.setCancelled(true);
            handleMemberClick(player, slot);
            return;
        }

        if (title.equals(ChatColor.stripColor(TeamConfirmGui.TITLE))) {
            event.setCancelled(true);
            handleConfirmClick(player, slot);
        }
    }

    private boolean isTeamMainMenu(String title) {
        TeamRecord viewerTeam = null;

        for (Player online : core.getServer().getOnlinePlayers()) {
            TeamRecord possibleTeam = teamService.getTeamByPlayer(online.getUniqueId());

            if (possibleTeam == null) {
                continue;
            }

            String expectedTitle = possibleTeam.name()
                    + " ("
                    + teamService.getTeamMembers(possibleTeam.teamId()).size()
                    + "/"
                    + teamService.maxMembers()
                    + ")";

            if (title.equals(expectedTitle)) {
                viewerTeam = possibleTeam;
                break;
            }
        }

        return viewerTeam != null;
    }

    private void handleMainClick(Player player, int slot) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.closeInventory();
            return;
        }

        List<UUID> members = teamService.getTeamMembers(team.teamId());

        if (slot >= 0 && slot < 45) {
            if (slot < members.size()) {
                UUID targetId = members.get(slot);
                player.setMetadata(META_TARGET, new FixedMetadataValue(core, targetId.toString()));

                MenuHistory.openChild(
                        core,
                        player,
                        () -> TeamsMainGui.open(core, player, teamService, inviteService),
                        () -> TeamMemberGui.open(player, targetId, teamService)
                );
                return;
            }

            if (teamService.isAdmin(player.getUniqueId())
                    && slot == members.size()
                    && members.size() < teamService.maxMembers()) {
                player.closeInventory();

                Component invitePrompt = Component.text("§7Type §d/team invite <player> §7to invite a player.")
                        .clickEvent(ClickEvent.suggestCommand("/team invite "));

                player.sendMessage(invitePrompt);
            }

            return;
        }

        if (slot == 47) {
            org.bukkit.Location home = teamHomeService.getTeamHome(team.teamId());

            if (home == null) {
                player.sendMessage("§cYour team does not have a home set.");
                player.sendMessage("§7Team Home can only be set from the §d/homes §7menu.");
                return;
            }

            player.closeInventory();

            teleportService.begin(player, "Team Home", () -> {
                player.teleport(home);
                player.sendMessage("§aTeleported to Team Home.");
            });
            return;
        }

        if (slot == 51 && teamService.isAdmin(player.getUniqueId())) {
            boolean newValue = !team.friendlyFire();
            teamService.setFriendlyFire(team.teamId(), newValue);

            String message = newValue ? "§aTeam PvP enabled." : "§cTeam PvP disabled.";
            player.sendMessage(message);
            player.sendActionBar(Component.text(message));

            TeamsMainGui.open(core, player, teamService, inviteService);
        }
    }

    private void handleInviteClick(Player player, int slot) {
        if (slot == 11) {
            if (inviteService.acceptInvite(player.getUniqueId())) {
                player.sendMessage("§aInvite accepted.");
                MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
            } else {
                player.closeInventory();
                player.sendMessage("§cCould not accept invite.");
            }
            return;
        }

        if (slot == 15) {
            if (inviteService.denyInvite(player.getUniqueId())) {
                player.closeInventory();
                player.sendMessage("§cInvite declined.");
            } else {
                player.closeInventory();
                player.sendMessage("§cNo invite found.");
            }
        }
    }

    private void handleMemberClick(Player player, int slot) {
        if (!player.hasMetadata(META_TARGET)) {
            player.closeInventory();
            return;
        }

        UUID targetId = UUID.fromString(player.getMetadata(META_TARGET).get(0).asString());
        TeamMemberRecord target = teamService.getMember(targetId);

        if (target == null) {
            player.closeInventory();
            return;
        }

        if (slot == 10) {
            startConfirm(player, "PROMOTE", targetId, "Promote Player");
            return;
        }

        if (slot == 11) {
            startConfirm(player, "DEMOTE", targetId, "Demote Player");
            return;
        }

        if (slot == 13) {
            MenuHistory.openChild(
                    core,
                    player,
                    () -> TeamMemberGui.open(player, targetId, teamService),
                    () -> playerStatisticsGui.open(player, targetId)
            );
            return;
        }

        if (slot == 15) {
            startConfirm(player, "KICK", targetId, "Kick Player");
            return;
        }

        if (slot == 16) {
            startConfirm(player, "BAN", targetId, "Ban Player");
        }
    }

    private void startConfirm(Player player, String action, UUID targetId, String title) {
        player.setMetadata(META_ACTION, new FixedMetadataValue(core, action));
        player.setMetadata(META_TARGET, new FixedMetadataValue(core, targetId.toString()));
        player.removeMetadata(META_CONFIRM, core);

        MenuHistory.openChild(
                core,
                player,
                () -> TeamMemberGui.open(player, targetId, teamService),
                () -> TeamConfirmGui.open(core, player, title)
        );
    }

    private void handleConfirmClick(Player player, int slot) {
        if (slot == 11) {
            clearConfirmMeta(player);
            player.closeInventory();
            player.sendMessage("§cAction cancelled.");
            return;
        }

        if (slot != 13 && slot != 15) {
            return;
        }

        if (!player.hasMetadata(META_ACTION)) {
            player.closeInventory();
            return;
        }

        String action = player.getMetadata(META_ACTION).get(0).asString();

        if (!isConfirmReady(player, action)) {
            markConfirmReady(player, action);
            return;
        }

        if (action.equals("DISBAND")) {
            if (teamService.disbandTeam(player.getUniqueId())) {
                clearConfirmMeta(player);
                player.closeInventory();
                player.sendMessage("§cTeam disbanded.");
                return;
            }

            player.sendMessage("§cOnly the founder can disband the team.");
            return;
        }

        if (!player.hasMetadata(META_TARGET)) {
            player.closeInventory();
            return;
        }

        UUID targetId = UUID.fromString(player.getMetadata(META_TARGET).get(0).asString());

        switch (action) {
            case "PROMOTE" -> {
                if (teamService.setMemberRole(player.getUniqueId(), targetId, TeamRole.ADMIN)) {
                    clearConfirmMeta(player);
                    player.sendMessage("§aPlayer promoted.");
                    MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                    return;
                }

                player.sendMessage("§cYou cannot promote this player.");
            }

            case "DEMOTE" -> {
                if (teamService.setMemberRole(player.getUniqueId(), targetId, TeamRole.MEMBER)) {
                    clearConfirmMeta(player);
                    player.sendMessage("§aPlayer demoted.");
                    MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                    return;
                }

                player.sendMessage("§cYou cannot demote this player.");
            }

            case "KICK" -> {
                if (teamService.kickMember(player.getUniqueId(), targetId)) {
                    clearConfirmMeta(player);
                    player.sendMessage("§cPlayer kicked.");
                    MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                    return;
                }

                player.sendMessage("§cYou cannot kick that player.");
            }

            case "BAN" -> {
                if (teamService.banMember(player.getUniqueId(), targetId)) {
                    clearConfirmMeta(player);
                    player.sendMessage("§cPlayer banned from this team.");
                    MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                    return;
                }

                player.sendMessage("§cYou cannot ban that player.");
            }

            default -> player.closeInventory();
        }
    }

    private boolean isConfirmReady(Player player, String action) {
        if (!player.hasMetadata(META_CONFIRM)) {
            return false;
        }

        return player.getMetadata(META_CONFIRM).get(0).asString().equals(action);
    }

    private void markConfirmReady(Player player, String action) {
        player.setMetadata(META_CONFIRM, new FixedMetadataValue(core, action));

        String message = "§7Click confirm again to continue.";
        player.sendActionBar(Component.text(message));
        player.sendMessage(message);

        core.getServer().getScheduler().runTaskLater(core, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (!player.hasMetadata(META_CONFIRM)) {
                return;
            }

            String current = player.getMetadata(META_CONFIRM).get(0).asString();
            if (!current.equals(action)) {
                return;
            }

            player.removeMetadata(META_CONFIRM, core);
            player.sendActionBar(Component.text("§cAction timed out."));
            player.sendMessage("§cAction timed out.");
        }, 20L * 5L);
    }

    private void clearConfirmMeta(Player player) {
        player.removeMetadata(META_ACTION, core);
        player.removeMetadata(META_TARGET, core);
        player.removeMetadata(META_CONFIRM, core);
    }
}