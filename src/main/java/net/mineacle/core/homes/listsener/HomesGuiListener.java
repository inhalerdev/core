package net.mineacle.core.homes.listener;

import net.kyori.adventure.text.Component;
import net.mineacle.core.Core;
import net.mineacle.core.homes.gui.ConfirmDeleteHomeGui;
import net.mineacle.core.homes.gui.HomesMainGui;
import net.mineacle.core.homes.service.HomeService;
import net.mineacle.core.homes.service.HomeWorldRules;
import net.mineacle.core.homes.service.TeleportService;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.metadata.FixedMetadataValue;

public final class HomesGuiListener implements Listener {

    private static final String META_HOME_PENDING = "mh_pendingDelete";
    private static final String META_HOME_CONFIRM = "mh_deleteConfirm";

    private final Core core;
    private final HomeService homeService;
    private final HomeWorldRules worldRules;
    private final TeleportService teleportService;

    public HomesGuiListener(Core core, HomeService homeService, HomeWorldRules worldRules, TeleportService teleportService) {
        this.core = core;
        this.homeService = homeService;
        this.worldRules = worldRules;
        this.teleportService = teleportService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity whoClicked = event.getWhoClicked();
        if (!(whoClicked instanceof Player player)) {
            return;
        }

        String homesTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', core.getMessage("homes.gui.title"));
        String deleteTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', core.getMessage("homes.gui.delete-title"));
        int slot = event.getRawSlot();

        if (event.getView().getTitle().equals(homesTitle)) {
            event.setCancelled(true);

            if (slot < 0 || slot >= event.getInventory().getSize()) {
                return;
            }

            for (int i = 0; i < HomesMainGui.BED_SLOTS.length; i++) {
                if (slot == HomesMainGui.BED_SLOTS[i]) {
                    handleHomeBedClick(player, i + 1);
                    return;
                }
            }

            for (int i = 0; i < HomesMainGui.DYE_SLOTS.length; i++) {
                if (slot == HomesMainGui.DYE_SLOTS[i]) {
                    handleHomeDyeClick(player, i + 1);
                    return;
                }
            }

            handleTeamHomePlaceholderClick(player, slot);
            return;
        }

        if (event.getView().getTitle().equals(deleteTitle)) {
            event.setCancelled(true);
            handlePlayerDeleteConfirm(player, slot);
        }
    }

    private void handleHomeBedClick(Player player, int id) {
        if (homeService.getMaxHomes(player) < id) {
            sendUpgradeMessage(player);
            return;
        }

        if (homeService.exists(player.getUniqueId(), id)) {
            Location target = homeService.get(player.getUniqueId(), id);
            if (target == null) {
                player.sendMessage(core.getMessage("homes.not-set").replace("%home%", homeService.getDisplayName(id)));
                return;
            }

            player.closeInventory();

            teleportService.begin(player, homeService.getDisplayName(id), () -> {
                player.teleport(target);

                String message = core.getMessage("homes.teleported")
                        .replace("%home%", homeService.getDisplayName(id));
                player.sendActionBar(Component.text(message));
                player.sendMessage(message);
            });
            return;
        }

        if (worldRules.isBlockedWorld(player.getLocation())) {
            String message = core.getMessage("homes.blocked-world");
            player.sendActionBar(Component.text(message));
            player.sendMessage(message);
            return;
        }

        homeService.set(player.getUniqueId(), id, player.getLocation());

        String message = core.getMessage("homes.set")
                .replace("%home%", homeService.getDisplayName(id));
        player.sendActionBar(Component.text(message));
        player.sendMessage(message);

        HomesMainGui.open(core, player, homeService);
    }

