package net.mineacle.core.teams.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.mineacle.core.Core;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.teams.gui.TeamBansGui;
import net.mineacle.core.teams.gui.TeamConfirmGui;
import net.mineacle.core.teams.gui.TeamGuiSession;
import net.mineacle.core.teams.gui.TeamInviteGui;
import net.mineacle.core.teams.gui.TeamMemberManageGui;
import net.mineacle.core.teams.gui.TeamsMainGui;
import net.mineacle.core.teams.model.TeamBanRecord;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.model.TeamRole;
import net.mineacle.core.teams.model.TeamSortType;
import net.mineacle.core.teams.service.TeamBanService;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import net.mineacle.core.teams.sign.TeamSignService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
    private final TeamSignService teamSignService;

    public TeamsGuiListener(
            Core core,
            TeamService teamService,
            TeamBanService banService,
            TeamInviteService inviteService,
            TeamHomeService teamHomeService,
            TeleportService teleportService,
            TeamSignService teamSignService
    ) {
        this.core = core;
        this.teamService = teamService;
        this.banService = banService;
        this.inviteService = inviteService;
        this.teamHomeService = teamHomeService;
        this.teleportService = teleportService;
        this.teamSignService = teamSignService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().getTitle();
        int slot = event.getRawSlot();

        if (title.startsWith(TeamsMainGui.TEAM_TITLE(core))) {
            event.setCancelled(true);
            handleMainTeamClick(player, slot);
            return;
        }

        if (title.equals(TeamsMainGui.NO_TEAM_TITLE(core))) {
            event.setCancelled(true);

            if (slot == 11) {
                player.closeInventory();
                sendCreateTeamPrompt(player);
                return;
            }

            if (slot == 15) {
                TeamInviteGui.open(core, player, inviteService, teamService);
            }

            return;
        }

        if (title.equals(TeamInviteGui.TITLE(core))) {
            event.setCancelled(true);
            handleInviteGuiClick(player, slot);
            return;
        }

        if (title.equals(TeamBansGui.TITLE(core))) {
            event.setCancelled(true);
            handleBansGuiClick(player, slot);
            return;
        }

        if (title.equals(org.bukkit.ChatColor.translateAlternateColorCodes('&', core.getMessage("teams.gui.member-manager-title")))) {
            event.setCancelled(true);
            handleManageMemberClick(player, slot);
            return;
        }

        if (title.equals(TeamConfirmGui.LEAVE_TITLE)
                || title.equals(TeamConfirmGui.DISBAND_TITLE)
                || title.equals(TeamConfirmGui.DELETE_HOME_TITLE)
                || title.equals(TeamConfirmGui.KICK_TITLE)
                || title.equals(TeamConfirmGui.TRANSFER_TITLE)) {
            event.setCancelled(true);
            handleConfirmClick(player, slot);
        }
    }

    private void handleMainTeamClick(Player player, int slot) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        TeamSortType sortType = TeamGuiSession.getSort(player.getUniqueId());
        String search = TeamGuiSession.getMemberSearch(player.getUniqueId()).toLowerCase(Locale.ROOT);

        List<UUID> allMembers = teamService.getSortedTeamMembers(team.teamId(), sortType);
        List<UUID> filteredMembers = new ArrayList<>();
        for (UUID memberId : allMembers) {
            String name = Bukkit.getOfflinePlayer(memberId).getName();
            String compare = name == null ? memberId.toString() : name;

            if (!search.isBlank() && !compare.toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }

            filteredMembers.add(memberId);
        }

        int page = TeamGuiSession.getPage(player.getUniqueId());
        int startIndex = page * 45;

        if (slot >= 0 && slot < 45) {
            int index = startIndex + slot;
            if (index >= filteredMembers.size()) {
                return;
            }

            UUID targetId = filteredMembers.get(index);
            player.setMetadata(META_MANAGE_TARGET, new FixedMetadataValue(core, targetId.toString()));
            TeamMemberManageGui.open(player, targetId, teamService);
            return;
        }

        switch (slot) {
            case 45 -> {
                player.closeInventory();
                teamSignService.openSearchSign(player);
            }
            case 46 -> {
                TeamSortType next = TeamGuiSession.getSort(player.getUniqueId()).next();
                TeamGuiSession.setSort(player.getUniqueId(), next);
                TeamGuiSession.setPage(player.getUniqueId(), 0);
                TeamsMainGui.open(core, player, teamService, inviteService);
            }
            case 47 -> {
                if (!teamService.isAdmin(player.getUniqueId())) {
                    player.sendMessage(core.getMessage("teams.invite.no-permission"));
                    return;
                }
                player.closeInventory();
                teamSignService.openInviteSign(player);
            }
            case 48 -> {
                int current = TeamGuiSession.getPage(player.getUniqueId());
                if (current > 0) {
                    TeamGuiSession.setPage(player.getUniqueId(), current - 1);
                }
                TeamsMainGui.open(core, player, teamService, inviteService);
            }
            case 49 -> TeamsMainGui.open(core, player, teamService, inviteService);
            case 50 -> {
                int current = TeamGuiSession.getPage(player.getUniqueId());
                int maxPage = Math.max(0, (filteredMembers.size() - 1) / 45);
                if (current < maxPage) {
                    TeamGuiSession.setPage(player.getUniqueId(), current + 1);
                }
                TeamsMainGui.open(core, player, teamService, inviteService);
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
            case 52 -> {
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
            case 53 -> {
                if (teamService.isFounder(player.getUniqueId())) {
                    TeamBansGui.open(core, player, team.teamId(), banService, teamService);
                    return;
                }

                if (!teamService.isAdmin(player.getUniqueId())) {
                    player.sendMessage(core.getMessage("teams.pvp.no-permission"));
                    return;
                }

                boolean newValue = !team.friendlyFire();
                teamService.setFriendlyFire(team.teamId(), newValue);
                player.sendMessage(newValue ? core.getMessage("teams.pvp.enabled") : core.getMessage("teams.pvp.disabled"));
                TeamsMainGui.open(core, player, teamService, inviteService);
            }
            default -> {
            }
        }
    }

    private void handleInviteGuiClick(Player player, int slot) {
        if (slot == 11) {
            if (!inviteService.acceptInvite(player.getUniqueId())) {
                player.sendMessage("§cCould not accept the invite.");
                return;
            }

            player.sendMessage("§7Joined the team.");
            TeamsMainGui.open(core, player, teamService, inviteService);
            return;
        }

        if (slot == 15) {
            if (!inviteService.denyInvite(player.getUniqueId())) {
                player.sendMessage("§cCould not deny the invite.");
                return;
            }

            player.sendMessage(core.getMessage("teams.invite.denied"));
            TeamsMainGui.open(core, player, teamService, inviteService);
        }
    }

    private void handleBansGuiClick(Player player, int slot) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.closeInventory();
            return;
        }

        if (slot == 49) {
            TeamsMainGui.open(core, player, teamService, inviteService);
            return;
        }

        if (slot < 0 || slot >= 45) {
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
                if (viewerMember.role() != TeamRole.FOUNDER) {
                    player.sendMessage(core.getMessage("teams.management.promote-founder-only"));
                    return;
                }
                if (targetMember.role() == TeamRole.MEMBER) {
                    teamService.setMemberRole(targetId, TeamRole.ADMIN);
                    player.sendMessage(core.getMessage("teams.management.promoted"));
                    TeamMemberManageGui.open(player, targetId, teamService);
                }
            }
            case 12 -> {
                if (viewerMember.role() != TeamRole.FOUNDER) {
                    player.sendMessage(core.getMessage("teams.management.demote-founder-only"));
                    return;
                }
                if (targetMember.role() == TeamRole.ADMIN) {
                    teamService.setMemberRole(targetId, TeamRole.MEMBER);
                    player.sendMessage(core.getMessage("teams.management.demoted"));
                    TeamMemberManageGui.open(player, targetId, teamService);
                }
            }
            case 14 -> {
                if (targetId.equals(player.getUniqueId())) {
                    player.sendMessage(core.getMessage("teams.management.cannot-kick-self"));
                    return;
                }

                String targetName = Bukkit.getOfflinePlayer(targetId).getName();
                if (targetName == null) {
                    targetName = targetId.toString();
                }

                if (viewerMember.role() == TeamRole.FOUNDER) {
                    if (targetMember.role() != TeamRole.FOUNDER) {
                        player.setMetadata(META_TEAM_ACTION, new FixedMetadataValue(core, "KICK:" + targetId));
                        player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
                        TeamConfirmGui.openKick(player, targetName);
                    }
                    return;
                }

                if (viewerMember.role() == TeamRole.ADMIN && targetMember.role() == TeamRole.MEMBER) {
                    player.setMetadata(META_TEAM_ACTION, new FixedMetadataValue(core, "KICK:" + targetId));
                    player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
                    TeamConfirmGui.openKick(player, targetName);
                    return;
                }

                player.sendMessage(core.getMessage("teams.management.cannot-kick-target"));
            }
            case 16 -> {
                if (viewerMember.role() != TeamRole.FOUNDER) {
                    player.sendMessage(core.getMessage("teams.management.transfer-founder-only"));
                    return;
                }
                if (targetMember.role() == TeamRole.FOUNDER) {
                    player.sendMessage(core.getMessage("teams.management.already-founder"));
                    return;
                }

                String targetName = Bukkit.getOfflinePlayer(targetId).getName();
                if (targetName == null) {
                    targetName = targetId.toString();
                }

                player.setMetadata(META_TEAM_ACTION, new FixedMetadataValue(core, "TRANSFER:" + targetId));
                player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
                TeamConfirmGui.openTransfer(player, targetName);
            }
            case 22 -> TeamsMainGui.open(core, player, teamService, inviteService);
            default -> {
            }
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

            player.sendActionBar(Component.text(core.getMessage("teams.gui.action-cancelled")));
            player.sendMessage(core.getMessage("teams.gui.action-cancelled"));
            return;
        }

        if (slot != 15) {
            return;
        }

        if (!confirmed) {
            player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, true));
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

                if (currentAction.equals(actionValue) && currentConfirmed) {
                    player.setMetadata(META_TEAM_CONFIRM, new FixedMetadataValue(core, false));
                    player.sendActionBar(Component.text(core.getMessage("teams.gui.action-timeout")));
                    player.sendMessage(core.getMessage("teams.gui.action-timeout"));
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
        String value = split[1];

        switch (action) {
            case "LEAVE" -> {
                if (!teamService.isFounder(player.getUniqueId())) {
                    teamService.removeMember(player.getUniqueId());
                    TeamGuiSession.clear(player.getUniqueId());
                    player.sendMessage(core.getMessage("teams.left-team"));
                }
            }
            case "DISBAND" -> {
                TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
                if (team != null && teamService.isFounder(player.getUniqueId())) {
                    teamService.disbandTeam(team.teamId());
                    TeamGuiSession.clear(player.getUniqueId());
                    player.sendMessage(core.getMessage("teams.disbanded"));
                }
            }
            case "DELETE_HOME" -> {
                TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
                if (team != null && teamService.isFounder(player.getUniqueId())) {
                    teamHomeService.deleteTeamHome(team.teamId());
                    player.sendMessage(core.getMessage("teams.home.deleted"));
                    TeamsMainGui.open(core, player, teamService, inviteService);
                }
            }
            case "KICK" -> {
                UUID targetId = UUID.fromString(value);
                TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
                if (team != null) {
                    banService.banForDefaultDuration(team.teamId(), targetId, player.getUniqueId());
                }
                teamService.removeMember(targetId);
                player.sendMessage(core.getMessage("teams.management.kicked"));
                TeamsMainGui.open(core, player, teamService, inviteService);
            }
            case "TRANSFER" -> {
                UUID targetId = UUID.fromString(value);
                TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
                if (team != null) {
                    if (teamService.transferFounder(team.teamId(), player.getUniqueId(), targetId)) {
                        player.sendMessage(core.getMessage("teams.management.transfer-success"));
                        TeamsMainGui.open(core, player, teamService, inviteService);
                    }
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

    private void sendCreateTeamPrompt(Player player) {
        player.sendMessage("§7Click the line below to create a team.");
        player.sendMessage(" ");

        Component clickable = Component.text("§d[CLICK HERE] §7Type /team create <name>")
                .clickEvent(ClickEvent.suggestCommand("/team create "));

        player.sendMessage(clickable);
        player.sendMessage(" ");
        player.sendMessage("§7It is clickable and will fill the command into chat for you.");
    }
}