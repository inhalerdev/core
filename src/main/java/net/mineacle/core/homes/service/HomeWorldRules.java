package net.mineacle.core.homes.service;

import net.mineacle.core.Core;
import org.bukkit.Location;

import java.util.List;

public final class HomeWorldRules {

    private final Core core;

    public HomeWorldRules(Core core) {
        this.core = core;
    }

    public boolean isBlockedWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        List<String> blocked = core.getConfig().getStringList("homes.blocked-set-worlds");
        String worldName = location.getWorld().getName();

        return blocked.stream().anyMatch(entry -> entry.equalsIgnoreCase(worldName));
    }
}