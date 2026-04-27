package net.mineacle.core.baltop.gui;

import net.mineacle.core.Core;
import net.mineacle.core.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BalTopGui {

    public static final String META_PAGE = "mineacle_baltop_page";
    public static final String TITLE_PREFIX = "Balance Top (Page ";

    private static final int SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 45;

    private static final int SLOT_PREVIOUS = 45;
    private static final int SLOT_REFRESH = 49;
    private static final int SLOT_NEXT = 53;

    private BalTopGui() {
    }

    public static void open(Core core, Player player, EconomyService economyService, int page) {
        List<Map.Entry<UUID, Long>> entries = economyService.topBalances(Integer.MAX_VALUE);

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) ENTRIES_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inventory = Bukkit.createInventory(
                null,
                SIZE,
                "Balance Top (Page " + (safePage + 1) + ")"
        );

        player.setMetadata(META_PAGE, new FixedMetadataValue(core, safePage));

        int start = safePage * ENTRIES_PER_PAGE;
        int end = Math.min(entries.size(), start + ENTRIES_PER_PAGE);

        for (int index = start; index < end; index++) {
            Map.Entry<UUID, Long> entry = entries.get(index);

            int slot = index - start;
            int placement = index + 1;

            OfflinePlayer target = Bukkit.getOfflinePlayer(entry.getKey());
            String name = target.getName() == null ? entry.getKey().toString() : target.getName();
            String balance = economyService.format(entry.getValue());

            inventory.setItem(slot, playerEntry(target, name, balance, placement));
        }

        if (safePage > 0) {
            inventory.setItem(SLOT_PREVIOUS, item(
                    Material.ARROW,
                    "&dPrevious Page",
                    List.of("&7Click to go to the previous page.")
            ));
        }

        inventory.setItem(SLOT_REFRESH, item(
                Material.EMERALD,
                "&dRefresh",
                List.of("&7Click to refresh Balance Top.")
        ));

        if (safePage < totalPages - 1) {
            inventory.setItem(SLOT_NEXT, item(
                    Material.ARROW,
                    "&dNext Page",
                    List.of("&7Click to go to the next page.")
            ));
        }

        player.openInventory(inventory);
    }

    public static boolean isTitle(String strippedTitle) {
        return strippedTitle != null && strippedTitle.startsWith(TITLE_PREFIX);
    }

    public static int currentPage(Player player) {
        if (!player.hasMetadata(META_PAGE)) {
            return 0;
        }

        return player.getMetadata(META_PAGE).get(0).asInt();
    }

    public static int entriesPerPage() {
        return ENTRIES_PER_PAGE;
    }

    public static boolean isEntrySlot(int slot) {
        return slot >= 0 && slot < ENTRIES_PER_PAGE;
    }

    public static int previousSlot() {
        return SLOT_PREVIOUS;
    }

    public static int refreshSlot() {
        return SLOT_REFRESH;
    }

    public static int nextSlot() {
        return SLOT_NEXT;
    }

    private static ItemStack playerEntry(OfflinePlayer owner, String name, String balance, int placement) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(owner);
        meta.setDisplayName(color("&d" + name));
        meta.setLore(List.of(
                color("&fBalance: &7" + balance + " &d(#" + placement + ")")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));

        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(color(line));
        }

        meta.setLore(coloredLore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}