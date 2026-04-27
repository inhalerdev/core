package net.mineacle.core.stats;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public final class PlayerStatisticsGui implements Listener {

    public void open(Player viewer, UUID targetId) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String name = displayName(targetId);

        Inventory inventory = Bukkit.createInventory(null, 27, name + " Stats");

        inventory.setItem(4, playerHead(target, "&d" + name, List.of("&7Viewing player stats.")));

        inventory.setItem(10, statItem(Material.EMERALD, "&dMoney", "&7" + VaultMoneyHook.formattedBalance(target)));
        inventory.setItem(11, statItem(Material.DIAMOND_SWORD, "&dPlayer Kills", "&7" + statistic(targetId, Statistic.PLAYER_KILLS)));
        inventory.setItem(12, statItem(Material.SKELETON_SKULL, "&dDeaths", "&7" + statistic(targetId, Statistic.DEATHS)));
        inventory.setItem(13, statItem(Material.CLOCK, "&dPlaytime", "&7" + playtime(targetId)));
        inventory.setItem(14, statItem(Material.GRASS_BLOCK, "&dBlocks Placed", "&7" + statistic(targetId, Statistic.USE_ITEM, Material.GRASS_BLOCK)));
        inventory.setItem(15, statItem(Material.COBBLESTONE, "&dBlocks Broken", "&7" + statistic(targetId, Statistic.MINE_BLOCK, Material.STONE)));
        inventory.setItem(16, statItem(Material.ZOMBIE_HEAD, "&dMobs Killed", "&7" + statistic(targetId, Statistic.MOB_KILLS)));

        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();

        if (!(clicker instanceof Player)) {
            return;
        }

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title != null && title.endsWith(" Stats")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title != null && title.endsWith(" Stats")) {
            event.setCancelled(true);
        }
    }

    private ItemStack playerHead(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(owner);
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(this::color).toList());
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack statItem(Material material, String name, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));
        meta.setLore(List.of(color(value)));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    private String statistic(UUID targetId, Statistic statistic) {
        Player player = Bukkit.getPlayer(targetId);

        if (player == null) {
            return "0";
        }

        try {
            return String.valueOf(player.getStatistic(statistic));
        } catch (Exception ignored) {
            return "0";
        }
    }

    private String statistic(UUID targetId, Statistic statistic, Material material) {
        Player player = Bukkit.getPlayer(targetId);

        if (player == null) {
            return "0";
        }

        try {
            return String.valueOf(player.getStatistic(statistic, material));
        } catch (Exception ignored) {
            return "0";
        }
    }

    private String playtime(UUID targetId) {
        Player player = Bukkit.getPlayer(targetId);

        if (player == null) {
            return "0m";
        }

        int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long totalSeconds = ticks / 20L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }

    private String displayName(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);

        if (online != null && online.getDisplayName() != null) {
            String stripped = ChatColor.stripColor(online.getDisplayName());
            if (stripped != null && !stripped.isBlank()) {
                return stripped;
            }
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        return offlinePlayer.getName() == null ? playerId.toString() : offlinePlayer.getName();
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}