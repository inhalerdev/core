package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamNameColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class TeamNameColorGui {

    public static String TITLE(Core core) {
        return color(core.getMessage("teams.gui.name-color-title"));
    }

    public static final TeamNameColor[] COLORS = {
            TeamNameColor.WHITE,
            TeamNameColor.RED,
            TeamNameColor.GOLD,
            TeamNameColor.YELLOW,
            TeamNameColor.GREEN,
            TeamNameColor.AQUA,
            TeamNameColor.BLUE,
            TeamNameColor.LIGHT_PURPLE,
            TeamNameColor.DARK_PURPLE
    };

    private TeamNameColorGui() {
    }

    public static void open(Core core, Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE(core));

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 24};

        for (int i = 0; i < COLORS.length; i++) {
            TeamNameColor nameColor = COLORS[i];
            inventory.setItem(slots[i], item(
                    nameColor.material(),
                    nameColor.colorCode() + nameColor.displayName(),
                    "&7Click to set your team's name color."
            ));
        }

        player.openInventory(inventory);
    }

    public static TeamNameColor fromSlot(int slot) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 20, 24};

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                return COLORS[i];
            }
        }

        return null;
    }

    private static ItemStack item(org.bukkit.Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));
        meta.setLore(java.util.List.of(color(lore)));

        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}