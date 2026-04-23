package net.mineacle.core.teams.service;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamInviteRecord;
import net.mineacle.core.teams.model.TeamRole;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public final class TeamInviteService {

    private final Core core;
    private final TeamService teamService;
    private final TeamBanService banService;

    public TeamInviteService(Core core, TeamService teamService, TeamBanService banService) {
        this.core = core;
        this.teamService = teamService;
        this.banService = banService;
    }

    public TeamInviteRecord getInvite(UUID inviteeId) {
        FileConfiguration config = core.getTeamsConfig();
        String base = "invites." + inviteeId;

        String teamId = config.getString(base + ".team");
        String inviterRaw = config.getString(base + ".inviter");
        long createdAt = config.getLong(base + ".created-at", 0L);

        if (teamId == null || inviterRaw == null || inviterRaw.isBlank()) {
            return null;
        }

        return new TeamInviteRecord(inviteeId, teamId, UUID.fromString(inviterRaw), createdAt);
    }

    public boolean hasInvite(UUID inviteeId) {
        return getInvite(inviteeId) != null;
    }

    public boolean createInvite(String teamId, UUID inviterId, UUID inviteeId) {
        if (teamService.getTeamById(teamId) == null) {
            return false;
        }

        if (teamService.hasTeam(inviteeId)) {
            return false;
        }

        if (banService.isBanned(teamId, inviteeId)) {
            return false;
        }

        FileConfiguration config = core.getTeamsConfig();
        config.set("invites." + inviteeId + ".team", teamId);
        config.set("invites." + inviteeId + ".inviter", inviterId.toString());
        config.set("invites." + inviteeId + ".created-at", System.currentTimeMillis());
        core.saveTeamsFile();
        return true;
    }

    public boolean denyInvite(UUID inviteeId) {
        if (!hasInvite(inviteeId)) {
            return false;
        }

        core.getTeamsConfig().set("invites." + inviteeId, null);
        core.saveTeamsFile();
        return true;
    }

    public boolean acceptInvite(UUID inviteeId) {
        TeamInviteRecord invite = getInvite(inviteeId);
        if (invite == null) {
            return false;
        }

        if (teamService.hasTeam(inviteeId)) {
            denyInvite(inviteeId);
            return false;
        }

        if (banService.isBanned(invite.teamId(), inviteeId)) {
            denyInvite(inviteeId);
            return false;
        }

        boolean added = teamService.addMember(invite.teamId(), inviteeId, TeamRole.MEMBER);
        if (!added) {
            return false;
        }

        core.getTeamsConfig().set("invites." + inviteeId, null);
        core.saveTeamsFile();
        return true;
    }
}