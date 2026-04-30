package net.mineacle.core.teams.gui;

import net.mineacle.core.common.player.DisplayNames;
import net.mineacle.core.common.text.TextColor;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public final class TeamMemberGui {

    public static final String TITLE_PREFIX = "Member: ";

    private TeamMemberGui() {
    }

    public static void open(Player viewer, UUID targetId, TeamService teamService) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        TeamMemberRecord member = teamService.getMember(targetId);

        String displayName = DisplayNames.prefixedDisplayName(target);
        String role = member == null ? "Unknown" : member.role().displayName();

        Inventory inventory = Bukkit.createInventory(
                null,
                27,
                ChatColor.DARK_GRAY + TITLE_PREFIX + TextColor.strip(displayName)
        );

        inventory.setItem(4, playerHead(
                target,
                displayName,
                List.of(
                        "&#bbbbbbRank: &d" + role,
                        "&#bbbbbbManage this team member."
                )
        ));

        inventory.setItem(10, item(
                Material.LIME_DYE,
                "&aPromote",
                List.of(
                        "&#bbbbbbPromote this member.",
                        "&#bbbbbbFounder only."
                )
        ));

        inventory.setItem(11, item(
                Material.ORANGE_DYE,
                "&6Demote",
                List.of(
                        "&#bbbbbbDemote this member.",
                        "&#bbbbbbFounder only."
                )
        ));

        inventory.setItem(13, item(
                Material.BOOK,
                "&dView Stats",
                List.of(
                        "&#bbbbbbOpen this player's stats."
                )
        ));

        inventory.setItem(15, item(
                Material.BARRIER,
                "&cKick",
                List.of(
                        "&#bbbbbbRemove this player from the team.",
                        "&#bbbbbbAdmins can kick members."
                )
        ));

        inventory.setItem(16, item(
                Material.REDSTONE_BLOCK,
                "&4Ban",
                List.of(
                        "&#bbbbbbKick and block this player",
                        "&#bbbbbbfrom joining for &d7 days&#bbbbbb."
                )
        ));

        viewer.openInventory(inventory);
    }

    private static ItemStack playerHead(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(owner);
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamMemberGui::color).toList());
        item.setItemMeta(meta);

        return item;
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamMemberGui::color).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    private static String color(String input) {
        return TextColor.color(input);
    }
}