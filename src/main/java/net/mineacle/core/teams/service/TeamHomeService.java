package net.mineacle.core.teams.service;

import net.mineacle.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class TeamHomeService {

    private final Core core;
    private final TeamService teamService;

    public TeamHomeService(Core core, TeamService teamService) {
        this.core = core;
        this.teamService = teamService;
    }

    public Location getTeamHome(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }

        String base = "team-homes." + teamId;
        if (!core.getTeamsConfig().contains(base + ".world")) {
            return null;
        }

        String worldName = core.getTeamsConfig().getString(base + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = core.getTeamsConfig().getDouble(base + ".x");
        double y = core.getTeamsConfig().getDouble(base + ".y");
        double z = core.getTeamsConfig().getDouble(base + ".z");
        float yaw = (float) core.getTeamsConfig().getDouble(base + ".yaw");
        float pitch = (float) core.getTeamsConfig().getDouble(base + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    public Location getPlayerTeamHome(java.util.UUID playerId) {
        String teamId = teamService.getPlayerTeamId(playerId);
        if (teamId == null) {
            return null;
        }
        return getTeamHome(teamId);
    }

    public boolean setTeamHome(String teamId, Location location) {
        if (teamId == null || location == null || location.getWorld() == null) {
            return false;
        }

        String base = "team-homes." + teamId;
        core.getTeamsConfig().set(base + ".world", location.getWorld().getName());
        core.getTeamsConfig().set(base + ".x", location.getX());
        core.getTeamsConfig().set(base + ".y", location.getY());
        core.getTeamsConfig().set(base + ".z", location.getZ());
        core.getTeamsConfig().set(base + ".yaw", location.getYaw());
        core.getTeamsConfig().set(base + ".pitch", location.getPitch());
        core.saveTeamsFile();
        return true;
    }

    public boolean deleteTeamHome(String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return false;
        }

        core.getTeamsConfig().set("team-homes." + teamId, null);
        core.saveTeamsFile();
        return true;
    }

    public boolean hasTeamHome(String teamId) {
        return getTeamHome(teamId) != null;
    }
}