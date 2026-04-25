package net.mineacle.core.teams.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.mineacle.core.Core;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.stats.PlayerStatisticsGui;
import net.mineacle.core.teams.gui.TeamBannerColorGui;
import net.mineacle.core.teams.gui.TeamBansGui;
import net.mineacle.core.teams.gui.TeamConfirmGui;
import net.mineacle.core.teams.gui.TeamGuiSession;
import net.mineacle.core.teams.gui.TeamInviteGui;
import net.mineacle.core.teams.gui.TeamManageGui;
import net.mineacle.core.teams.gui.TeamMemberManageGui;
import net.mineacle.core.teams.gui.TeamNameColorGui;
import net.mineacle.core.teams.gui.TeamsMainGui;
import net.mineacle.core.teams.model.TeamBanRecord;
import net.mineacle.core.teams.model.TeamBannerColor;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamNameColor;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.model.TeamRole;
import net.mineacle.core.teams.model.TeamSortType;
import net.mineacle.core.teams.service.TeamBanService;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamsGuiListener implements Listener {

    private static final String META_TEAM_ACTION = "mt_action";
    private static final String META_TEAM_CONFIRM = "mt_confirm";
    private static final String META_MANAGE_TARGET = "mt_manage_target";

    private final Core core;
    private final TeamService teamService;
    private final TeamBanService banService;
    private final TeamInviteService inviteService;
    private final TeamHomeService teamHomeService;
    private final TeleportService teleportService;
    private final PlayerStatisticsGui playerStatisticsGui;

    private final Set<UUID> navigating = ConcurrentHashMap.newKeySet();

    public TeamsGuiListener(
            Core core,
            TeamService teamService,
            TeamBanService banService,
            TeamInviteService inviteService,
            TeamHomeService teamHomeService,
            TeleportService teleportService,
            PlayerStatisticsGui playerStatisticsGui
    ) {
        this.core = core;
        this.teamService = teamService;
        this.banService = banService;
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

        int topSize = event.getView().getTopInventory().getSize();
        int slot = event.getRawSlot();

        if (slot < 0 || slot >= topSize) {
            return;
        }

        String title = cleanTitle(event.getView().getTitle());

        if (title.equals(cleanTitle(TeamsMainGui.NO_TEAM_TITLE(core)))) {
            event.setCancelled(true);
            handleNoTeamClick(player, slot);
            return;
        }

        if (title.equals("team manage") || title.equals(cleanTitle(TeamManageGui.TITLE(core)))) {
            event.setCancelled(true);
            handleManageGuiClick(player, slot);
            return;
        }

        if (title.equals("banner color") || title.equals(cleanTitle(TeamBannerColorGui.TITLE(core)))) {
            event.setCancelled(true);
            handleBannerColorGuiClick(player, slot);
            return;
        }

        if (title.equals("name color") || title.equals(cleanTitle(TeamNameColorGui.TITLE(core)))) {
            event.setCancelled(true);
            handleNameColorGuiClick(player, slot);
            return;
        }

        if (title.equals(cleanTitle(TeamInviteGui.TITLE(core)))) {
            event.setCancelled(true);
            handleInviteGuiClick(player, slot);
            return;
        }

        if (title.equals(cleanTitle(TeamBansGui.TITLE(core)))) {
            event.setCancelled(true);
            handleBansGuiClick(player, slot);
            return;
        }

        if (TeamMemberManageGui.isTitle(event.getView().getTitle())) {
            event.setCancelled(true);
            handleManageMemberClick(player, slot);
            return;
        }

        if (title.equals(cleanTitle(TeamConfirmGui.LEAVE_TITLE))
                || title.equals(cleanTitle(TeamConfirmGui.DISBAND_TITLE))
                || title.equals(cleanTitle(TeamConfirmGui.DELETE_HOME_TITLE))
                || title.equals(cleanTitle(TeamConfirmGui.KICK_TITLE))
                || title.equals(cleanTitle(TeamConfirmGui.TRANSFER_TITLE))) {
            event.setCancelled(true);
            handleConfirmClick(player, slot);
            return;
        }

        if (teamService.getTeamByPlayer(player.getUniqueId()) != null && topSize == 54) {
            event.setCancelled(true);
            handleMainTeamClick(player, slot);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        if (navigating.remove(uuid)) {
            return;
        }

        String title = cleanTitle(event.getView().getTitle());

        if (title.equals("banner color")
                || title.equals("name color")
                || title.equals(cleanTitle(TeamBannerColorGui.TITLE(core)))
                || title.equals(cleanTitle(TeamNameColorGui.TITLE(core)))) {
            openPreviousMenu(player, () -> {
                if (player.isOnline() && teamService.getTeamByPlayer(uuid) != null && teamService.isAdmin(uuid)) {
                    TeamManageGui.open(core, player, teamService);
                }
            });
            return;
        }

        if (title.equals("team manage")
                || title.equals(cleanTitle(TeamManageGui.TITLE(core)))
                || title.equals(cleanTitle(TeamBansGui.TITLE(core)))
                || title.equals(cleanTitle(TeamInviteGui.TITLE(core)))
                || TeamMemberManageGui.isTitle(event.getView().getTitle())
                || title.equals(cleanTitle(TeamConfirmGui.LEAVE_TITLE))
                || title.equals(cleanTitle(TeamConfirmGui.DISBAND_TITLE))
                || title.equals(cleanTitle(TeamConfirmGui.DELETE_HOME_TITLE))
                || title.equals(cleanTitle(TeamConfirmGui.KICK_TITLE))
                || title.equals(cleanTitle(TeamConfirmGui.TRANSFER_TITLE))) {
            openPreviousMenu(player, () -> {
                if (player.isOnline() && teamService.getTeamByPlayer(uuid) != null) {
                    TeamsMainGui.open(core, player, teamService, inviteService);
                }
            });
        }
    }

    private void handleNoTeamClick(Player player, int slot) {
        if (slot == 15 && inviteService.hasInvite(player.getUniqueId())) {
            navigate(player, () -> TeamInviteGui.open(core, player, inviteService, teamService));
            return;
        }

        if (slot == 15) {
            player.closeInventory();

            String message = "§cYou have no current team invites.";
            player.sendActionBar(Component.text(message));
            player.sendMessage(message);
            return;
        }

        if (slot == 11 || slot == 13) {
            player.closeInventory();
            Bukkit.getScheduler().runTaskLater(core, () -> sendCreateTeamPrompt(player), 1L);
        }
    }

    private void handleMainTeamClick(Player player, int slot) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        TeamSortType sortType = TeamGuiSession.getSort(player.getUniqueId());
        List<UUID> members = new ArrayList<>(teamService.getSortedTeamMembers(team.teamId(), sortType));
        int memberCount = teamService.getTeamMembers(team.teamId()).size();

        if (slot >= 0 && slot < TeamsMainGui.maxRosterGuiSlots()) {
            if (slot < members.size()) {
                UUID targetId = members.get(slot);
                player.setMetadata(META_MANAGE_TARGET, new FixedMetadataValue(core, targetId.toString()));
                navigate(player, () -> TeamMemberManageGui.open(core, player, targetId, teamService));
                return;
            }

            if (teamService.isAdmin(player.getUniqueId())
                    && slot == members.size()
                    && memberCount < TeamsMainGui.teamSizeLimit(core)) {
                player.closeInventory();
                player.sendMessage("§7Type the player name after the command to invite them.");

                Component clickable = Component.text("§d/team invite ")
                        .clickEvent(ClickEvent.suggestCommand("/team invite "));

                player.sendMessage(clickable);
                return;
            }

            return;
        }

        switch (slot) {
            case 45 -> {
                TeamSortType next = TeamGuiSession.getSort(player.getUniqueId()).next();
                TeamGuiSession.setSort(player.getUniqueId(), next);
                TeamGuiSession.setPage(player.getUniqueId(), 0);
                TeamsMainGui.open(core, player, teamService, inviteService);
            }

            case 47 -> {
                org.bukkit.Location home = teamHomeService.getTeamHome(team.teamId());

                if (home == null) {
                    if (!teamService.isAdmin(player.getUniqueId())) {
                        player.sendMessage(core.getMessage("teams.home.no-home"));
                        return;
                    }

                    teamHomeService.setTeamHome(team.teamId(), player.getLocation());
                    player.sendMessage(core.getMessage("teams.home.set"));
                    TeamsMainGui.open(core, player, teamService, inviteService);
                    return;
                }

                player.closeInventory();
                teleportService.begin(player, "Team Home", () -> {
                    player.teleport(home);
                    player.sendMessage(core.getMessage("teams.home.teleported"));
                });
            }

            case 48 -> {
                if (!teamService.isAdmin(player.getUniqueId())) {
                    return;
                }

                boolean newValue = !team.friendlyFire();
                teamService.setFriendlyFire(team.teamId(), newValue);
                player.sendMessage(newValue ? core.getMessage("teams.pvp.enabled") : core.getMessage("teams.pvp.disabled"));
                TeamsMainGui.open(core, player, teamService, inviteService);
            }

            case 49 -> {
                // Team Info is informational.
            }

            case 50 -> {
                if (!teamService.isAdmin(player.getUniqueId())) {
                    player.sendMessage(core.getMessage("teams.management.no-permission"));
                    return;
                }

                navigate(player, () -> TeamManageGui.open(core, player, teamService));
            }

            case 51 -> {
                if (!teamService.isAdmin(player.getUniqueId())) {
                    player.sendMessage(core.getMessage("teams.bans.no-permission"));
                    return;
                }

                navigate(player, () -> TeamBansGui.open(core, player, team.teamId(), banService, teamService));
            }

            default -> {
            }
        }
    }

    private void handleManageGuiClick(Player player, int slot) {
        if (!teamService.isAdmin(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(core.getMessage("teams.management.no-permission"));
            return;
        }

        if (slot == 10) {
            navigate(player, () -> TeamBannerColorGui.open(core, player));
            return;
        }

        if (slot == 12) {
            navigate(player, () -> TeamNameColorGui.open(core, player));
            return;
        }

        if (slot == 16) {
            TeamManageGui.open(core, player, teamService);
        }
    }

    private void handleBannerColorGuiClick(Player player, int slot) {
        TeamBannerColor color = TeamBannerColorGui.fromSlot(slot);
        if (color == null) {
            return;
        }

        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(core.getMessage("teams.management.no-permission"));
            return;
        }

        teamService.setBannerColor(team.teamId(), color);
        player.sendMessage(core.getMessage("teams.management.banner-updated"));
        navigate(player, () -> TeamManageGui.open(core, player, teamService));
    }

    private void handleNameColorGuiClick(Player player, int slot) {
        TeamNameColor color = TeamNameColorGui.fromSlot(slot);
        if (color == null) {
            return;
        }

        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage(core.getMessage("teams.management.no-permission"));
            return;
        }

        teamService.setNameColor(team.teamId(), color.colorCode());
        player.sendMessage(core.getMessage("teams.management.name-color-updated"));
        navigate(player, () -> TeamManageGui.open(core, player, teamService));
    }

    private void handleInviteGuiClick(Player player, int slot) {
        if (slot == 11) {
            TeamRecord inviteTeam = inviteService.getInvite(player.getUniqueId()) == null
                    ? null
                    : teamService.getTeamById(inviteService.getInvite(player.getUniqueId()).teamId());

            if (inviteTeam != null && teamService.getTeamMembers(inviteTeam.teamId()).size() >= TeamsMainGui.teamSizeLimit(core)) {
                player.sendMessage("§cThat team is full.");
                return;
            }

            if (!inviteService.acceptInvite(player.getUniqueId())) {
                player.sendMessage("§cCould not accept the invite.");
                return;
            }

            player.sendMessage("§7Joined the team.");
            navigate(player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
            return;
        }

        if (slot == 15) {
            if (!inviteService.denyInvite(player.getUniqueId())) {
                player.sendMessage("§cCould not deny the invite.");
                return;
            }

            player.sendMessage(core.getMessage("teams.invite.denied"));
            navigate(player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
        }
    }

    private void handleBansGuiClick(Player player, int slot) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            navigate(player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
            return;
        }

        if (slot < 0 || slot >= TeamsMainGui.maxRosterGuiSlots()) {
            return;
        }

        List<TeamBanRecord> bans = banService.getActiveBans(team.teamId());

        if (slot >= bans.size()) {
            return;
        }

        TeamBanRecord selected = bans.get(slot);
        banService.clearBan(team.teamId(), selected.playerId());
        player.sendMessage(core.getMessage("teams.bans.unbanned"));
        TeamBansGui.open(core, player, team.teamId(), banService, teamService);
    }

    private void handleManageMemberClick(Player player, int slot) {
        if (!player.hasMetadata(META_MANAGE_TARGET)) {
            player.closeInventory();
            return;
        }

        UUID targetId = UUID.fromString(player.getMetadata(META_MANAGE_TARGET).get(0).asString());
        TeamRecord viewerTeam = teamService.getTeamByPlayer(player.getUniqueId());
        TeamRecord targetTeam = teamService.getTeamByPlayer(targetId);

        if (viewerTeam == null || targetTeam == null || !viewerTeam.teamId().equals(targetTeam.teamId())) {
            player.closeInventory();
            return;
        }

        TeamMemberRecord viewerMember = teamService.getMember(player.getUniqueId());
        TeamMemberRecord targetMember = teamService.getMember(targetId);

        if (viewerMember == null || targetMember == null) {
            player.closeInventory();
            return;
        }

        switch (slot) {
            case 10 -> {
                if (!TeamMemberManageGui.canPromote(viewerMember, targetMember)) {
                    player.sendMessage(core.getMessage("teams.management.promote-founder-only"));
                    return;
                }
                startConfirm(player, "PROMOTE:" + targetId);
            }

            case 11 -> {
                if (!TeamMemberManageGui.canDemote(viewerMember, targetMember)) {
                    player.sendMessage(core.getMessage("teams.management.demote-founder-only"));
                    return;
                }
                startConfirm(player, "DEMOTE:" + targetId);
            }

            case 12 -> {
                if (!TeamMemberManageGui.canKick(player, targetId, viewerMember, targetMember)) {
                    player.sendMessage(core.getMessage("teams.management.cannot-kick-target"));
                    return;
                }
                startConfirm(player, "KICK:" + targetId);
            }

            case 13 -> playerStatisticsGui.open(player, targetId);

            case 14 -> {
                if (!TeamMemberManageGui.canKick(player, targetId, viewerMember, targetMember)) {
                    player.sendMessage(core.getMessage("teams.management.cannot-kick-target"));
                    return;
                }
                startConfirm(player, "BAN:" + targetId);
            }

            case 16 -> {
                if (!TeamMemberManageGui.canTransferFounder(player, targetId, viewerMember, targetMember)) {
                    player.sendMessage(core.getMessage("teams.management.transfer-founder-only"));
                    return;
                }
                startConfirm(player, "TRANSFER:" + targetId);
            }

            default -> {
            }
        }
    }

    private void startConfirm(Player player, String action) {
        player.setMetadata(META_TEAM_ACTION, new FixedMetadataValue(core, action));
        player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
        player.sendActionBar(Component.text(core.getMessage("teams.gui.click-again")));
        player.sendMessage(core.getMessage("teams.gui.click-again"));

        core.getServer().getScheduler().runTaskLater(core, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (!player.hasMetadata(META_TEAM_ACTION) || !player.hasMetadata(META_TEAM_CONFIRM)) {
                return;
            }

            String currentAction = player.getMetadata(META_TEAM_ACTION).get(0).asString();
            boolean currentConfirmed = player.getMetadata(META_TEAM_CONFIRM).get(0).asBoolean();

            if (currentAction.equals(action) && !currentConfirmed) {
                clearConfirmMeta(player);
                player.sendActionBar(Component.text(core.getMessage("teams.gui.action-timeout")));
                player.sendMessage(core.getMessage("teams.gui.action-timeout"));
            }
        }, 20L * 5);
    }

    private void handleConfirmClick(Player player, int slot) {
        if (!player.hasMetadata(META_TEAM_ACTION)) {
            player.closeInventory();
            return;
        }

        String actionValue = player.getMetadata(META_TEAM_ACTION).get(0).asString();
        boolean confirmed = player.hasMetadata(META_TEAM_CONFIRM)
                && player.getMetadata(META_TEAM_CONFIRM).get(0).asBoolean();

        if (slot != 10 && slot != 11 && slot != 12 && slot != 14 && slot != 16 && slot != 15) {
            return;
        }

        if (!confirmed) {
            player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, true));
            player.sendActionBar(Component.text(core.getMessage("teams.gui.click-again")));
            player.sendMessage(core.getMessage("teams.gui.click-again"));
            return;
        }

        clearConfirmMeta(player);

        String[] split = actionValue.split(":", 2);
        if (split.length != 2) {
            return;
        }

        String action = split[0];
        UUID targetId = UUID.fromString(split[1]);

        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        switch (action) {
            case "PROMOTE" -> {
                teamService.setMemberRole(targetId, TeamRole.ADMIN);
                player.sendMessage(core.getMessage("teams.management.promoted"));
                TeamMemberManageGui.open(core, player, targetId, teamService);
            }

            case "DEMOTE" -> {
                teamService.setMemberRole(targetId, TeamRole.MEMBER);
                player.sendMessage(core.getMessage("teams.management.demoted"));
                TeamMemberManageGui.open(core, player, targetId, teamService);
            }

            case "KICK" -> {
                teamService.removeMember(targetId);
                player.sendMessage(core.getMessage("teams.management.kicked"));
                TeamsMainGui.open(core, player, teamService, inviteService);
            }

            case "BAN" -> {
                banService.banForDefaultDuration(team.teamId(), targetId, player.getUniqueId());
                teamService.removeMember(targetId);
                player.sendMessage(core.getMessage("teams.management.kicked"));
                TeamsMainGui.open(core, player, teamService, inviteService);
            }

            case "TRANSFER" -> {
                if (teamService.transferFounder(team.teamId(), player.getUniqueId(), targetId)) {
                    player.sendMessage(core.getMessage("teams.management.transfer-success"));
                    TeamsMainGui.open(core, player, teamService, inviteService);
                }
            }

            default -> {
            }
        }
    }

    private void navigate(Player player, Runnable openAction) {
        UUID uuid = player.getUniqueId();
        navigating.add(uuid);

        Bukkit.getScheduler().runTask(core, () -> {
            if (!player.isOnline()) {
                navigating.remove(uuid);
                return;
            }

            openAction.run();

            Bukkit.getScheduler().runTaskLater(core, () -> navigating.remove(uuid), 2L);
        });
    }

    private void clearConfirmMeta(Player player) {
        player.removeMetadata(META_TEAM_ACTION, core);
        player.removeMetadata(META_TEAM_CONFIRM, core);
    }

    private void sendCreateTeamPrompt(Player player) {
        player.sendMessage("§cYou are not in a team.");

        Component clickable = Component.text("§7Type §d/team create <name> §7to create a team.")
                .clickEvent(ClickEvent.suggestCommand("/team create "));

        player.sendMessage(clickable);
    }

    private String cleanTitle(String input) {
        String colored = ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
        String stripped = ChatColor.stripColor(colored);
        return stripped == null ? "" : stripped.trim().toLowerCase(Locale.ROOT);
    }
}