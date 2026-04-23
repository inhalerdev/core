package net.mineacle.core.homes.service;

import net.kyori.adventure.text.Component;
import net.mineacle.core.Core;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TeleportService {

    private final Core core;
    private final Map<UUID, Location> teleportOrigins = new HashMap<>();
    private final Set<UUID> teleporting = new HashSet<>();

    public TeleportService(Core core) {
        this.core = core;
    }

    public boolean isTeleporting(UUID uuid) {
        return teleporting.contains(uuid);
    }

    public void cancel(UUID uuid) {
        teleporting.remove(uuid);
        teleportOrigins.remove(uuid);
    }

    public void begin(Player player, String targetName, Runnable action) {
        UUID uuid = player.getUniqueId();

        if (teleporting.contains(uuid)) {
            return;
        }

        int delaySeconds = core.getConfig().getInt("homes.teleport.delay-seconds", 5);

        teleporting.add(uuid);
        teleportOrigins.put(uuid, player.getLocation());

        if (delaySeconds <= 0) {
            teleporting.remove(uuid);
            teleportOrigins.remove(uuid);
            action.run();
            return;
        }

        new BukkitRunnable() {
            int countdown = delaySeconds;

            @Override
            public void run() {
                if (!teleporting.contains(uuid)) {
                    cancel();
                    return;
                }

                if (!player.isOnline()) {
                    teleporting.remove(uuid);
                    teleportOrigins.remove(uuid);
                    cancel();
                    return;
                }

                if (countdown <= 0) {
                    teleporting.remove(uuid);
                    teleportOrigins.remove(uuid);
                    action.run();
                    cancel();
                    return;
                }

                String message = core.getMessage("homes.teleporting")
                        .replace("%target%", targetName)
                        .replace("%seconds%", String.valueOf(countdown));

                player.sendActionBar(Component.text(message));
                countdown--;
            }
        }.runTaskTimer(core, 0L, 20L);
    }

    public void handleMove(Player player, Location to) {
        if (!core.getConfig().getBoolean("homes.teleport.cancel-on-move", true)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        if (!teleporting.contains(uuid)) {
            return;
        }

        Location origin = teleportOrigins.get(uuid);
        if (origin == null || to == null) {
            return;
        }

        if (!sameBlock(origin, to)) {
            teleporting.remove(uuid);
            teleportOrigins.remove(uuid);

            String message = core.getMessage("homes.teleport-cancelled-move");
            player.sendActionBar(Component.text(message));
            player.sendMessage(message);
        }
    }

    private boolean sameBlock(Location a, Location b) {
        if (a.getWorld() == null || b.getWorld() == null) {
            return false;
        }

        if (!a.getWorld().getName().equalsIgnoreCase(b.getWorld().getName())) {
            return false;
        }

        return a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}