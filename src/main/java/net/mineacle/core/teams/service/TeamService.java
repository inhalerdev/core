package net.mineacle.core.teams.service;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.model.TeamRole;
import net.mineacle.core.teams.model.TeamSortType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TeamService {

    private final Core core;

    public TeamService(Core core) {
        this.core = core;
    }

    public boolean hasTeam(UUID playerId) {
        return getPlayerTeamId(playerId) != null;
    }

    public String getPlayerTeamId(UUID playerId) {
        String teamId = core.getTeamsConfig().getString("members." + playerId + ".team");
        if (teamId == null || teamId.isBlank()) {
            return null;
        }
        return teamId;
    }

    public TeamMemberRecord getMember(UUID playerId) {
        String teamId = getPlayerTeamId(playerId);
        if (teamId == null) {
            return null;
        }

        String roleRaw = core.getTeamsConfig().getString("members." + playerId + ".role", "MEMBER");
        long joinedAt = core.getTeamsConfig().getLong("members." + playerId + ".joined-at", 0L);

        return new TeamMemberRecord(
                playerId,
                teamId,
                TeamRole.valueOf(roleRaw.toUpperCase(Locale.ROOT)),
                joinedAt
        );
    }

    public TeamRecord getTeamById(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }

        FileConfiguration config = core.getTeamsConfig();
        String base = "teams." + teamId;

        if (!config.contains(base + ".name")) {
            return null;
        }

        String name = config.getString(base + ".name");
        String founderRaw = config.getString(base + ".founder");
        boolean friendlyFire = config.getBoolean(base + ".friendly-fire", false);
        long createdAt = config.getLong(base + ".created-at", 0L);

        if (founderRaw == null || founderRaw.isBlank()) {
            return null;
        }

        return new TeamRecord(teamId, name, UUID.fromString(founderRaw), friendlyFire, createdAt);
    }

    public TeamRecord getTeamByPlayer(UUID playerId) {
        return getTeamById(getPlayerTeamId(playerId));
    }

    public TeamRecord getTeamByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        FileConfiguration config = core.getTeamsConfig();
        if (!config.contains("teams")) {
            return null;
        }

        for (String teamId : config.getConfigurationSection("teams").getKeys(false)) {
            String storedName = config.getString("teams." + teamId + ".name");
            if (storedName != null && storedName.equalsIgnoreCase(name.trim())) {
                return getTeamById(teamId);
            }
        }

        return null;
    }

    public boolean isValidTeamName(String name) {
        if (name == null) {
            return false;
        }

        String trimmed = name.trim();
        return !trimmed.isBlank() && trimmed.length() <= 16;
    }

    public boolean createTeam(UUID founderId, String teamName) {
        if (!isValidTeamName(teamName)) {
            return false;
        }

        if (hasTeam(founderId)) {
            return false;
        }

        if (getTeamByName(teamName) != null) {
            return false;
        }

        String teamId = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        FileConfiguration config = core.getTeamsConfig();

        config.set("teams." + teamId + ".name", teamName.trim());
        config.set("teams." + teamId + ".founder", founderId.toString());
        config.set("teams." + teamId + ".friendly-fire", false);
        config.set("teams." + teamId + ".created-at", now);

        config.set("members." + founderId + ".team", teamId);
        config.set("members." + founderId + ".role", TeamRole.FOUNDER.name());
        config.set("members." + founderId + ".joined-at", now);

        core.saveTeamsFile();
        return true;
    }

    public boolean addMember(String teamId, UUID playerId, TeamRole role) {
        TeamRecord team = getTeamById(teamId);
        if (team == null || hasTeam(playerId)) {
            return false;
        }

        FileConfiguration config = core.getTeamsConfig();
        config.set("members." + playerId + ".team", teamId);
        config.set("members." + playerId + ".role", role.name());
        config.set("members." + playerId + ".joined-at", System.currentTimeMillis());
        core.saveTeamsFile();
        return true;
    }

    public boolean removeMember(UUID playerId) {
        if (!hasTeam(playerId)) {
            return false;
        }

        core.getTeamsConfig().set("members." + playerId, null);
        core.saveTeamsFile();
        return true;
    }

    public boolean disbandTeam(String teamId) {
        TeamRecord team = getTeamById(teamId);
        if (team == null) {
            return false;
        }

        FileConfiguration config = core.getTeamsConfig();

        for (UUID memberId : getTeamMembers(teamId)) {
            config.set("members." + memberId, null);
            config.set("invites." + memberId, null);
        }

        config.set("team-homes." + teamId, null);
        config.set("teams." + teamId, null);

        core.saveTeamsFile();
        return true;
    }

    public List<UUID> getTeamMembers(String teamId) {
        List<UUID> members = new ArrayList<>();
        FileConfiguration config = core.getTeamsConfig();

        if (!config.contains("members")) {
            return members;
        }

        for (String playerIdRaw : config.getConfigurationSection("members").getKeys(false)) {
            String memberTeamId = config.getString("members." + playerIdRaw + ".team");
            if (teamId.equals(memberTeamId)) {
                members.add(UUID.fromString(playerIdRaw));
            }
        }

        return members;
    }

    public List<UUID> getSortedTeamMembers(String teamId, TeamSortType sortType) {
        List<UUID> members = new ArrayList<>(getTeamMembers(teamId));

        Comparator<UUID> comparator = switch (sortType) {
            case JOIN_DATE -> Comparator.comparingLong(uuid -> {
                TeamMemberRecord member = getMember(uuid);
                return member == null ? Long.MAX_VALUE : member.joinedAt();
            });
            case PERMISSIONS -> Comparator.comparingInt(uuid -> roleWeight(getMember(uuid) == null ? TeamRole.MEMBER : getMember(uuid).role()));
            case ALPHABETICALLY -> Comparator.comparing(uuid -> {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String name = offlinePlayer.getName();
                return name == null ? uuid.toString() : name.toLowerCase(Locale.ROOT);
            });
            case ONLINE_MEMBERS -> Comparator
                    .comparing((UUID uuid) -> Bukkit.getPlayer(uuid) == null)
                    .thenComparing(uuid -> {
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                        String name = offlinePlayer.getName();
                        return name == null ? uuid.toString() : name.toLowerCase(Locale.ROOT);
                    });
        };

        members.sort(comparator);
        return members;
    }

    public int getTeamMemberCount(String teamId) {
        return getTeamMembers(teamId).size();
    }

    public boolean isFounder(UUID playerId) {
        TeamMemberRecord member = getMember(playerId);
        return member != null && member.role() == TeamRole.FOUNDER;
    }

    public boolean isAdmin(UUID playerId) {
        TeamMemberRecord member = getMember(playerId);
        return member != null && (member.role() == TeamRole.FOUNDER || member.role() == TeamRole.ADMIN);
    }

    public boolean setFriendlyFire(String teamId, boolean enabled) {
        TeamRecord team = getTeamById(teamId);
        if (team == null) {
            return false;
        }

        core.getTeamsConfig().set("teams." + teamId + ".friendly-fire", enabled);
        core.saveTeamsFile();
        return true;
    }

    public boolean setMemberRole(UUID playerId, TeamRole role) {
        TeamMemberRecord member = getMember(playerId);
        if (member == null) {
            return false;
        }

        core.getTeamsConfig().set("members." + playerId + ".role", role.name());
        core.saveTeamsFile();
        return true;
    }

    public boolean transferFounder(String teamId, UUID oldFounder, UUID newFounder) {
        TeamRecord team = getTeamById(teamId);
        if (team == null) {
            return false;
        }

        TeamMemberRecord oldMember = getMember(oldFounder);
        TeamMemberRecord newMember = getMember(newFounder);

        if (oldMember == null || newMember == null) {
            return false;
        }

        if (!oldMember.teamId().equals(teamId) || !newMember.teamId().equals(teamId)) {
            return false;
        }

        if (oldMember.role() != TeamRole.FOUNDER) {
            return false;
        }

        core.getTeamsConfig().set("teams." + teamId + ".founder", newFounder.toString());
        core.getTeamsConfig().set("members." + oldFounder + ".role", TeamRole.ADMIN.name());
        core.getTeamsConfig().set("members." + newFounder + ".role", TeamRole.FOUNDER.name());
        core.saveTeamsFile();
        return true;
    }

    private int roleWeight(TeamRole role) {
        return switch (role) {
            case FOUNDER -> 0;
            case ADMIN -> 1;
            case MEMBER -> 2;
        };
    }
}