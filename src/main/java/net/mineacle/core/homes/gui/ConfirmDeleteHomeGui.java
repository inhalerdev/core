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
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        inventory.setItem(11, item(
                Material.RED_STAINED_GLASS_PANE,
                "&cCancel",
                List.of("&7Do not continue")
        ));

        inventory.setItem(13, item(
                confirmActionMaterial(core),
                "&fDelete " + displayName,
                List.of("&7Confirm this action")
        ));

        inventory.setItem(15, item(
                Material.LIME_STAINED_GLASS_PANE,
                "&aConfirm",
                List.of("&7Click to confirm")
        ));

        player.openInventory(inventory);
    }

    public static void openTeamDelete(Core core, Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', core.getMessage("homes.gui.team-delete-title"));
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        inventory.setItem(11, item(
                Material.RED_STAINED_GLASS_PANE,
                "&cCancel",
                List.of("&7Do not continue")
        ));

        inventory.setItem(13, item(
                confirmActionMaterial(core),
                "&fDelete Team Home",
                List.of("&7Confirm this action")
        ));

        inventory.setItem(15, item(
                Material.LIME_STAINED_GLASS_PANE,
                "&aConfirm",
                List.of("&7Click to confirm")
        ));

        player.openInventory(inventory);
    }

    private static Material confirmActionMaterial(Core core) {
        String raw = core.getConfig().getString("gui.confirm-menu.action-material", "MAGENTA_DYE");

        try {
            Material material = Material.valueOf(raw.toUpperCase());
            return material.isItem() ? material : Material.MAGENTA_DYE;
        } catch (IllegalArgumentException exception) {
            return Material.MAGENTA_DYE;
        }
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .toList());

        item.setItemMeta(meta);
        return item;
    }
}