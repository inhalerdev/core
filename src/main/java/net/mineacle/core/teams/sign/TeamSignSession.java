package net.mineacle.core.teams.sign;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TeamSignSession {

    public record Session(
            TeamSignInputType type,
            Location signLocation,
            BlockData previousBlockData
    ) {}

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private TeamSignSession() {
    }

    public static void set(UUID playerId, Session session) {
        SESSIONS.put(playerId, session);
    }

    public static Session get(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    public static Session remove(UUID playerId) {
        return SESSIONS.remove(playerId);
    }

    public static boolean has(UUID playerId) {
        return SESSIONS.containsKey(playerId);
    }
}