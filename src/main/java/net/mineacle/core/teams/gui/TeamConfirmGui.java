package net.mineacle.core.teams.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class TeamConfirmGui {

    public static final String TITLE = ChatColor.DARK_GRAY + "Confirm Action";
    public static final String DELETE_HOME_TITLE = TITLE;

    private TeamConfirmGui() {
    }

    public static void open(Player player, String actionName) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        inventory.setItem(11, item(
                Material.RED_STAINED_GLASS_PANE,
                "&cCancel",
                List.of("&7Do not continue")
        ));

        inventory.setItem(13, item(
                Material.PURPLE_STAINED_GLASS_PANE,
                "&f" + actionName,
                List.of("&7Confirm this action")
        ));

        inventory.setItem(15, item(
                Material.LIME_STAINED_GLASS_PANE,
                "&aConfirm",
                List.of("&7Click to confirm")
        ));

        player.openInventory(inventory);
    }

    public static void openDeleteHome(Player player) {
        open(player, "Delete Team Home");
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamConfirmGui::color).toList());
        item.setItemMeta(meta);

        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}