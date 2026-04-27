package net.mineacle.core.stats;

import net.mineacle.core.common.format.MoneyFormatter;
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

public final class PlayerStatisticsGui implements Listener {

    private static final int SIZE = 27;

    private static final int SLOT_MONEY = 10;
    private static final int SLOT_PLAYER_KILLS = 11;
    private static final int SLOT_DEATHS = 12;
    private static final int SLOT_PLAYTIME = 13;
    private static final int SLOT_BLOCKS_PLACED = 14;
    private static final int SLOT_BLOCKS_BROKEN = 15;
    private static final int SLOT_MOBS_KILLED = 16;

    public void open(Player viewer, UUID targetId) {
        String name = displayName(targetId);
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);

        Inventory inventory = Bukkit.createInventory(null, SIZE, name + " Stats");

        inventory.setItem(SLOT_MONEY, statItem(
                Material.EMERALD,
                "&dMONEY",
                "&7" + balance(target)
        ));

        inventory.setItem(SLOT_PLAYER_KILLS, statItem(
                Material.DIAMOND_SWORD,
                "&dKILLS",
                "&7" + compactStatistic(targetId, Statistic.PLAYER_KILLS)
        ));

        inventory.setItem(SLOT_DEATHS, statItem(
                Material.SKELETON_SKULL,
                "&dDEATHS",
                "&7" + compactStatistic(targetId, Statistic.DEATHS)
        ));

        inventory.setItem(SLOT_PLAYTIME, statItem(
                Material.CLOCK,
                "&dPLAYTIME",
                "&7" + playtime(targetId)
        ));

        inventory.setItem(SLOT_BLOCKS_PLACED, statItem(
                Material.GRASS_BLOCK,
                "&dBLOCKS PLACED",
                "&7" + compactStatistic(targetId, Statistic.USE_ITEM, Material.GRASS_BLOCK)
        ));

        inventory.setItem(SLOT_BLOCKS_BROKEN, statItem(
                Material.COBBLESTONE,
                "&dBLOCKS BROKEN",
                "&7" + compactStatistic(targetId, Statistic.MINE_BLOCK, Material.STONE)
        ));

        inventory.setItem(SLOT_MOBS_KILLED, statItem(
                Material.ZOMBIE_HEAD,
                "&dMOBS KILLED",
                "&7" + compactStatistic(targetId, Statistic.MOB_KILLS)
        ));

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

    private String compactStatistic(UUID targetId, Statistic statistic) {
        return MoneyFormatter.compact(rawStatistic(targetId, statistic));
    }

    private String compactStatistic(UUID targetId, Statistic statistic, Material material) {
        return MoneyFormatter.compact(rawStatistic(targetId, statistic, material));
    }

    private int rawStatistic(UUID targetId, Statistic statistic) {
        Player player = Bukkit.getPlayer(targetId);

        if (player == null) {
            return 0;
        }

        try {
            return player.getStatistic(statistic);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int rawStatistic(UUID targetId, Statistic statistic, Material material) {
        Player player = Bukkit.getPlayer(targetId);

        if (player == null) {
            return 0;
        }

        try {
            return player.getStatistic(statistic, material);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String playtime(UUID targetId) {
        Player player = Bukkit.getPlayer(targetId);

        if (player == null) {
            return "0m";
        }

        int ticks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long totalSeconds = ticks / 20L;

        long days = totalSeconds / 86400L;
        long hours = (totalSeconds % 86400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;

        if (days > 0) {
            return days + "d " + hours + "h";
        }

        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }

        return minutes + "m";
    }

    private String balance(OfflinePlayer player) {
        if (player == null) {
            return "$0";
        }

        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object economy = Bukkit.getServicesManager().load(economyClass);

            if (economy == null) {
                return "$0";
            }

            Method getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            Object result = getBalance.invoke(economy, player);

            if (!(result instanceof Number number)) {
                return "$0";
            }

            return MoneyFormatter.money(number.doubleValue());
        } catch (Throwable ignored) {
            return "$0";
        }
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