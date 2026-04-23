package net.mineacle.core.teams.model;

import java.util.UUID;

public final class TeamMemberRecord {

    private final UUID playerId;
    private final String teamId;
    private final TeamRole role;
    private final long joinedAt;

    public TeamMemberRecord(UUID playerId, String teamId, TeamRole role, long joinedAt) {
        this.playerId = playerId;
        this.teamId = teamId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public UUID playerId() {
        return playerId;
    }

    public String teamId() {
        return teamId;
    }

    public TeamRole role() {
        return role;
    }

    public long joinedAt() {
        return joinedAt;
    }
}