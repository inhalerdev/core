package net.mineacle.core.baltop.listener;

import net.mineacle.core.Core;
import net.mineacle.core.baltop.gui.BalTopGui;
import net.mineacle.core.common.gui.MenuHistory;
import net.mineacle.core.economy.service.EconomyService;
import net.mineacle.core.stats.PlayerStatisticsGui;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BalTopGuiListener implements Listener {

    private final Core core;
    private final EconomyService economyService;
    private final PlayerStatisticsGui playerStatisticsGui;

    public BalTopGuiListener(Core core, EconomyService economyService) {
        this.core = core;
        this.economyService = economyService;
        this.playerStatisticsGui = new PlayerStatisticsGui();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!BalTopGui.isTitle(title)) {
            return;
        }

        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClick() == ClickType.DOUBLE_CLICK
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        }

        int rawSlot = event.getRawSlot();
        int topSize = event.getView().getTopInventory().getSize();

        if (rawSlot < 0 || rawSlot >= topSize) {
            return;
        }

        int page = BalTopGui.currentPage(player);

        if (rawSlot == BalTopGui.previousSlot()) {
            MenuHistory.openWithoutBackTrigger(
                    core,
                    player,
                    () -> BalTopGui.open(core, player, economyService, page - 1)
            );
            return;
        }

        if (rawSlot == BalTopGui.refreshSlot()) {
            MenuHistory.openWithoutBackTrigger(
                    core,
                    player,
                    () -> BalTopGui.open(core, player, economyService, page)
            );
            return;
        }

        if (rawSlot == BalTopGui.nextSlot()) {
            MenuHistory.openWithoutBackTrigger(
                    core,
                    player,
                    () -> BalTopGui.open(core, player, economyService, page + 1)
            );
            return;
        }

        if (!BalTopGui.isEntrySlot(rawSlot)) {
            return;
        }

        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) {
            return;
        }

        UUID targetId = targetIdAtSlot(page, rawSlot);

        if (targetId == null) {
            return;
        }

        MenuHistory.openChild(
                core,
                player,
                () -> BalTopGui.open(core, player, economyService, page),
                () -> playerStatisticsGui.open(player, targetId)
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        String title = ChatColor.stripColor(event.getView().getTitle());

        if (!BalTopGui.isTitle(title)) {
            return;
        }

        event.setCancelled(true);
        event.setResult(org.bukkit.event.Event.Result.DENY);
    }

    private UUID targetIdAtSlot(int page, int slot) {
        List<Map.Entry<UUID, Long>> entries = economyService.topBalances(Integer.MAX_VALUE);
        int index = (page * BalTopGui.entriesPerPage()) + slot;

        if (index < 0 || index >= entries.size()) {
            return null;
        }

        return entries.get(index).getKey();
    }
}