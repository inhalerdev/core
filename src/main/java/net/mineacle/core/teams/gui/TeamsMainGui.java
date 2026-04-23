package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public final class TeamsMainGui {

    public static final String NO_TEAM_TITLE = ChatColor.DARK_GRAY + "Team Menu";
    public static final String TEAM_TITLE = ChatColor.DARK_GRAY + "Your Team";

    private TeamsMainGui() {
    }

    public static void open(Core core, Player player, TeamService teamService, TeamInviteService inviteService) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            openNoTeamMenu(player, inviteService.hasInvite(player.getUniqueId()));
            return;
        }

        openTeamMenu(player, teamService, team);
    }

    private static void openNoTeamMenu(Player player, boolean hasInvite) {
        Inventory inventory = Bukkit.createInventory(null, 27, NO_TEAM_TITLE);

        inventory.setItem(11, item(
                Material.NAME_TAG,
                "&dCreate Team",
                List.of("&7Use &d/team create <name>")
        ));

        inventory.setItem(13, item(
                Material.BARRIER,
                "&7No Team",
                List.of("&fYou are not in a team")
        ));

        inventory.setItem(15, item(
                hasInvite ? Material.LIME_DYE : Material.GRAY_DYE,
                hasInvite ? "&aPending Invite" : "&7No Pending Invite",
                hasInvite ? List.of("&7Use &d/team accept", "&7or &d/team deny")
                        : List.of("&7No team invite waiting")
        ));

        player.openInventory(inventory);
    }

    private static void openTeamMenu(Player player, TeamService teamService, TeamRecord team) {
        Inventory inventory = Bukkit.createInventory(null, 54, TEAM_TITLE);

        inventory.setItem(45, item(
                Material.WHITE_BANNER,
                "&dTeam Home",
                List.of("&7Click to teleport")
        ));

        inventory.setItem(46, item(
                team.friendlyFire() ? Material.REDSTONE_TORCH : Material.LEVER,
                "&dFriendly Fire",
                List.of(team.friendlyFire() ? "&cEnabled" : "&aDisabled")
        ));

        inventory.setItem(47, item(
                Material.PLAYER_HEAD,
                "&dInvite Player",
                List.of("&7Use &d/team invite <player>")
        ));

        if (teamService.isFounder(player.getUniqueId())) {
            inventory.setItem(51, item(
                    Material.TNT,
                    "&cDisband Team",
                    List.of("&7Click to disband team")
            ));

            inventory.setItem(53, item(
                    Material.PURPLE_DYE,
                    "&cDelete Team Home",
                    List.of("&7Click to delete team home")
            ));
        } else {
            inventory.setItem(51, item(
                    Material.RED_STAINED_GLASS_PANE,
                    "&cLeave Team",
                    List.of("&7Click to leave team")
            ));
        }

        int slot = 0;
        for (UUID memberId : teamService.getTeamMembers(team.teamId())) {
            if (slot >= 45) {
                break;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            String role = teamService.getMember(memberId).role().name();

            inventory.setItem(slot, item(
                    Material.PLAYER_HEAD,
                    "&f" + (offlinePlayer.getName() == null ? memberId.toString() : offlinePlayer.getName()),
                    List.of("&7Role: &d" + prettyRole(role))
            ));

            slot++;
        }

        player.openInventory(inventory);
    }

    private static String prettyRole(String raw) {
        return switch (raw) {
            case "FOUNDER" -> "Founder";
            case "ADMIN" -> "Admin";
            default -> "Member";
        };
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