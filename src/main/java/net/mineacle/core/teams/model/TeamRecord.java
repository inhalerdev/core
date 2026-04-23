package net.mineacle.core.teams.model;

import java.util.UUID;

public final class TeamRecord {

    private final String teamId;
    private final String name;
    private final UUID founder;
    private final boolean friendlyFire;
    private final long createdAt;

    public TeamRecord(String teamId, String name, UUID founder, boolean friendlyFire, long createdAt) {
        this.teamId = teamId;
        this.name = name;
        this.founder = founder;
        this.friendlyFire = friendlyFire;
        this.createdAt = createdAt;
    }

    public String teamId() {
        return teamId;
    }

    public String name() {
        return name;
    }

    public UUID founder() {
        return founder;
    }

    public boolean friendlyFire() {
        return friendlyFire;
    }

    public long createdAt() {
        return createdAt;
    }
}