package net.mineacle.core.baltop.listener;

import net.mineacle.core.Core;
import net.mineacle.core.baltop.gui.BalTopGui;
import net.mineacle.core.common.gui.MenuHistory;
import net.mineacle.core.economy.service.EconomyService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class BalTopGuiListener implements Listener {

    private final Core core;
    private final EconomyService economyService;

    public BalTopGuiListener(Core core, EconomyService economyService) {
        this.core = core;
        this.economyService = economyService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        if (slot < 0 || slot >= topSize) {
            return;
        }

        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!BalTopGui.isTitle(core, title)) {
            return;
        }

        event.setCancelled(true);

        int page = BalTopGui.currentPage(player);

        if (slot == 45) {
            MenuHistory.openRoot(core, player, () -> BalTopGui.open(core, player, economyService, page - 1));
            return;
        }

        if (slot == 49) {
            MenuHistory.openRoot(core, player, () -> BalTopGui.open(core, player, economyService, page));
            return;
        }

        if (slot == 53) {
            MenuHistory.openRoot(core, player, () -> BalTopGui.open(core, player, economyService, page + 1));
        }
    }
}