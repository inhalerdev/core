package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamBannerColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class TeamBannerColorGui {

    public static String TITLE(Core core) {
        return color(core.getMessage("teams.gui.banner-color-title"));
    }

    public static final TeamBannerColor[] COLORS = {
            TeamBannerColor.WHITE,
            TeamBannerColor.GRAY,
            TeamBannerColor.LIGHT_GRAY,
            TeamBannerColor.RED,
            TeamBannerColor.ORANGE,
            TeamBannerColor.YELLOW,
            TeamBannerColor.LIME,
            TeamBannerColor.GREEN,
            TeamBannerColor.CYAN,
            TeamBannerColor.LIGHT_BLUE,
            TeamBannerColor.BLUE,
            TeamBannerColor.PURPLE,
            TeamBannerColor.MAGENTA,
            TeamBannerColor.PINK
    };

    private TeamBannerColorGui() {
    }

    public static void open(Core core, Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE(core));

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

        for (int i = 0; i < COLORS.length; i++) {
            TeamBannerColor bannerColor = COLORS[i];
            inventory.setItem(slots[i], item(
                    bannerColor.bannerMaterial(),
                    "&f" + bannerColor.displayName(),
                    "&7Click to set your team's banner color."
            ));
        }

        player.openInventory(inventory);
    }

    public static TeamBannerColor fromSlot(int slot) {
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

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