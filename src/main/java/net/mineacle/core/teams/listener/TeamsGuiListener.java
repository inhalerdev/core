package net.mineacle.core.teams.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.mineacle.core.Core;
import net.mineacle.core.common.gui.MenuHistory;
import net.mineacle.core.common.text.TextColor;
import net.mineacle.core.homes.gui.HomesMainGui;
import net.mineacle.core.homes.service.HomeService;
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
    private final HomeService homeService;
    private final TeleportService teleportService;
    private final PlayerStatisticsGui playerStatisticsGui;

    public TeamsGuiListener(
            Core core,
            TeamService teamService,
            TeamInviteService inviteService,
            TeamHomeService teamHomeService,
            HomeService homeService,
            TeleportService teleportService,
            PlayerStatisticsGui playerStatisticsGui
    ) {
        this.core = core;
        this.teamService = teamService;
        this.inviteService = inviteService;
        this.teamHomeService = teamHomeService;
        this.homeService = homeService;
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
                return true;
            }
        }

        return false;
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

                Component invitePrompt = Component.text("§7Type §d/team invite <player> §7to invite a player")
                        .clickEvent(ClickEvent.suggestCommand("/team invite "));

                player.sendMessage(invitePrompt);
            }

            return;
        }

        if (slot == 47) {
            handleTeamHomeButton(player, team);
            return;
        }

        if (slot == 51 && teamService.isAdmin(player.getUniqueId())) {
            boolean newValue = !team.friendlyFire();
            teamService.setFriendlyFire(team.teamId(), newValue);

            String message = newValue ? "§aTeam PvP enabled" : "§cTeam PvP disabled";

            sendBoth(player, message);

            TeamsMainGui.open(core, player, teamService, inviteService);
        }
    }

    private void handleTeamHomeButton(Player player, TeamRecord team) {
        org.bukkit.Location home = teamHomeService.getTeamHome(team.teamId());

        if (home != null) {
            player.closeInventory();

            teleportService.begin(player, "Team Home", () -> {
                player.teleport(home);
                sendBoth(player, "§7Teleported to §dTeam Home");
            });

            return;
        }

        if (!teamService.isFounder(player.getUniqueId())) {
            sendBoth(player, "§cYour team does not have a home set");
            player.sendMessage("§7Ask your §dteam founder §7to set Team Home");
            return;
        }

        sendBoth(player, "§7Open Homes to set §dTeam Home");

        MenuHistory.openChild(
                core,
                player,
                () -> TeamsMainGui.open(core, player, teamService, inviteService),
                () -> HomesMainGui.open(core, player, homeService)
        );
    }

    private void handleInviteClick(Player player, int slot) {
        if (slot == 11) {
            if (inviteService.acceptInvite(player.getUniqueId())) {
                sendBoth(player, "§aInvite accepted");
                MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
            } else {
                player.closeInventory();
                sendBoth(player, "§cCould not accept invite");
            }

            return;
        }

        if (slot == 15) {
            if (inviteService.denyInvite(player.getUniqueId())) {
                player.closeInventory();
                sendBoth(player, "§cInvite declined");
            } else {
                player.closeInventory();
                sendBoth(player, "§cNo invite found");
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
            sendBoth(player, "§cThat player is no longer in your team");
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
        if (slot == TeamConfirmGui.CANCEL_SLOT) {
            clearConfirmMeta(player);
            player.closeInventory();
            sendBoth(player, "§cAction cancelled");
            return;
        }

        if (slot == TeamConfirmGui.ACTION_SLOT) {
            return;
        }

        if (slot != TeamConfirmGui.CONFIRM_SLOT) {
            return;
        }

        if (!player.hasMetadata(META_ACTION)) {
            clearConfirmMeta(player);
            player.closeInventory();
            sendBoth(player, "§cNo action is ready to confirm");
            return;
        }

        String action = player.getMetadata(META_ACTION).get(0).asString();

        if (!isConfirmReady(player, action)) {
            markConfirmReady(player, action);
            return;
        }

        executeConfirmedAction(player, action);
    }

    private void executeConfirmedAction(Player player, String action) {
        switch (action) {
            case "DISBAND" -> {
                if (teamService.disbandTeam(player.getUniqueId())) {
                    clearConfirmMeta(player);
                    player.closeInventory();
                    sendBoth(player, "§cTeam disbanded");
                    return;
                }

                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cOnly the founder can disband the team");
            }

            case "LEAVE" -> {
                if (teamService.removeMember(player.getUniqueId())) {
                    clearConfirmMeta(player);
                    player.closeInventory();
                    sendBoth(player, "§cYou left your team");
                    return;
                }

                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cYou cannot leave as founder Use /team disband");
            }

            case "DELETE_HOME" -> {
                TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

                if (team == null) {
                    clearConfirmMeta(player);
                    player.closeInventory();
                    sendBoth(player, "§cYou are not in a team");
                    return;
                }

                if (!teamService.isAdmin(player.getUniqueId())) {
                    clearConfirmMeta(player);
                    player.closeInventory();
                    sendBoth(player, "§cOnly admins can delete team home");
                    return;
                }

                if (!teamHomeService.deleteTeamHome(team.teamId())) {
                    clearConfirmMeta(player);
                    player.closeInventory();
                    sendBoth(player, "§cYour team does not have a home set");
                    return;
                }

                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cTeam home deleted");
            }

            case "PROMOTE", "DEMOTE", "KICK", "BAN" -> executeConfirmedTargetAction(player, action);

            default -> {
                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cUnknown action");
            }
        }
    }

    private void executeConfirmedTargetAction(Player player, String action) {
        if (!player.hasMetadata(META_TARGET)) {
            clearConfirmMeta(player);
            player.closeInventory();
            sendBoth(player, "§cNo player is selected");
            return;
        }

        UUID targetId = UUID.fromString(player.getMetadata(META_TARGET).get(0).asString());

        switch (action) {
            case "PROMOTE" -> {
                if (teamService.setMemberRole(player.getUniqueId(), targetId, TeamRole.ADMIN)) {
                    clearConfirmMeta(player);
                    sendBoth(player, "§aPlayer promoted");
                    MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                    return;
                }

                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cYou cannot promote this player");
            }

            case "DEMOTE" -> {
                if (teamService.setMemberRole(player.getUniqueId(), targetId, TeamRole.MEMBER)) {
                    clearConfirmMeta(player);
                    sendBoth(player, "§aPlayer demoted");
                    MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                    return;
                }

                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cYou cannot demote this player");
            }

            case "KICK" -> {
                if (teamService.kickMember(player.getUniqueId(), targetId)) {
                    clearConfirmMeta(player);
                    sendBoth(player, "§cPlayer kicked");
                    MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                    return;
                }

                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cYou cannot kick that player");
            }

            case "BAN" -> {
                if (teamService.banMember(player.getUniqueId(), targetId)) {
                    clearConfirmMeta(player);
                    sendBoth(player, "§cPlayer banned from this team");
                    MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
                    return;
                }

                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cYou cannot ban that player");
            }

            default -> {
                clearConfirmMeta(player);
                player.closeInventory();
                sendBoth(player, "§cUnknown action");
            }
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

        String message = "§7Click confirm again to continue";

        sendBoth(player, message);

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
            sendBoth(player, "§cAction timed out");
        }, 20L * 5L);
    }

    private void clearConfirmMeta(Player player) {
        player.removeMetadata(META_ACTION, core);
        player.removeMetadata(META_TARGET, core);
        player.removeMetadata(META_CONFIRM, core);
    }

    private void sendBoth(Player player, String message) {
        player.sendMessage(message);
        player.sendActionBar(actionBar(message));
    }

    private Component actionBar(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(TextColor.color(message));
    }
}