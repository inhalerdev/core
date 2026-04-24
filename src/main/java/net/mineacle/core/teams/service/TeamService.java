package net.mineacle.core.teams.service;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamBannerColor;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamNameColor;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.model.TeamRole;
import net.mineacle.core.teams.model.TeamSortType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

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

    public TeamRecord getTeamByPlayer(UUID playerId) {
        TeamMemberRecord member = getMember(playerId);
        if (member == null) {
            return null;
        }

        return getTeamById(member.teamId());
    }

    public String getPlayerTeamId(UUID playerId) {
        TeamMemberRecord member = getMember(playerId);
        if (member == null) {
            return null;
        }

        return member.teamId();
    }

    public TeamRecord getTeamById(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }

        String base = "teams." + teamId;
        if (!core.getTeamsConfig().contains(base + ".name")) {
            return null;
        }

        String name = core.getTeamsConfig().getString(base + ".name");
        String founderRaw = core.getTeamsConfig().getString(base + ".founder");
        boolean friendlyFire = core.getTeamsConfig().getBoolean(base + ".friendly-fire", false);
        TeamBannerColor bannerColor = TeamBannerColor.fromString(core.getTeamsConfig().getString(base + ".banner-color", "PURPLE"));
        String nameColor = TeamNameColor.fromString(core.getTeamsConfig().getString(base + ".name-color", "&f")).colorCode();

        if (name == null || founderRaw == null || founderRaw.isBlank()) {
            return null;
        }

        return new TeamRecord(
                teamId,
                name,
                UUID.fromString(founderRaw),
                friendlyFire,
                bannerColor,
                nameColor
        );
    }

    public TeamRecord getTeamByName(String teamName) {
        ConfigurationSection section = core.getTeamsConfig().getConfigurationSection("teams");
        if (section == null) {
            return null;
        }

        for (String teamId : section.getKeys(false)) {
            TeamRecord record = getTeamById(teamId);
            if (record != null && record.name().equalsIgnoreCase(teamName)) {
                return record;
            }
        }

        return null;
    }

    public boolean hasTeam(UUID playerId) {
        return getMember(playerId) != null;
    }

    public boolean createTeam(UUID founderId, String teamName) {
        if (hasTeam(founderId) || getTeamByName(teamName) != null || !isValidTeamName(teamName)) {
            return false;
        }

        String teamId = UUID.randomUUID().toString();
        String base = "teams." + teamId;

        core.getTeamsConfig().set(base + ".name", teamName);
        core.getTeamsConfig().set(base + ".founder", founderId.toString());
        core.getTeamsConfig().set(base + ".friendly-fire", false);
        core.getTeamsConfig().set(base + ".banner-color", TeamBannerColor.PURPLE.name());
        core.getTeamsConfig().set(base + ".name-color", TeamNameColor.WHITE.colorCode());

        addMember(teamId, founderId, TeamRole.FOUNDER);
        core.saveTeamsFile();
        return true;
    }

    public boolean addMember(String teamId, UUID playerId, TeamRole role) {
        if (getTeamById(teamId) == null || hasTeam(playerId)) {
            return false;
        }

        String base = "members." + playerId;
        core.getTeamsConfig().set(base + ".team", teamId);
        core.getTeamsConfig().set(base + ".role", role.name());
        core.getTeamsConfig().set(base + ".joined-at", System.currentTimeMillis());
        core.saveTeamsFile();
        return true;
    }

    public void removeMember(UUID playerId) {
        core.getTeamsConfig().set("members." + playerId, null);
        core.saveTeamsFile();
    }

    public void disbandTeam(String teamId) {
        for (UUID memberId : getTeamMembers(teamId)) {
            core.getTeamsConfig().set("members." + memberId, null);
        }

        core.getTeamsConfig().set("teams." + teamId, null);
        core.getTeamsConfig().set("team-homes." + teamId, null);
        core.getTeamsConfig().set("bans." + teamId, null);
        core.saveTeamsFile();
    }

    public TeamMemberRecord getMember(UUID playerId) {
        String base = "members." + playerId;
        String teamId = core.getTeamsConfig().getString(base + ".team");
        String roleRaw = core.getTeamsConfig().getString(base + ".role");
        long joinedAt = core.getTeamsConfig().getLong(base + ".joined-at", 0L);

        if (teamId == null || roleRaw == null || roleRaw.isBlank()) {
            return null;
        }

        TeamRole role;
        try {
            role = TeamRole.valueOf(roleRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        return new TeamMemberRecord(playerId, teamId, role, joinedAt);
    }

    public boolean isFounder(UUID playerId) {
        TeamMemberRecord member = getMember(playerId);
        return member != null && member.role() == TeamRole.FOUNDER;
    }

    public boolean isAdmin(UUID playerId) {
        TeamMemberRecord member = getMember(playerId);
        return member != null && (member.role() == TeamRole.FOUNDER || member.role() == TeamRole.ADMIN);
    }

    public void setFriendlyFire(String teamId, boolean enabled) {
        core.getTeamsConfig().set("teams." + teamId + ".friendly-fire", enabled);
        core.saveTeamsFile();
    }

    public void setMemberRole(UUID playerId, TeamRole role) {
        core.getTeamsConfig().set("members." + playerId + ".role", role.name());
        core.saveTeamsFile();
    }

    public boolean transferFounder(String teamId, UUID oldFounder, UUID newFounder) {
        TeamMemberRecord oldMember = getMember(oldFounder);
        TeamMemberRecord newMember = getMember(newFounder);
        TeamRecord team = getTeamById(teamId);

        if (team == null || oldMember == null || newMember == null) {
            return false;
        }

        if (!team.teamId().equals(oldMember.teamId()) || !team.teamId().equals(newMember.teamId())) {
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

    public List<UUID> getTeamMembers(String teamId) {
        List<UUID> members = new ArrayList<>();
        ConfigurationSection section = core.getTeamsConfig().getConfigurationSection("members");
        if (section == null) {
            return members;
        }

        for (String playerIdRaw : section.getKeys(false)) {
            try {
                TeamMemberRecord member = getMember(UUID.fromString(playerIdRaw));
                if (member != null && member.teamId().equals(teamId)) {
                    members.add(member.playerId());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        return members;
    }

    public List<UUID> getSortedTeamMembers(String teamId, TeamSortType sortType) {
        List<UUID> members = new ArrayList<>(getTeamMembers(teamId));

        Comparator<UUID> comparator = switch (sortType) {
            case ALPHABETICAL -> Comparator.comparing(uuid -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                return player.getName() == null ? uuid.toString() : player.getName().toLowerCase(Locale.ROOT);
            });

            case ONLINE_MEMBERS -> Comparator.<UUID, Integer>comparing(uuid -> {
                org.bukkit.entity.Player player = Bukkit.getPlayer(uuid);
                return player != null && player.isOnline() ? 0 : 1;
            }).thenComparing(uuid -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                return player.getName() == null ? uuid.toString() : player.getName().toLowerCase(Locale.ROOT);
            });

            case PERMISSIONS -> Comparator.comparing((UUID uuid) -> {
                TeamMemberRecord member = getMember(uuid);
                if (member == null) {
                    return 99;
                }

                return switch (member.role()) {
                    case FOUNDER -> 0;
                    case ADMIN -> 1;
                    case MEMBER -> 2;
                };
            }).thenComparing(uuid -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                return player.getName() == null ? uuid.toString() : player.getName().toLowerCase(Locale.ROOT);
            });

            case JOIN_DATE -> Comparator.comparingLong(uuid -> {
                TeamMemberRecord member = getMember(uuid);
                return member == null ? Long.MAX_VALUE : member.joinedAt();
            });

            case MONEY -> Comparator.comparing(uuid -> {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                return player.getName() == null ? uuid.toString() : player.getName().toLowerCase(Locale.ROOT);
            });
        };

        members.sort(comparator);
        return members;
    }

    public boolean isValidTeamName(String teamName) {
        if (teamName == null) {
            return false;
        }

        String trimmed = teamName.trim();
        if (trimmed.length() < 3 || trimmed.length() > 16) {
            return false;
        }

        if (trimmed.contains("&") || trimmed.contains("§") || trimmed.contains("#")) {
            return false;
        }

        return trimmed.matches("[A-Za-z0-9 _-]+");
    }

    public TeamBannerColor getBannerColor(String teamId) {
        TeamRecord team = getTeamById(teamId);
        return team == null ? TeamBannerColor.PURPLE : team.bannerColor();
    }

    public void setBannerColor(String teamId, TeamBannerColor color) {
        core.getTeamsConfig().set("teams." + teamId + ".banner-color", color.name());
        core.saveTeamsFile();
    }

    public String getNameColor(String teamId) {
        TeamRecord team = getTeamById(teamId);
        return team == null ? TeamNameColor.WHITE.colorCode() : team.nameColor();
    }

    public void setNameColor(String teamId, String colorCode) {
        core.getTeamsConfig().set("teams." + teamId + ".name-color", TeamNameColor.fromString(colorCode).colorCode());
        core.saveTeamsFile();
    }

    public String formatTeamName(TeamRecord team) {
        return ChatColor.translateAlternateColorCodes('&', team.nameColor() + team.name());
    }

    public String colorizePlayerName(TeamRecord team, String playerName) {
        return ChatColor.translateAlternateColorCodes('&', team.nameColor() + playerName);
    }
}