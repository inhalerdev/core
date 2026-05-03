package net.mineacle.core.spawn.gui;

import net.mineacle.core.common.text.TextColor;
import net.mineacle.core.spawn.model.SpawnPoint;
import net.mineacle.core.spawn.service.SpawnService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class SpawnGui {

    private SpawnGui() {
    }

    public static void open(Player player, SpawnService spawnService) {
        Inventory inventory = Bukkit.createInventory(null, spawnService.size(), spawnService.title());

        for (SpawnPoint point : spawnService.spawnPoints()) {
            if (!point.enabled()) {
                continue;
            }

            boolean current = spawnService.isCurrentWorld(player, point);
            int online = spawnService.onlineInWorld(point);

            inventory.setItem(point.slot(), spawnItem(
                    current,
                    point.displayName(),
                    online,
                    spawnService.maxPlayersDisplay()
            ));
        }

        if (spawnService.randomEnabled()) {
            inventory.setItem(spawnService.randomSlot(), item(
                    Material.NETHER_STAR,
                    spawnService.randomDisplayName(),
                    List.of(
                            "&7Click to teleport to a random &#b777f5Spawn",
                            "",
                            "&7Chooses the lowest-populated spawn"
                    )
            ));
        }

        player.openInventory(inventory);
    }

    private static ItemStack spawnItem(boolean current, String displayName, int online, int maxPlayers) {
        if (current) {
            return item(
                    Material.GLOW_ITEM_FRAME,
                    displayName,
                    List.of(
                            "&7" + online + "&8/&7" + maxPlayers,
                            "",
                            "&a➥ You are currently here"
                    )
            );
        }

        return item(
                Material.ITEM_FRAME,
                displayName,
                List.of(
                        "&7" + online + "&8/&7" + maxPlayers,
                        "",
                        "&e➥ Return to Spawn"
                )
        );
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(TextColor.color(name));
        meta.setLore(lore.stream().map(TextColor::color).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        item.setItemMeta(meta);
        return item;
    }
}