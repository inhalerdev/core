package net.mineacle.core.common.gui;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.Plugin;

public final class MenuCloseListener implements Listener {

    private final Plugin plugin;

    public MenuCloseListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        String title = ChatColor.stripColor(event.getView().getTitle());

        if (title == null || !isMineacleMenu(title)) {
            return;
        }

        MenuHistory.handleClose(plugin, player);
    }

    private boolean isMineacleMenu(String title) {
        return title.equalsIgnoreCase("Homes")
                || title.equalsIgnoreCase("Delete Home")
                || title.equalsIgnoreCase("Delete Team Home")
                || title.startsWith("Team:")
                || title.startsWith("Member:")
                || title.equalsIgnoreCase("Team Invite")
                || title.equalsIgnoreCase("Confirm Action")
                || title.equalsIgnoreCase("Teleport Request")
                || title.startsWith("MOST MONEY (Page ")
                || title.endsWith(" Stats");
    }
}