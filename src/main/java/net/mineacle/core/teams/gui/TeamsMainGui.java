package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.common.player.DisplayNames;
import net.mineacle.core.common.text.TextColor;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamHomeService;
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

    public static final int TEAM_HOME_SLOT = 47;
    public static final int TEAM_CHAT_SLOT = 48;
    public static final int TEAM_INFO_SLOT = 49;
    public static final int TEAM_PVP_SLOT = 51;

    private TeamsMainGui() {
    }

    public static void open(Core core, Player player, TeamService teamService, TeamInviteService inviteService) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            TeamStartGui.open(core, player, inviteService);
            return;
        }

        TeamHomeService teamHomeService = new TeamHomeService(core, teamService);
        boolean hasTeamHome = teamHomeService.hasTeamHome(team.teamId());
        boolean teamChatEnabled = teamService.isTeamChatEnabled(player.getUniqueId());

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

            String name = DisplayNames.prefixedDisplayName(offlinePlayer);
            String role = member == null ? "Member" : member.role().displayName();

            inventory.setItem(slot, playerHead(
                    offlinePlayer,
                    name,
                    List.of(
                            "&#bbbbbbRole: &d" + role,
                            "&#bbbbbbClick to manage"
                    )
            ));

            slot++;
        }

        if (teamService.isAdmin(player.getUniqueId()) && memberCount < teamService.maxMembers() && slot < 45) {
            inventory.setItem(slot, item(
                    Material.LIME_STAINED_GLASS_PANE,
                    "&aInvite Player",
                    List.of("&#bbbbbbClick to autofill &d/team invite")
            ));
        }

        inventory.setItem(TEAM_HOME_SLOT, teamHomeItem(hasTeamHome));
        inventory.setItem(TEAM_CHAT_SLOT, teamChatItem(teamChatEnabled));

        inventory.setItem(TEAM_INFO_SLOT, item(
                Material.BOOK,
                "&dTeam Info",
                List.of(
                        "&#bbbbbbName: &d" + team.name(),
                        "&#bbbbbbMembers: &d" + memberCount + "&#bbbbbb/" + teamService.maxMembers()
                )
        ));

        if (teamService.isAdmin(player.getUniqueId())) {
            inventory.setItem(TEAM_PVP_SLOT, pvpItem(team.friendlyFire()));
        }

        player.openInventory(inventory);
    }

    private static ItemStack teamHomeItem(boolean hasTeamHome) {
        if (hasTeamHome) {
            return item(
                    Material.PURPLE_BANNER,
                    "&dTeam Home",
                    List.of(
                            "&#bbbbbbStatus: &aSet",
                            "&#bbbbbbClick to teleport to Team Home"
                    )
            );
        }

        return item(
                Material.WHITE_BANNER,
                "&fTeam Home",
                List.of(
                        "&#bbbbbbStatus: &cNot Set",
                        "&#bbbbbbClick to open Homes and set Team Home"
                )
        );
    }

    private static ItemStack teamChatItem(boolean enabled) {
        return item(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                "&dTeam Chat",
                List.of(
                        "&#bbbbbbCurrently: " + (enabled ? "&aEnabled" : "&cDisabled"),
                        "&#bbbbbbClick to toggle"
                )
        );
    }

    private static ItemStack pvpItem(boolean friendlyFire) {
        return item(
                Material.DIAMOND_SWORD,
                "&dTeam PvP",
                List.of(
                        "&#bbbbbbCurrently: " + (friendlyFire ? "&aON" : "&cOFF"),
                        "&#bbbbbbClick to toggle"
                )
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
        return TextColor.color(input);
    }
}