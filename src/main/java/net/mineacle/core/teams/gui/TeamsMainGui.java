package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamInviteService;
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

public final class TeamsMainGui {

    public static final String TITLE_SUFFIX = ")";

    private TeamsMainGui() {
    }

    public static void open(Core core, Player player, TeamService teamService, TeamInviteService inviteService) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            player.sendMessage("§7Type §d/team create <name> §7to create a team.");
            player.sendMessage("§7Type §d/team join §7to view invites.");
            return;
        }

        int memberCount = teamService.getTeamMembers(team.teamId()).size();

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                ChatColor.DARK_GRAY + team.name() + " (" + memberCount + "/" + teamService.maxMembers() + ")"
        );

        int slot = 0;
        for (UUID memberId : teamService.getTeamMembers(team.teamId())) {
            if (slot >= 45) {
                break;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            TeamMemberRecord member = teamService.getMember(memberId);

            String name = offlinePlayer.getName() == null ? memberId.toString() : offlinePlayer.getName();
            String role = member == null ? "Member" : member.role().displayName();

            inventory.setItem(slot, playerHead(
                    offlinePlayer,
                    "&f" + name,
                    List.of(
                            "&7Role: &d" + role,
                            "&7Click to manage"
                    )
            ));

            slot++;
        }

        if (teamService.isAdmin(player.getUniqueId())
                && memberCount < teamService.maxMembers()
                && slot < 45) {
            inventory.setItem(slot, item(
                    Material.LIME_STAINED_GLASS_PANE,
                    "&aInvite Player",
                    List.of("&7Click to autofill &d/team invite")
            ));
        }

        inventory.setItem(47, item(
                Material.PURPLE_BANNER,
                "&dTeam Home",
                List.of("&7Click to teleport to Team Home")
        ));

        inventory.setItem(49, item(
                Material.BOOK,
                "&dTeam Info",
                List.of(
                        "&7Name: &d" + team.name(),
                        "&7Members: &d" + memberCount + "&7/" + teamService.maxMembers()
                )
        ));

        if (teamService.isAdmin(player.getUniqueId())) {
            inventory.setItem(51, pvpItem(team.friendlyFire()));
        }

        player.openInventory(inventory);
    }

    private static ItemStack pvpItem(boolean friendlyFire) {
        return item(
                Material.DIAMOND_SWORD,
                "&dTeam PVP",
                List.of("&fCurrently: " + (friendlyFire ? "&aON" : "&cOFF"))
        );
    }

    private static ItemStack playerHead(OfflinePlayer owner, String name, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(owner);
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamsMainGui::color).toList());
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
        meta.setLore(lore.stream().map(TeamsMainGui::color).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}