package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.UUID;

public final class TeamMemberManageGui {

    public static final String TITLE_SUFFIX = " Team Member";

    private TeamMemberManageGui() {
    }

    public static String title(OfflinePlayer target, TeamMemberRecord member) {
        String targetName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        String roleName = member == null ? "Unknown" : prettyRole(member.role());
        return ChatColor.DARK_GRAY + targetName + " - " + roleName;
    }

    public static boolean isTitle(String rawTitle) {
        String stripped = ChatColor.stripColor(rawTitle);
        return stripped != null && stripped.contains(" - ");
    }

    public static void open(Core core, Player viewer, UUID targetId, TeamService teamService) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        TeamMemberRecord member = teamService.getMember(targetId);
        TeamMemberRecord viewerMember = teamService.getMember(viewer.getUniqueId());

        Inventory inventory = Bukkit.createInventory(null, 27, title(target, member));

        String targetName = target.getName() == null ? targetId.toString() : target.getName();
        String roleName = member == null ? "Unknown" : prettyRole(member.role());

        inventory.setItem(4, playerHead(
                target,
                "&d" + targetName,
                List.of(
                        "&7Team Rank: &d" + roleName,
                        "&7Click an option below."
                )
        ));

        inventory.setItem(10, actionItem(
                Material.LIME_DYE,
                "&aPromote",
                canPromote(viewerMember, member),
                "&7Promote this member.",
                "&cOnly founders can promote members."
        ));

        inventory.setItem(11, actionItem(
                Material.ORANGE_DYE,
                "&6Demote",
                canDemote(viewerMember, member),
                "&7Demote this admin.",
                "&cOnly founders can demote admins."
        ));

        inventory.setItem(12, actionItem(
                Material.BARRIER,
                "&cKick",
                canKick(viewer, targetId, viewerMember, member),
                "&7Remove this player from the team.",
                "&cYou cannot kick this player."
        ));

        inventory.setItem(13, actionItem(
                Material.BOOK,
                "&dView Stats",
                true,
                "&7Open this player's statistics.",
                "&7Open this player's statistics."
        ));

        inventory.setItem(14, actionItem(
                Material.REDSTONE_BLOCK,
                "&4Ban From Team",
                canKick(viewer, targetId, viewerMember, member),
                "&7Remove and lock this player out.",
                "&cYou cannot ban this player."
        ));

        inventory.setItem(16, actionItem(
                Material.GOLDEN_HELMET,
                "&6Transfer Founder",
                canTransferFounder(viewer, targetId, viewerMember, member),
                "&7Transfer founder to this player.",
                "&cOnly the founder can transfer ownership."
        ));

        viewer.openInventory(inventory);
    }

    public static boolean canPromote(TeamMemberRecord viewer, TeamMemberRecord target) {
        return viewer != null
                && target != null
                && viewer.role() == TeamRole.FOUNDER
                && target.role() == TeamRole.MEMBER;
    }

    public static boolean canDemote(TeamMemberRecord viewer, TeamMemberRecord target) {
        return viewer != null
                && target != null
                && viewer.role() == TeamRole.FOUNDER
                && target.role() == TeamRole.ADMIN;
    }

    public static boolean canKick(Player viewerPlayer, UUID targetId, TeamMemberRecord viewer, TeamMemberRecord target) {
        if (viewerPlayer.getUniqueId().equals(targetId)) {
            return false;
        }

        if (viewer == null || target == null || target.role() == TeamRole.FOUNDER) {
            return false;
        }

        if (viewer.role() == TeamRole.FOUNDER) {
            return true;
        }

        return viewer.role() == TeamRole.ADMIN && target.role() == TeamRole.MEMBER;
    }

    public static boolean canTransferFounder(Player viewerPlayer, UUID targetId, TeamMemberRecord viewer, TeamMemberRecord target) {
        if (viewerPlayer.getUniqueId().equals(targetId)) {
            return false;
        }

        return viewer != null
                && target != null
                && viewer.role() == TeamRole.FOUNDER
                && target.role() != TeamRole.FOUNDER;
    }

    public static String prettyRole(TeamRole role) {
        return switch (role) {
            case FOUNDER -> "Founder";
            case ADMIN -> "Admin";
            case MEMBER -> "Member";
        };
    }

    private static ItemStack playerHead(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(owner);
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamMemberManageGui::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack actionItem(Material material, String name, boolean allowed, String allowedLore, String deniedLore) {
        ItemStack item = new ItemStack(allowed ? material : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color((allowed ? "" : "&7") + name));
        meta.setLore(List.of(
                color(allowed ? allowedLore : deniedLore),
                color(allowed ? "&7Double-click confirm required." : "&8Unavailable")
        ));
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}