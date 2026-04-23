package net.mineacle.core.teams.gui;

import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamRole;
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

public final class TeamMemberManageGui {

    public static final String TITLE = ChatColor.DARK_GRAY + "Member Manager";

    private TeamMemberManageGui() {
    }

    public static void open(Player viewer, UUID targetId, TeamService teamService) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE);

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        TeamMemberRecord member = teamService.getMember(targetId);

        String targetName = target.getName() == null ? targetId.toString() : target.getName();
        String roleName = member == null ? "Unknown" : prettyRole(member.role());

        inventory.setItem(4, item(
                Material.PLAYER_HEAD,
                "&f" + targetName,
                List.of("&7Role: &d" + roleName)
        ));

        inventory.setItem(10, item(
                Material.LIME_DYE,
                "&aPromote",
                List.of("&7Promote this member")
        ));

        inventory.setItem(12, item(
                Material.ORANGE_DYE,
                "&6Demote",
                List.of("&7Demote this member")
        ));

        inventory.setItem(14, item(
                Material.BARRIER,
                "&cKick",
                List.of("&7Remove from team")
        ));

        inventory.setItem(16, item(
                Material.GOLDEN_HELMET,
                "&6Transfer Founder",
                List.of("&7Make this player founder")
        ));

        inventory.setItem(22, item(
                Material.ARROW,
                "&dBack",
                List.of("&7Return to team menu")
        ));

        viewer.openInventory(inventory);
    }

    private static String prettyRole(TeamRole role) {
        return switch (role) {
            case FOUNDER -> "Founder";
            case ADMIN -> "Admin";
            case MEMBER -> "Member";
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