    private void handleHomeDyeClick(Player player, int id) {
        if (homeService.getMaxHomes(player) < id) {
            sendUpgradeMessage(player);
            return;
        }

        if (!homeService.exists(player.getUniqueId(), id)) {
            if (worldRules.isBlockedWorld(player.getLocation())) {
                String message = core.getMessage("homes.blocked-world");
                player.sendActionBar(Component.text(message));
                player.sendMessage(message);
                return;
            }

            homeService.set(player.getUniqueId(), id, player.getLocation());

            String message = core.getMessage("homes.set")
                    .replace("%home%", homeService.getDisplayName(id));
            player.sendActionBar(Component.text(message));
            player.sendMessage(message);

            HomesMainGui.open(core, player, homeService);
            return;
        }

        player.setMetadata(META_HOME_PENDING, new FixedMetadataValue(core, id));
        player.setMetadata(META_HOME_CONFIRM, new FixedMetadataValue(core, 0));
        ConfirmDeleteHomeGui.openPlayerDelete(core, player, id);
    }

    private void handlePlayerDeleteConfirm(Player player, int slot) {
        if (!player.hasMetadata(META_HOME_PENDING)) {
            player.closeInventory();
            return;
        }

        int id = player.getMetadata(META_HOME_PENDING).get(0).asInt();

        if (slot == 11) {
            clearPlayerDeleteMeta(player);
            player.closeInventory();
            HomesMainGui.open(core, player, homeService);

            String message = core.getMessage("homes.delete-cancelled");
            player.sendActionBar(Component.text(message));
            player.sendMessage(message);
            return;
        }

        if (slot != 15) {
            return;
        }

        int confirmValue = player.getMetadata(META_HOME_CONFIRM).get(0).asInt();

        if (confirmValue == id) {
            homeService.delete(player.getUniqueId(), id);
            clearPlayerDeleteMeta(player);
            player.closeInventory();

            String message = core.getMessage("homes.deleted")
                    .replace("%home%", homeService.getDisplayName(id));
            player.sendActionBar(Component.text(message));
            player.sendMessage(message);
            return;
        }

        player.setMetadata(META_HOME_CONFIRM, new FixedMetadataValue(core, id));

        String message = core.getMessage("homes.delete-confirm");
        player.sendActionBar(Component.text(message));
        player.sendMessage(message);

        int timeout = core.getConfig().getInt("homes.delete-confirm.timeout-seconds", 5);
        core.getServer().getScheduler().runTaskLater(core, () -> {
            if (!player.isOnline()) {
                return;
            }

            if (player.hasMetadata(META_HOME_CONFIRM)
                    && player.getMetadata(META_HOME_CONFIRM).get(0).asInt() == id) {
                clearPlayerDeleteMeta(player);

                String timeoutMessage = core.getMessage("homes.delete-timeout");
                player.sendActionBar(Component.text(timeoutMessage));
                player.sendMessage(timeoutMessage);
            }
        }, 20L * Math.max(1, timeout));
    }

    private void handleTeamHomePlaceholderClick(Player player, int slot) {
        int bannerSlot = core.getConfig().getInt("homes.team-home.banner-slot", 10);
        int dyeSlot = core.getConfig().getInt("homes.team-home.dye-slot", 19);

        if (slot != bannerSlot && slot != dyeSlot) {
            return;
        }

        if (!core.getConfig().getBoolean("homes.team-home.enabled", true)) {
            return;
        }

        String line1 = core.getMessage("teams.gui.placeholder-lore-1");
        String line2 = core.getMessage("teams.gui.placeholder-lore-2");
        player.sendMessage(line1);
        player.sendMessage(line2);
    }

    private void clearPlayerDeleteMeta(Player player) {
        player.removeMetadata(META_HOME_PENDING, core);
        player.removeMetadata(META_HOME_CONFIRM, core);
    }

    private void sendUpgradeMessage(Player player) {
        player.sendMessage(" ");
        player.sendMessage(core.getMessage("homes.upgrade-line-1"));
        player.sendMessage(" ");
        player.sendMessage(core.getMessage("homes.upgrade-line-2"));
        player.sendMessage(" ");
    }
}