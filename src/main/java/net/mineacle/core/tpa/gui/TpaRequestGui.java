package net.mineacle.core.tpa.gui;

import net.mineacle.core.Core;
import net.mineacle.core.tpa.service.TpaRequest;
import net.mineacle.core.tpa.service.TpaRequestType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public final class TpaRequestGui {

    public static final String TITLE = ChatColor.DARK_GRAY + "Teleport Request";

    private TpaRequestGui() {
    }

    public static void open(Core core, Player viewer, TpaRequest request) {
        if (request == null) {
            viewer.sendMessage("§cYou have no pending teleport requests.");
            return;
        }

        OfflinePlayer requester = Bukkit.getOfflinePlayer(request.requesterId());
        String requesterName = requester.getName() == null ? request.requesterId().toString() : requester.getName();

        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        inventory.setItem(11, item(
                Material.RED_STAINED_GLASS_PANE,
                "&cDeny",
                List.of("&7Deny this teleport request.")
        ));

        inventory.setItem(13, playerHead(
                requester,
                "&d" + requesterName,
                request.type() == TpaRequestType.TO_TARGET
                        ? List.of("&7Wants to teleport to you.")
                        : List.of("&7Wants you to teleport to them.")
        ));

        inventory.setItem(15, item(
                Material.LIME_STAINED_GLASS_PANE,
                "&aAccept",
                List.of("&7Accept this teleport request.")
        ));

        viewer.openInventory(inventory);
    }

    private static ItemStack playerHead(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(owner);
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TpaRequestGui::color).toList());

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
        meta.setLore(lore.stream().map(TpaRequestGui::color).toList());
        item.setItemMeta(meta);

        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}