package net.mineacle.core.teams.gui;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamMemberRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.model.TeamRole;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TeamsMainGui {

    public static final int TEAM_SIZE_LIMIT = 27;

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
                List.of(
                        "&7Type &d/team create <name>",
                        "&7to create a team."
                )
        ));

        inventory.setItem(13, item(
                Material.BARRIER,
                core.getMessage("teams.gui.no-team-item-title"),
                List.of(
                        "&fYou are not in a team.",
                        "&7Type &d/team create <name>",
                        "&7to get started."
                )
        ));

        inventory.setItem(15, item(
                hasInvite ? Material.LIME_DYE : Material.GRAY_DYE,
                hasInvite ? core.getMessage("teams.gui.pending-invite-title") : core.getMessage("teams.gui.no-pending-invite-title"),
                List.of(hasInvite ? "&7Click to view team invites." : "&7You have no current team invites.")
        ));

        player.openInventory(inventory);
    }

    private static void openTeamMenu(Core core, Player player, TeamService teamService, TeamRecord team) {
        TeamSortType sortType = TeamGuiSession.getSort(player.getUniqueId());
        List<UUID> members = teamService.getSortedTeamMembers(team.teamId(), sortType);
        int memberCount = teamService.getTeamMembers(team.teamId()).size();

        String title = team.name() + ChatColor.GRAY + " (" + memberCount + "/" + TEAM_SIZE_LIMIT + ")";
        Inventory inventory = Bukkit.createInventory(null, 54, title);

        int guiSlot = 0;
        for (UUID memberId : members) {
            if (guiSlot >= 45) {
                break;
            }

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
            String name = offlinePlayer.getName() == null ? memberId.toString() : offlinePlayer.getName();

            TeamMemberRecord member = teamService.getMember(memberId);
            String role = member == null ? "Member" : prettyRole(member.role());

            inventory.setItem(guiSlot, playerHead(
                    offlinePlayer,
                    "&f" + name,
                    List.of(
                            "&7Role: &d" + role,
                            "&7Click to view statistics."
                    )
            ));

            guiSlot++;
        }

        if (teamService.isAdmin(player.getUniqueId()) && memberCount < TEAM_SIZE_LIMIT && guiSlot < 45) {
            inventory.setItem(guiSlot, item(
                    Material.LIME_STAINED_GLASS_PANE,
                    "&aInvite Player",
                    List.of(
                            "&7Click to invite a player.",
                            "&7This will autofill &d/team invite &7in chat."
                    )
            ));
        }

        inventory.setItem(45, item(
                Material.HOPPER,
                core.getMessage("teams.gui.sort-title"),
                sortLore(sortType)
        ));

        inventory.setItem(47, item(
                team.bannerColor().bannerMaterial(),
                core.getMessage("teams.gui.team-home-title"),
                List.of(core.getMessage("teams.gui.team-home-lore-1"))
        ));

        if (teamService.isAdmin(player.getUniqueId())) {
            inventory.setItem(48, item(
                    team.friendlyFire() ? Material.REDSTONE_TORCH : Material.LEVER,
                    core.getMessage("teams.gui.friendly-fire-title"),
                    List.of(
                            team.friendlyFire() ? core.getMessage("teams.gui.friendly-fire-enabled") : core.getMessage("teams.gui.friendly-fire-disabled"),
                            core.getMessage("teams.gui.friendly-fire-lore-2")
                    )
            ));
        }

        String leaderName = Bukkit.getOfflinePlayer(team.founder()).getName();
        if (leaderName == null) {
            leaderName = team.founder().toString();
        }

        inventory.setItem(49, item(
                Material.IRON_HELMET,
                core.getMessage("teams.gui.team-info-title"),
                List.of(
                        "&fTeam: " + teamService.formatTeamName(team),
                        "&fLeader: &d" + leaderName,
                        "&fMembers: &d" + memberCount + "&7/" + TEAM_SIZE_LIMIT,
                        "&fSort: &d" + sortType.displayName()
                )
        ));

        if (teamService.isAdmin(player.getUniqueId())) {
            inventory.setItem(50, item(
                    Material.NAME_TAG,
                    core.getMessage("teams.gui.manage-button-title"),
                    List.of(core.getMessage("teams.gui.manage-button-lore-1"))
            ));

            inventory.setItem(51, item(
                    Material.PAPER,
                    core.getMessage("teams.gui.bans-button-title"),
                    List.of(core.getMessage("teams.gui.bans-button-lore-1"))
            ));
        }

        player.openInventory(inventory);
    }

    private static List<String> sortLore(TeamSortType current) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Click to cycle sorting.");
        lore.add("");

        for (TeamSortType type : TeamSortType.values()) {
            if (type == current) {
                lore.add("&d▶ " + type.displayName());
            } else {
                lore.add("&8• " + type.displayName());
            }
        }

        return lore;
    }

    private static String prettyRole(TeamRole role) {
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
        item.setItemMeta(meta);

        return item;
    }

    private static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}