package net.mineacle.core.teams.service;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamBanRecord;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TeamBanService {

    public static final long DEFAULT_BAN_DURATION_MILLIS = 7L * 24L * 60L * 60L * 1000L;

    private final Core core;

    public TeamBanService(Core core) {
        this.core = core;
    }

    public TeamBanRecord getBan(String teamId, UUID playerId) {
        if (teamId == null || teamId.isBlank() || playerId == null) {
            return null;
        }

        String base = "bans." + teamId + "." + playerId;
        FileConfiguration config = core.getTeamsConfig();

        if (!config.contains(base + ".expires-at")) {
            return null;
        }

        String bannedByRaw = config.getString(base + ".banned-by");
        long createdAt = config.getLong(base + ".created-at", 0L);
        long expiresAt = config.getLong(base + ".expires-at", 0L);

        if (bannedByRaw == null || bannedByRaw.isBlank() || expiresAt <= 0L) {
            return null;
        }

        TeamBanRecord record = new TeamBanRecord(
                teamId,
                playerId,
                UUID.fromString(bannedByRaw),
                createdAt,
                expiresAt
        );

        if (record.expired()) {
            clearBan(teamId, playerId);
            return null;
        }

        return record;
    }

    public List<TeamBanRecord> getActiveBans(String teamId) {
        List<TeamBanRecord> bans = new ArrayList<>();

        if (teamId == null || teamId.isBlank()) {
            return bans;
        }

        ConfigurationSection section = core.getTeamsConfig().getConfigurationSection("bans." + teamId);
        if (section == null) {
            return bans;
        }

        for (String playerIdRaw : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(playerIdRaw);
                TeamBanRecord record = getBan(teamId, playerId);
                if (record != null) {
                    bans.add(record);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return bans;
    }

    public boolean isBanned(String teamId, UUID playerId) {
        return getBan(teamId, playerId) != null;
    }

    public void banForDefaultDuration(String teamId, UUID playerId, UUID bannedBy) {
        ban(teamId, playerId, bannedBy, DEFAULT_BAN_DURATION_MILLIS);
    }

    public void ban(String teamId, UUID playerId, UUID bannedBy, long durationMillis) {
        if (teamId == null || teamId.isBlank() || playerId == null || bannedBy == null || durationMillis <= 0L) {
            return;
        }

        long now = System.currentTimeMillis();
        String base = "bans." + teamId + "." + playerId;

        core.getTeamsConfig().set(base + ".banned-by", bannedBy.toString());
        core.getTeamsConfig().set(base + ".created-at", now);
        core.getTeamsConfig().set(base + ".expires-at", now + durationMillis);
        core.saveTeamsFile();
    }

    public void clearBan(String teamId, UUID playerId) {
        if (teamId == null || teamId.isBlank() || playerId == null) {
            return;
        }

        core.getTeamsConfig().set("bans." + teamId + "." + playerId, null);
        core.saveTeamsFile();
    }
}