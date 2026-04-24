package net.mineacle.core.teams.model;

public enum TeamSortType {
    JOIN_DATE("Join Date"),
    PERMISSIONS("Permissions"),
    MONEY("Money"),
    ALPHABETICAL("Alphabetically"),
    ONLINE_MEMBERS("Online Members");

    private final String displayName;

    TeamSortType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public TeamSortType next() {
        TeamSortType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}