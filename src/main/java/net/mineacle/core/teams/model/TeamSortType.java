package net.mineacle.core.teams.model;

public enum TeamSortType {
    JOIN_DATE,
    PERMISSIONS,
    ALPHABETICALLY,
    ONLINE_MEMBERS;

    public TeamSortType next() {
        TeamSortType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public String displayName() {
        return switch (this) {
            case JOIN_DATE -> "Join Date";
            case PERMISSIONS -> "Permissions";
            case ALPHABETICALLY -> "Alphabetically";
            case ONLINE_MEMBERS -> "Online Members";
        };
    }
}