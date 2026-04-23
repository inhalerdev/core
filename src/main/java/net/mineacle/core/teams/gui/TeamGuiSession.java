package net.mineacle.core.teams.gui;

import net.mineacle.core.teams.model.TeamSortType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamGuiSession {

    private static final Map<UUID, Integer> PAGE_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, TeamSortType> SORT_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, String> MEMBER_SEARCH_BY_PLAYER = new HashMap<>();
    private static final Map<UUID, String> INVITE_SEARCH_BY_PLAYER = new HashMap<>();

    private TeamGuiSession() {
    }

    public static int getPage(UUID playerId) {
        return PAGE_BY_PLAYER.getOrDefault(playerId, 0);
    }

    public static void setPage(UUID playerId, int page) {
        PAGE_BY_PLAYER.put(playerId, Math.max(0, page));
    }

    public static TeamSortType getSort(UUID playerId) {
        return SORT_BY_PLAYER.getOrDefault(playerId, TeamSortType.JOIN_DATE);
    }

    public static void setSort(UUID playerId, TeamSortType sortType) {
        SORT_BY_PLAYER.put(playerId, sortType);
    }

    public static String getMemberSearch(UUID playerId) {
        return MEMBER_SEARCH_BY_PLAYER.getOrDefault(playerId, "");
    }

    public static void setMemberSearch(UUID playerId, String query) {
        if (query == null || query.isBlank()) {
            MEMBER_SEARCH_BY_PLAYER.remove(playerId);
            return;
        }
        MEMBER_SEARCH_BY_PLAYER.put(playerId, query.trim());
    }

    public static String getInviteSearch(UUID playerId) {
        return INVITE_SEARCH_BY_PLAYER.getOrDefault(playerId, "");
    }

    public static void setInviteSearch(UUID playerId, String query) {
        if (query == null || query.isBlank()) {
            INVITE_SEARCH_BY_PLAYER.remove(playerId);
            return;
        }
        INVITE_SEARCH_BY_PLAYER.put(playerId, query.trim());
    }

    public static void clear(UUID playerId) {
        PAGE_BY_PLAYER.remove(playerId);
        SORT_BY_PLAYER.remove(playerId);
        MEMBER_SEARCH_BY_PLAYER.remove(playerId);
        INVITE_SEARCH_BY_PLAYER.remove(playerId);
    }
}