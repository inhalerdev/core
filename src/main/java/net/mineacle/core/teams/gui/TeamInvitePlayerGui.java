package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;

public final class TeamInvitePlayerGui {

    public static String TITLE(Core core) {
        return color(core.getMessage("teams.gui.invite-player-title"));
    }

    private TeamInvitePlayerGui() {
    }

    public static void open(Core core, Player viewer, TeamService teamService) {
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE(core));

        String search = TeamGuiSession.getInviteSearch(viewer.getUniqueId()).toLowerCase(Locale.ROOT);

        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(viewer.getUniqueId())) {
                continue;
            }

            if (teamService.hasTeam(online.getUniqueId())) {
                continue;
            }

            if (!search.isBlank() && !online.getName().toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }

            if (slot >= 45) {
                break;
            }

            inventory.setItem(slot, item(
                    Material.PLAYER_HEAD,
                    "&f" + online.getName(),
                    List.of(core.getMessage("teams.gui.invite-player-head-lore-1"))
            ));
            slot++;
        }

        inventory.setItem(45, item(
                Material.OAK_SIGN,
                core.getMessage("teams.gui.invite-search-title"),
                search.isBlank()
                        ? List.of(core.getMessage("teams.gui.invite-search-lore-1"))
                        : List.of(core.getMessage("teams.gui.invite-search-lore-current").replace("%search%", search))
        ));

        inventory.setItem(49, item(
                Material.ARROW,
                core.getMessage("teams.gui.back-menu-title"),
                List.of(core.getMessage("teams.gui.back-menu-lore-1"))
        ));

        inventory.setItem(53, item(
                Material.BARRIER,
                core.getMessage("teams.gui.clear-search-title"),
                List.of(core.getMessage("teams.gui.clear-search-lore-1"))
        ));

        viewer.openInventory(inventory);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamInvitePlayerGui::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}