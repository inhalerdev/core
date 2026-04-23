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

    public static final String LEAVE_TITLE = ChatColor.DARK_GRAY + "Leave Team";
    public static final String DISBAND_TITLE = ChatColor.DARK_GRAY + "Disband Team";
    public static final String DELETE_HOME_TITLE = ChatColor.DARK_GRAY + "Delete Team Home";
    public static final String KICK_TITLE = ChatColor.DARK_GRAY + "Kick Member";
    public static final String TRANSFER_TITLE = ChatColor.DARK_GRAY + "Transfer Founder";

    private TeamConfirmGui() {
    }

    public static void openLeave(Player player, String teamName) {
        Inventory inventory = Bukkit.createInventory(null, 27, LEAVE_TITLE);

        inventory.setItem(11, item(Material.RED_STAINED_GLASS_PANE, "&cCancel", List.of("&fClick to cancel")));
        inventory.setItem(13, item(Material.PLAYER_HEAD, "&d" + teamName, List.of("&7You are about to leave this team")));
        inventory.setItem(15, item(Material.LIME_STAINED_GLASS_PANE, "&aConfirm", List.of("&fClick twice to confirm")));

        player.openInventory(inventory);
    }

    public static void openDisband(Player player, String teamName) {
        Inventory inventory = Bukkit.createInventory(null, 27, DISBAND_TITLE);

        inventory.setItem(11, item(Material.RED_STAINED_GLASS_PANE, "&cCancel", List.of("&fClick to cancel")));
        inventory.setItem(13, item(Material.TNT, "&c" + teamName, List.of("&7This will disband your team")));
        inventory.setItem(15, item(Material.LIME_STAINED_GLASS_PANE, "&aConfirm", List.of("&fClick twice to confirm")));

        player.openInventory(inventory);
    }

    public static void openDeleteHome(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, DELETE_HOME_TITLE);

        inventory.setItem(11, item(Material.RED_STAINED_GLASS_PANE, "&cCancel", List.of("&fClick to cancel")));
        inventory.setItem(13, item(Material.PURPLE_BANNER, "&dTeam Home", List.of("&7This will delete your team home")));
        inventory.setItem(15, item(Material.LIME_STAINED_GLASS_PANE, "&aConfirm", List.of("&fClick twice to confirm")));

        player.openInventory(inventory);
    }

    public static void openKick(Player player, String memberName) {
        Inventory inventory = Bukkit.createInventory(null, 27, KICK_TITLE);

        inventory.setItem(11, item(Material.RED_STAINED_GLASS_PANE, "&cCancel", List.of("&fClick to cancel")));
        inventory.setItem(13, item(Material.BARRIER, "&c" + memberName, List.of("&7This will remove the member")));
        inventory.setItem(15, item(Material.LIME_STAINED_GLASS_PANE, "&aConfirm", List.of("&fClick twice to confirm")));

        player.openInventory(inventory);
    }

    public static void openTransfer(Player player, String memberName) {
        Inventory inventory = Bukkit.createInventory(null, 27, TRANSFER_TITLE);

        inventory.setItem(11, item(Material.RED_STAINED_GLASS_PANE, "&cCancel", List.of("&fClick to cancel")));
        inventory.setItem(13, item(Material.GOLDEN_HELMET, "&6" + memberName, List.of("&7This member will become founder")));
        inventory.setItem(15, item(Material.LIME_STAINED_GLASS_PANE, "&aConfirm", List.of("&fClick twice to confirm")));

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