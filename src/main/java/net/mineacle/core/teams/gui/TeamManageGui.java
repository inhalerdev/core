package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class TeamManageGui {

    public static final String RAW_TITLE = "Team Manage";

    public static String TITLE(Core core) {
        return color(core.getMessage("teams.gui.manage-title"));
    }

    private TeamManageGui() {
    }

    public static void open(Core core, Player player, TeamService teamService) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 27, TITLE(core));

        inventory.setItem(10, item(
                team.bannerColor().bannerMaterial(),
                core.getMessage("teams.gui.manage-banner-title"),
                List.of(
                        core.getMessage("teams.gui.manage-banner-lore-1"),
                        "&7Current: &f" + team.bannerColor().displayName(),
                        "",
                        "&dClick to change"
                )
        ));

        inventory.setItem(12, item(
                Material.NAME_TAG,
                core.getMessage("teams.gui.manage-name-title"),
                List.of(
                        core.getMessage("teams.gui.manage-name-lore-1"),
                        "&7Current: " + team.nameColor() + team.nameColor(),
                        "",
                        "&dClick to change"
                )
        ));

        inventory.setItem(16, item(
                team.bannerColor().bannerMaterial(),
                core.getMessage("teams.gui.manage-preview-title"),
                List.of(
                        core.getMessage("teams.gui.manage-preview-lore-1").replace("%team%", teamService.formatTeamName(team)),
                        core.getMessage("teams.gui.manage-preview-lore-2").replace("%banner%", team.bannerColor().displayName()),
                        core.getMessage("teams.gui.manage-preview-lore-3").replace("%color%", ChatColor.translateAlternateColorCodes('&', team.nameColor()) + team.nameColor())
                )
        ));

        player.openInventory(inventory);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamManageGui::color).toList());

        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}