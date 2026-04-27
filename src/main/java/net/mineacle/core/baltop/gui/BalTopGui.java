package net.mineacle.core.baltop.gui;

import net.mineacle.core.Core;
import net.mineacle.core.economy.service.EconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BalTopGui {

    public static final String META_PAGE = "mineacle_baltop_page";

    private static final int SIZE = 54;
    private static final int ENTRIES_PER_PAGE = 45;

    private BalTopGui() {
    }

    public static void open(Core core, Player player, EconomyService economyService, int page) {
        List<Map.Entry<UUID, Long>> entries = economyService.topBalances(Integer.MAX_VALUE);

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) ENTRIES_PER_PAGE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        String baseTitle = title(core);
        Inventory inventory = Bukkit.createInventory(
                null,
                SIZE,
                ChatColor.translateAlternateColorCodes('&', baseTitle + " &8(" + (safePage + 1) + "/" + totalPages + ")")
        );

        player.setMetadata(META_PAGE, new FixedMetadataValue(core, safePage));

        int start = safePage * ENTRIES_PER_PAGE;
        int end = Math.min(entries.size(), start + ENTRIES_PER_PAGE);

        if (entries.isEmpty()) {
            inventory.setItem(22, item(
                    Material.BARRIER,
                    core.getMessage("baltop.gui-empty-title"),
                    List.of(core.getMessage("baltop.gui-empty-lore"))
            ));
        } else {
            for (int i = start; i < end; i++) {
                Map.Entry<UUID, Long> entry = entries.get(i);
                int slot = i - start;
                int position = i + 1;

                OfflinePlayer target = Bukkit.getOfflinePlayer(entry.getKey());
                String name = target.getName() == null ? entry.getKey().toString() : target.getName();
                String balance = economyService.format(entry.getValue());

                inventory.setItem(slot, playerHead(
                        target,
                        core.getMessage("baltop.gui-entry-title")
                                .replace("%position%", String.valueOf(position))
                                .replace("%player%", name)
                                .replace("%balance%", balance),
                        core.getMessagesConfig().getStringList("baltop.gui-entry-lore").stream()
                                .map(line -> core.getMessageText(line)
                                        .replace("%position%", String.valueOf(position))
                                        .replace("%player%", name)
                                        .replace("%balance%", balance))
                                .toList()
                ));
            }
        }

        if (safePage > 0) {
            inventory.setItem(45, item(
                    Material.ARROW,
                    core.getMessage("baltop.previous-title"),
                    List.of(core.getMessage("baltop.previous-lore"))
            ));
        }

        inventory.setItem(49, item(
                Material.NETHER_STAR,
                core.getMessage("baltop.refresh-title"),
                List.of(core.getMessage("baltop.refresh-lore"))
        ));

        if (safePage < totalPages - 1) {
            inventory.setItem(53, item(
                    Material.ARROW,
                    core.getMessage("baltop.next-title"),
                    List.of(core.getMessage("baltop.next-lore"))
            ));
        }

        player.openInventory(inventory);
    }

    public static boolean isTitle(Core core, String strippedTitle) {
        if (strippedTitle == null) {
            return false;
        }

        String baseTitle = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', title(core)));
        return strippedTitle.startsWith(baseTitle);
    }

    public static int currentPage(Player player) {
        if (!player.hasMetadata(META_PAGE)) {
            return 0;
        }

        return player.getMetadata(META_PAGE).get(0).asInt();
    }

    private static String title(Core core) {
        return core.getConfig().getString("baltop.gui-title", "&dBalance Top");
    }

    private static ItemStack playerHead(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(owner);
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(BalTopGui::color).toList());
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
        meta.setLore(lore.stream().map(BalTopGui::color).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}