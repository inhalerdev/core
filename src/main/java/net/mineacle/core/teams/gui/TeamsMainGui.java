package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.model.TeamSortType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TeamsMainGui {

    public static String NO_TEAM_TITLE(Core core) {
        return color(core.getMessage("teams.gui.no-team-title"));
    }

    public static String TEAM_TITLE(Core core) {
        return color(core.getMessage("teams.gui.team-title"));
    }

    private TeamsMainGui() {
    }

    public static void open(Core core, Player player, TeamService teamService, TeamInviteService inviteService) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            openNoTeamMenu(core, player, inviteService.hasInvite(player.getUniqueId()));
            return;
        }

        openTeamMenu(core, player, teamService, team);
    }

    private static void openNoTeamMenu(Core core, Player player, boolean hasInvite) {
        Inventory inventory = Bukkit.createInventory(null, 27, NO_TEAM_TITLE(core));

        inventory.setItem(11, item(
                Material.NAME_TAG,
                core.getMessage("teams.gui.create-team-title"),
                List.of(core.getMessage("teams.gui.create-team-lore-1"))
        ));

        inventory.setItem(13, item(
                Material.BARRIER,
                core.getMessage("teams.gui.no-team-item-title"),
                List.of(core.getMessage("teams.gui.no-team-item-lore-1"))
        ));

        inventory.setItem(15, item(
                hasInvite ? Material.LIME_DYE : Material.GRAY_DYE,
                hasInvite ? core.getMessage("teams.gui.pending-invite-title") : core.getMessage("teams.gui.no-pending-invite-title"),
                List.of(hasInvite ? core.getMessage("teams.gui.pending-invite-lore-1") : core.getMessage("teams.gui.no-pending-invite-lore-1"))
        ));

        player.openInventory(inventory);
    }

    private static void openTeamMenu(Core core, Player player, TeamService teamService, TeamRecord team) {
        TeamSortType sortType = TeamGuiSession.getSort(player.getUniqueId());
        int page = TeamGuiSession.getPage(player.getUniqueId());
        String search = TeamGuiSession.getMemberSearch(player.getUniqueId()).toLowerCase(Locale.ROOT);

        List<UUID> sortedMembers = teamService.getSortedTeamMembers(team.teamId(), sortType);
        List<UUID> filteredMembers = new ArrayList<>();

        for (UUID memberId : sortedMembers) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            String name = offlinePlayer.getName() == null ? memberId.toString() : offlinePlayer.getName();

            if (!search.isBlank() && !name.toLowerCase(Locale.ROOT).contains(search)) {
                continue;
            }

            filteredMembers.add(memberId);
        }

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                TEAM_TITLE(core) + ChatColor.GRAY + " (" + (page + 1) + ")"
        );

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, filteredMembers.size());

        int guiSlot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            UUID memberId = filteredMembers.get(i);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            String name = offlinePlayer.getName() == null ? memberId.toString() : offlinePlayer.getName();
            String role = prettyRole(teamService.getMember(memberId).role());

            inventory.setItem(guiSlot, item(
                    Material.PLAYER_HEAD,
                    "&f" + name,
                    List.of(
                            core.getMessage("teams.gui.member-head-lore-1").replace("%role%", role),
                            core.getMessage("teams.gui.member-head-lore-2")
                    )
            ));
            guiSlot++;
        }

        inventory.setItem(45, item(
                Material.OAK_SIGN,
                core.getMessage("teams.gui.search-title"),
                search.isBlank()
                        ? List.of(core.getMessage("teams.gui.search-lore-1"))
                        : List.of(core.getMessage("teams.gui.search-lore-current").replace("%search%", search))
        ));

        inventory.setItem(46, item(
                Material.HOPPER,
                core.getMessage("teams.gui.sort-title"),
                List.of(
                        core.getMessage("teams.gui.sort-lore-1").replace("%sort%", sortType.displayName()),
                        core.getMessage("teams.gui.sort-lore-2")
                )
        ));

        inventory.setItem(47, item(
                Material.PLAYER_HEAD,
                core.getMessage("teams.gui.invite-player-title-button"),
                List.of(core.getMessage("teams.gui.invite-player-lore-1"))
        ));

        inventory.setItem(48, item(
                Material.NAME_TAG,
                core.getMessage("teams.gui.manage-button-title"),
                List.of(core.getMessage("teams.gui.manage-button-lore-1"))
        ));

        String leaderName = Bukkit.getOfflinePlayer(team.founder()).getName();
        if (leaderName == null) {
            leaderName = team.founder().toString();
        }

        inventory.setItem(49, item(
                Material.IRON_HELMET,
                core.getMessage("teams.gui.team-info-title"),
                List.of(
                        core.getMessage("teams.gui.team-info-lore-1").replace("%team%", teamService.formatTeamName(team)),
                        core.getMessage("teams.gui.team-info-lore-2").replace("%leader%", leaderName),
                        core.getMessage("teams.gui.team-info-lore-3").replace("%members%", String.valueOf(filteredMembers.size()))
                )
        ));

        inventory.setItem(50, item(
                Material.ARROW,
                core.getMessage("teams.gui.next-title"),
                List.of(core.getMessage("teams.gui.next-lore-1"))
        ));

        inventory.setItem(51, item(
                teamService.isFounder(player.getUniqueId()) ? Material.TNT : Material.RED_STAINED_GLASS_PANE,
                teamService.isFounder(player.getUniqueId()) ? core.getMessage("teams.gui.disband-title") : core.getMessage("teams.gui.leave-title"),
                List.of(teamService.isFounder(player.getUniqueId()) ? core.getMessage("teams.gui.disband-lore-1") : core.getMessage("teams.gui.leave-lore-1"))
        ));

        inventory.setItem(52, item(
                team.bannerColor().bannerMaterial(),
                core.getMessage("teams.gui.team-home-title"),
                List.of(core.getMessage("teams.gui.team-home-lore-1"))
        ));

        inventory.setItem(53, item(
                teamService.isFounder(player.getUniqueId()) ? Material.PAPER : (team.friendlyFire() ? Material.REDSTONE_TORCH : Material.LEVER),
                teamService.isFounder(player.getUniqueId()) ? core.getMessage("teams.gui.bans-button-title") : core.getMessage("teams.gui.friendly-fire-title"),
                teamService.isFounder(player.getUniqueId())
                        ? List.of(core.getMessage("teams.gui.bans-button-lore-1"))
                        : List.of(
                                team.friendlyFire() ? core.getMessage("teams.gui.friendly-fire-enabled") : core.getMessage("teams.gui.friendly-fire-disabled"),
                                core.getMessage("teams.gui.friendly-fire-lore-2")
                        )
        ));

        player.openInventory(inventory);
    }

    private static String prettyRole(net.mineacle.core.teams.model.TeamRole role) {
        return switch (role) {
            case FOUNDER -> "Founder";
            case ADMIN -> "Admin";
            case MEMBER -> "Member";
        };
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(TeamsMainGui::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}