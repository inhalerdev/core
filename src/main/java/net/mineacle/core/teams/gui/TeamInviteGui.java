package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamInviteRecord;
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

public final class TeamInviteGui {

    public static String TITLE(Core core) {
        return color(core.getMessage("teams.gui.invites-title"));
    }

    private TeamInviteGui() {
    }

    public static void open(Core core, Player player, TeamInviteService inviteService, TeamService teamService) {
        Inventory inventory = Bukkit.createInventory(null, 27, TITLE(core));

        TeamInviteRecord invite = inviteService.getInvite(player.getUniqueId());

        if (invite == null) {
            inventory.setItem(13, item(
                    Material.BARRIER,
                    core.getMessage("teams.gui.no-invite-title"),
                    List.of(core.getMessage("teams.gui.no-invite-lore-1"))
            ));
            player.openInventory(inventory);
            return;
        }

        TeamRecord team = teamService.getTeamById(invite.teamId());
        OfflinePlayer inviter = Bukkit.getOfflinePlayer(invite.inviterId());

        String teamName = team == null ? "Unknown Team" : teamService.formatTeamName(team);
        String inviterName = inviter.getName() == null ? invite.inviterId().toString() : inviter.getName();
        Material bannerMaterial = team == null ? Material.PURPLE_BANNER : team.bannerColor().bannerMaterial();

        inventory.setItem(11, item(
                Material.LIME_STAINED_GLASS_PANE,
                core.getMessage("teams.gui.invite-accept-title"),
                List.of(
                        core.getMessage("teams.gui.invite-accept-lore-1").replace("%team%", teamName),
                        core.getMessage("teams.gui.invite-accept-lore-2").replace("%inviter%", inviterName),
                        core.getMessage("teams.gui.invite-accept-lore-3")
                )
        ));

        inventory.setItem(13, item(
                bannerMaterial,
                core.getMessage("teams.gui.invite-center-title").replace("%team%", teamName),
                List.of(core.getMessage("teams.gui.invite-center-lore-1").replace("%inviter%", inviterName))
        ));

        inventory.setItem(15, item(
                Material.RED_STAINED_GLASS_PANE,
                core.getMessage("teams.gui.invite-deny-title"),
                List.of(
                        core.getMessage("teams.gui.invite-deny-lore-1").replace("%team%", teamName),
                        core.getMessage("teams.gui.invite-deny-lore-2").replace("%inviter%", inviterName),
                        core.getMessage("teams.gui.invite-deny-lore-3")
                )
        ));

        player.openInventory(inventory);
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamInviteGui::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}