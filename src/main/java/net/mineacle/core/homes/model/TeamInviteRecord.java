package net.mineacle.core.teams.model;

import java.util.UUID;

public final class TeamInviteRecord {

    private final UUID inviteeId;
    private final String teamId;
    private final UUID inviterId;
    private final long createdAt;

    public TeamInviteRecord(UUID inviteeId, String teamId, UUID inviterId, long createdAt) {
        this.inviteeId = inviteeId;
        this.teamId = teamId;
        this.inviterId = inviterId;
        this.createdAt = createdAt;
    }

    public UUID inviteeId() {
        return inviteeId;
    }

    public String teamId() {
        return teamId;
    }

    public UUID inviterId() {
        return inviterId;
    }

    public long createdAt() {
        return createdAt;
    }
}