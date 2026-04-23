package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamBanRecord;
import net.mineacle.core.teams.service.TeamBanService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TeamBansGui {

    public static String TITLE(Core core) {
        return color(core.getMessage("teams.gui.bans-title"));
    }

    private TeamBansGui() {
    }

    public static void open(Core core, Player viewer, String teamId, TeamBanService banService, TeamService teamService) {
        Inventory inventory = Bukkit.createInventory(null, 54, TITLE(core));

        List<TeamBanRecord> bans = banService.getActiveBans(teamId);

        if (bans.isEmpty()) {
            inventory.setItem(22, item(
                    Material.BARRIER,
                    core.getMessage("teams.gui.no-ban-title"),
                    List.of(core.getMessage("teams.gui.no-ban-lore-1"))
            ));
            inventory.setItem(49, item(
                    Material.ARROW,
                    core.getMessage("teams.gui.back-menu-title"),
                    List.of(core.getMessage("teams.gui.back-menu-lore-1"))
            ));
            viewer.openInventory(inventory);
            return;
        }

        int slot = 0;
        for (TeamBanRecord ban : bans) {
            if (slot >= 45) {
                break;
            }

            UUID bannedId = ban.playerId();
            OfflinePlayer banned = Bukkit.getOfflinePlayer(bannedId);
            OfflinePlayer bannedBy = Bukkit.getOfflinePlayer(ban.bannedBy());

            String bannedName = banned.getName() == null ? bannedId.toString() : banned.getName();
            String bannedByName = bannedBy.getName() == null ? ban.bannedBy().toString() : bannedBy.getName();
            String expires = formatTime(ban.expiresAt());

            inventory.setItem(slot, item(
                    Material.PLAYER_HEAD,
                    "&f" + bannedName,
                    List.of(
                            core.getMessage("teams.gui.ban-head-lore-1").replace("%expires%", expires),
                            core.getMessage("teams.gui.ban-head-lore-2").replace("%banned_by%", bannedByName),
                            core.getMessage("teams.gui.ban-head-lore-3")
                    )
            ));
            slot++;
        }

        inventory.setItem(49, item(
                Material.ARROW,
                core.getMessage("teams.gui.back-menu-title"),
                List.of(core.getMessage("teams.gui.back-menu-lore-1"))
        ));

        viewer.openInventory(inventory);
    }

    private static String formatTime(long epochMillis) {
        return new SimpleDateFormat("MM/dd HH:mm", Locale.US).format(new Date(epochMillis));
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamBansGui::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}