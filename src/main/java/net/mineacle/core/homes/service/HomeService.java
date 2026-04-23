package net.mineacle.core.homes.service;

import net.mineacle.core.Core;
import net.mineacle.core.homes.model.HomeRecord;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HomeService {

    private final Core core;

    public HomeService(Core core) {
        this.core = core;
    }

    public int getMaxHomes(Player player) {
        FileConfiguration config = core.getConfig();

        int defaultMax = config.getInt("homes.max-homes.default", 2);
        int plusMax = config.getInt("homes.max-homes.plus", 5);
        String plusPermission = config.getString("homes.plus-permission", "mineaclehomes.plus");

        if (plusPermission != null && !plusPermission.isBlank() && player.hasPermission(plusPermission)) {
            return plusMax;
        }

        return defaultMax;
    }

    public boolean exists(UUID uuid, int id) {
        return get(uuid, id) != null;
    }

    public Location get(UUID uuid, int id) {
        String base = path(uuid, id);
        FileConfiguration homes = core.getHomesConfig();

        if (!homes.contains(base + ".world")) {
            return null;
        }

        HomeRecord record = new HomeRecord(
                homes.getString(base + ".world"),
                homes.getDouble(base + ".x"),
                homes.getDouble(base + ".y"),
                homes.getDouble(base + ".z"),
                (float) homes.getDouble(base + ".yaw"),
                (float) homes.getDouble(base + ".pitch")
        );

        return record.toLocation();
    }

    public void set(UUID uuid, int id, Location location) {
        set(uuid, id, location, getDefaultDisplayName(id));
    }

    public void set(UUID uuid, int id, Location location, String displayName) {
        String base = path(uuid, id);
        FileConfiguration homes = core.getHomesConfig();
        HomeRecord record = HomeRecord.fromLocation(location);

        homes.set(base + ".world", record.worldName());
        homes.set(base + ".x", record.x());
        homes.set(base + ".y", record.y());
        homes.set(base + ".z", record.z());
        homes.set(base + ".yaw", record.yaw());
        homes.set(base + ".pitch", record.pitch());
        homes.set(base + ".name", sanitizeName(displayName, id));

        core.saveHomesFile();
    }

    public void rename(UUID uuid, int id, String newName) {
        if (!exists(uuid, id)) {
            return;
        }

        core.getHomesConfig().set(path(uuid, id) + ".name", sanitizeName(newName, id));
        core.saveHomesFile();
    }

    public void delete(UUID uuid, int id) {
        core.getHomesConfig().set(path(uuid, id), null);
        core.saveHomesFile();
    }

    public String getDisplayName(UUID uuid, int id) {
        String stored = core.getHomesConfig().getString(path(uuid, id) + ".name");
        if (stored == null || stored.isBlank()) {
            return getDefaultDisplayName(id);
        }
        return stored;
    }

    public String getDefaultDisplayName(int id) {
        return "Home " + id;
    }

    public Integer findHomeIdByName(UUID uuid, int maxHomes, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        for (int id = 1; id <= maxHomes; id++) {
            if (!exists(uuid, id)) {
                continue;
            }

            String displayName = getDisplayName(uuid, id);
            if (displayName.equalsIgnoreCase(input.trim())) {
                return id;
            }
        }

        try {
            int parsed = Integer.parseInt(input.trim());
            if (parsed >= 1 && parsed <= 5 && exists(uuid, parsed)) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }

        return null;
    }

    public Integer findHomeIdByStoredOrDefaultName(UUID uuid, int maxHomes, String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        for (int id = 1; id <= maxHomes; id++) {
            String displayName = getDisplayName(uuid, id);
            if (displayName.equalsIgnoreCase(input.trim())) {
                return id;
            }
        }

        try {
            int parsed = Integer.parseInt(input.trim());
            if (parsed >= 1 && parsed <= 5) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }

        return null;
    }

    public Integer findFirstEmptySlot(Player player) {
        int max = getMaxHomes(player);
        UUID uuid = player.getUniqueId();

        for (int id = 1; id <= max; id++) {
            if (!exists(uuid, id)) {
                return id;
            }
        }

        return null;
    }

    public Integer findByName(UUID uuid, int maxHomes, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        for (int id = 1; id <= maxHomes; id++) {
            if (!exists(uuid, id)) {
                continue;
            }

            if (getDisplayName(uuid, id).equalsIgnoreCase(name.trim())) {
                return id;
            }
        }

        return null;
    }

    public List<String> getSavedHomeNames(Player player) {
        List<String> names = new ArrayList<>();
        UUID uuid = player.getUniqueId();
        int max = getMaxHomes(player);

        for (int id = 1; id <= max; id++) {
            if (exists(uuid, id)) {
                names.add(getDisplayName(uuid, id));
            }
        }

        return names;
    }

    public boolean isValidName(String name) {
        if (name == null) {
            return false;
        }

        String trimmed = name.trim();
        return !trimmed.isBlank() && trimmed.length() <= 24;
    }

    public String sanitizeName(String name, int fallbackId) {
        if (!isValidName(name)) {
            return getDefaultDisplayName(fallbackId);
        }

        return name.trim();
    }

    private String path(UUID uuid, int id) {
        return "homes." + uuid + "." + id;
    }
}