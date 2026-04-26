package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamInviteRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class TeamInviteGui {

    public static final String TITLE = ChatColor.DARK_GRAY + "Team Invite";

    private TeamInviteGui() {
    }

    public static void open(Core core, Player player, TeamInviteService inviteService, TeamService teamService) {
        TeamInviteRecord invite = inviteService.getInvite(player.getUniqueId());

        if (invite == null) {
            player.sendMessage("§cYou have no current team invites.");
            return;
        }

        TeamRecord team = teamService.getTeamById(invite.teamId());

        if (team == null) {
            player.sendMessage("§cThat team no longer exists.");
            inviteService.denyInvite(player.getUniqueId());
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        inventory.setItem(11, item(
                Material.LIME_CONCRETE,
                "&aAccept Invite",
                List.of("&7Join &d" + team.name())
        ));

        inventory.setItem(13, item(
                Material.PURPLE_BANNER,
                "&d" + team.name(),
                List.of("&7Team invite")
        ));

        inventory.setItem(15, item(
                Material.RED_CONCRETE,
                "&cDecline Invite",
                List.of("&7Decline this invite")
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
        meta.setLore(lore.stream().map(TeamInviteGui::color).toList());

        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}