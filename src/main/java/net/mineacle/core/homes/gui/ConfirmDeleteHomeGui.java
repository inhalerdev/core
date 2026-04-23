package net.mineacle.core.homes.gui;

import net.mineacle.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ConfirmDeleteHomeGui {

    private ConfirmDeleteHomeGui() {
    }

    public static void openPlayerDelete(Core core, Player player, int id, String displayName) {
        String title = ChatColor.translateAlternateColorCodes('&', core.getMessage("homes.gui.delete-title"));
        Inventory inventory = Bukkit.createInventory(null, 9 * 3, title);

        inventory.setItem(
                11,
                item(Material.RED_STAINED_GLASS_PANE, "&cCancel", List.of("&fClick to cancel deletion"))
        );

        inventory.setItem(
                13,
                item(Material.PURPLE_DYE, "&d" + displayName, List.of("&7This will delete this home"))
        );

        inventory.setItem(
                15,
                item(Material.LIME_STAINED_GLASS_PANE, "&aConfirm", List.of("&fClick twice to confirm deletion"))
        );

        player.openInventory(inventory);
    }

    public static void openTeamDelete(Core core, Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', core.getMessage("homes.gui.team-delete-title"));
        Inventory inventory = Bukkit.createInventory(null, 9 * 3, title);

        inventory.setItem(
                11,
                item(Material.RED_STAINED_GLASS_PANE, "&cCancel", List.of("&fClick to cancel deletion"))
        );

        inventory.setItem(
                13,
                item(Material.PURPLE_BANNER, "&dTeam Home", List.of("&7This will delete your team home"))
        );

        inventory.setItem(
                15,
                item(Material.LIME_STAINED_GLASS_PANE, "&aConfirm", List.of("&fClick twice to confirm deletion"))
        );

        player.openInventory(inventory);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());

        item.setItemMeta(meta);
        return item;
    }
}