package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.common.text.TextColor;
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

    public static void open(Core core, Player player, String actionName) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        inventory.setItem(11, item(
                Material.RED_STAINED_GLASS_PANE,
                "&cCancel",
                List.of("&#ccccccDo not continue")
        ));

        inventory.setItem(13, item(
                confirmActionMaterial(core),
                "&d" + actionName,
                List.of(
                        "&#ccccccClick once to arm this action.",
                        "&#ccccccClick confirm again to continue."
                )
        ));

        inventory.setItem(15, item(
                Material.LIME_STAINED_GLASS_PANE,
                "&aConfirm",
                List.of("&#ccccccDouble-click confirm to continue")
        ));

        player.openInventory(inventory);
    }

    public static void openDeleteHome(Core core, Player player) {
        open(core, player, "Delete Team Home");
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

        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamConfirmGui::color).toList());

        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return TextColor.color(input);
    }
}