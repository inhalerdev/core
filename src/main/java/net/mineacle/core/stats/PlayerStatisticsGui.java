package net.mineacle.core.stats;

import net.mineacle.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.Sound;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerStatisticsGui implements Listener {

    private static final int SIZE = 27;

    private static final int SLOT_HEAD = 4;
    private static final int SLOT_KILLS = 10;
    private static final int SLOT_DEATHS = 11;
    private static final int SLOT_PLAYTIME = 12;
    private static final int SLOT_BLOCKS_PLACED = 14;
    private static final int SLOT_BLOCKS_BROKEN = 15;
    private static final int SLOT_MOBS_KILLED = 16;

    private final Core core;
    private final Map<UUID, UUID> openTargets = new ConcurrentHashMap<>();

    public PlayerStatisticsGui(Core core) {
        this.core = core;
    }

    public void open(Player viewer, UUID targetId) {
        String displayName = visibleName(targetId);
        Inventory inventory = Bukkit.createInventory(null, SIZE, stripColor(displayName) + " Statistics");

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);

        inventory.setItem(SLOT_HEAD, playerHead(target, "&d" + displayName, "&7Viewing player statistics."));
        inventory.setItem(SLOT_KILLS, statItem(Material.NETHERITE_SWORD, "&fKills", statisticValue(targetId, Statistic.PLAYER_KILLS)));
        inventory.setItem(SLOT_DEATHS, statItem(Material.SKELETON_SKULL, "&fDeaths", statisticValue(targetId, Statistic.DEATHS)));
        inventory.setItem(SLOT_PLAYTIME, statItem(Material.CLOCK, "&fPlaytime", playtimeValue(targetId)));
        inventory.setItem(SLOT_BLOCKS_PLACED, statItem(Material.GRASS_BLOCK, "&fBlocks Placed", statisticValue(targetId, Statistic.USE_ITEM, Material.GRASS_BLOCK)));
        inventory.setItem(SLOT_BLOCKS_BROKEN, statItem(Material.COBBLESTONE, "&fBlocks Broken", statisticValue(targetId, Statistic.MINE_BLOCK, Material.STONE)));
        inventory.setItem(SLOT_MOBS_KILLED, statItem(Material.ZOMBIE_HEAD, "&fMobs Killed", statisticValue(targetId, Statistic.MOB_KILLS)));

        openTargets.put(viewer.getUniqueId(), targetId);
        viewer.openInventory(inventory);
        viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) {
            return;
        }

        if (!event.getView().getTitle().endsWith(" Statistics")) {
            return;
        }

        if (!openTargets.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) {
            return;
        }

        if (event.getView().getTitle().endsWith(" Statistics") && openTargets.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private ItemStack playerHead(OfflinePlayer owner, String name, String lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(owner);
        meta.setDisplayName(color(name));
        meta.setLore(java.util.List.of(color(lore)));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack statItem(Material material, String name, String value) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(java.util.List.of(color("&7Value: &f" + value)));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            stack.setItemMeta(meta);
        }

        return stack;
    }

    private String statisticValue(UUID targetId, Statistic statistic) {
        Player online = Bukkit.getPlayer(targetId);
        if (online == null) {
            return "0";
        }

        try {
            return String.valueOf(online.getStatistic(statistic));
        } catch (Exception ignored) {
            return "0";
        }
    }

    private String statisticValue(UUID targetId, Statistic statistic, Material material) {
        Player online = Bukkit.getPlayer(targetId);
        if (online == null) {
            return "0";
        }

        try {
            return String.valueOf(online.getStatistic(statistic, material));
        } catch (Exception ignored) {
            return "0";
        }
    }

    private String playtimeValue(UUID targetId) {
        Player online = Bukkit.getPlayer(targetId);
        if (online == null) {
            return "0m";
        }

        int ticks = online.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long totalSeconds = ticks / 20L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }

    private String visibleName(UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null && online.getDisplayName() != null && !ChatColor.stripColor(online.getDisplayName()).isBlank()) {
            return ChatColor.stripColor(online.getDisplayName());
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : playerId.toString();
    }

    private String stripColor(String input) {
        String stripped = ChatColor.stripColor(input);
        return stripped == null ? "" : stripped;
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}