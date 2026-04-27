package net.mineacle.core.teams.service;

import net.mineacle.core.teams.model.TeamInviteRecord;
import net.mineacle.core.teams.model.TeamRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamInviteService {

    private final TeamService teamService;
    private final Map<UUID, TeamInviteRecord> invites = new HashMap<>();

    public TeamInviteService(TeamService teamService) {
        this.teamService = teamService;
    }

    public boolean createInvite(String teamId, UUID inviterId, UUID targetId) {
        TeamRecord team = teamService.getTeamById(teamId);

        if (team == null) {
            return false;
        }

        if (teamService.hasTeam(targetId)) {
            return false;
        }

        if (teamService.isBanned(teamId, targetId)) {
            return false;
        }

        if (teamService.getTeamMembers(teamId).size() >= teamService.maxMembers()) {
            return false;
        }

        invites.put(targetId, new TeamInviteRecord(teamId, inviterId, targetId, System.currentTimeMillis()));
        return true;
    }

    public boolean hasInvite(UUID playerId) {
        return invites.containsKey(playerId);
    }

    public TeamInviteRecord getInvite(UUID playerId) {
        return invites.get(playerId);
    }

    public boolean acceptInvite(UUID playerId) {
        TeamInviteRecord invite = invites.remove(playerId);

        if (invite == null) {
            return false;
        }

        if (teamService.isBanned(invite.teamId(), playerId)) {
            return false;
        }

        return teamService.addMember(invite.teamId(), playerId);
    }

    public boolean denyInvite(UUID playerId) {
        return invites.remove(playerId) != null;
    }
}