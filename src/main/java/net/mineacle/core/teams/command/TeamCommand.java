package net.mineacle.core.teams.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.mineacle.core.Core;
import net.mineacle.core.common.gui.MenuHistory;
import net.mineacle.core.common.player.DisplayNames;
import net.mineacle.core.common.text.TextColor;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.teams.gui.TeamConfirmGui;
import net.mineacle.core.teams.gui.TeamInviteGui;
import net.mineacle.core.teams.gui.TeamsMainGui;
import net.mineacle.core.teams.model.TeamInviteRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TeamCommand implements CommandExecutor, TabCompleter {

    private static final String META_TARGET = "simple_team_target";
    private static final String META_ACTION = "simple_team_action";
    private static final String META_CONFIRM = "simple_team_confirm";

    private final Core core;
    private final TeamService teamService;
    private final TeamInviteService inviteService;
    private final TeamHomeService teamHomeService;
    private final TeleportService teleportService;

    public TeamCommand(
            Core core,
            TeamService teamService,
            TeamInviteService inviteService,
            TeamHomeService teamHomeService,
            TeleportService teleportService
    ) {
        this.core = core;
        this.teamService = teamService;
        this.inviteService = inviteService;
        this.teamHomeService = teamHomeService;
        this.teleportService = teleportService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("mineacleteams.use")) {
            player.sendMessage(TextColor.color("&cYou do not have permission."));
            return true;
        }

        if (args.length == 0) {
            if (teamService.hasTeam(player.getUniqueId())) {
                MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
            } else {
                sendNoTeamPrompt(player);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (!teamService.hasTeam(player.getUniqueId())
                && !sub.equals("create")
                && !sub.equals("join")
                && !sub.equals("accept")
                && !sub.equals("decline")
                && !sub.equals("deny")
                && !sub.equals("invites")) {
            sendNoTeamPrompt(player);
            return true;
        }

        switch (sub) {
            case "create" -> {
                return create(player, args);
            }

            case "join", "invites" -> {
                return join(player);
            }

            case "accept" -> {
                return accept(player);
            }

            case "decline", "deny" -> {
                return decline(player);
            }

            case "invite" -> {
                return invite(player, args);
            }

            case "chat" -> {
                return teamChat(player);
            }

            case "leave" -> {
                return leave(player);
            }

            case "disband" -> {
                return disband(player);
            }

            case "kick" -> {
                return confirmTargetAction(player, args, "KICK", "Kick Player", "§cUsage: /team kick <player>");
            }

            case "ban" -> {
                return confirmTargetAction(player, args, "BAN", "Ban Player", "§cUsage: /team ban <player>");
            }

            case "promote" -> {
                return confirmTargetAction(player, args, "PROMOTE", "Promote Player", "§cUsage: /team promote <player>");
            }

            case "demote" -> {
                return confirmTargetAction(player, args, "DEMOTE", "Demote Player", "§cUsage: /team demote <player>");
            }

            case "home" -> {
                return home(player);
            }

            case "sethome" -> {
                return setHome(player);
            }

            case "delhome" -> {
                return delHome(player);
            }

            case "pvp" -> {
                return pvp(player);
            }

            default -> {
                player.sendMessage(TextColor.color("&cUnknown team command."));
                return true;
            }
        }
    }

    private boolean create(Player player, String[] args) {
        if (teamService.hasTeam(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cYou are already in a team."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(TextColor.color("&cUsage: /team create <name>"));
            return true;
        }

        String name = args[1];

        if (!teamService.createTeam(player.getUniqueId(), name)) {
            player.sendMessage(TextColor.color("&cCould not create that team. Use 3-16 letters, numbers, or underscores."));
            return true;
        }

        player.sendMessage(TextColor.color("&aTeam created."));
        MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
        return true;
    }

    private boolean join(Player player) {
        if (!inviteService.hasInvite(player.getUniqueId())) {
            String message = TextColor.color("&cYou have no current team invites.");
            player.sendActionBar(Component.text(message));
            player.sendMessage(message);
            return true;
        }

        TeamInviteGui.open(core, player, inviteService, teamService);
        return true;
    }

    private boolean accept(Player player) {
        TeamInviteRecord invite = inviteService.getInvite(player.getUniqueId());

        if (invite == null) {
            player.sendMessage(TextColor.color("&cYou have no current team invites."));
            return true;
        }

        if (!inviteService.acceptInvite(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cCould not accept invite."));
            return true;
        }

        player.sendMessage(TextColor.color("&aInvite accepted."));
        MenuHistory.openRoot(core, player, () -> TeamsMainGui.open(core, player, teamService, inviteService));
        return true;
    }

    private boolean decline(Player player) {
        if (!inviteService.denyInvite(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cYou have no current team invites."));
            return true;
        }

        player.sendMessage(TextColor.color("&cInvite declined."));
        return true;
    }

    private boolean invite(Player player, String[] args) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage(TextColor.color("&cYou are not in a team."));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cOnly admins can invite players."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(TextColor.color("&cUsage: /team invite <player>"));
            return true;
        }

        if (teamService.getTeamMembers(team.teamId()).size() >= teamService.maxMembers()) {
            player.sendMessage(TextColor.color("&cYour team is full."));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            player.sendMessage(TextColor.color("&cThat player is not online."));
            return true;
        }

        if (teamService.hasTeam(target.getUniqueId())) {
            player.sendMessage(TextColor.color("&cThat player is already in a team."));
            return true;
        }

        if (teamService.isBanned(team.teamId(), target.getUniqueId())) {
            player.sendMessage(TextColor.color("&cThat player is banned from joining this team."));
            return true;
        }

        if (!inviteService.createInvite(team.teamId(), player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(TextColor.color("&cCould not send invite."));
            return true;
        }

        String senderName = DisplayNames.prefixedDisplayName(player);
        String targetName = DisplayNames.prefixedDisplayName(target);

        player.sendMessage(TextColor.color("&aInvite sent to " + targetName + "&a."));

        target.sendActionBar(Component.text(TextColor.color("&dTeam invite from " + senderName)));
        target.sendMessage(TextColor.color("&#bbbbbbYou were invited to join &d" + team.name() + "&#bbbbbb."));
        target.sendMessage(Component.text(TextColor.color("&a[View Invite]"))
                .clickEvent(ClickEvent.runCommand("/team join")));

        return true;
    }

    private boolean teamChat(Player player) {
        if (!teamService.hasTeam(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cYou are not in a team."));
            return true;
        }

        boolean enabled = teamService.toggleTeamChat(player.getUniqueId());

        String message = enabled
                ? TextColor.color("&#bbbbbbTeam chat enabled")
                : TextColor.color("&#bbbbbbTeam chat disabled");

        player.sendMessage(message);
        player.sendActionBar(Component.text(message));
        return true;
    }

    private boolean leave(Player player) {
        if (!teamService.removeMember(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cYou cannot leave as founder. Use /team disband."));
            return true;
        }

        player.sendMessage(TextColor.color("&cYou left your team."));
        return true;
    }

    private boolean disband(Player player) {
        if (!teamService.isFounder(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cOnly the founder can disband the team."));
            return true;
        }

        clearConfirmMeta(player);
        player.setMetadata(META_ACTION, new FixedMetadataValue(core, "DISBAND"));

        MenuHistory.openRoot(core, player, () -> TeamConfirmGui.open(core, player, "Disband Team"));
        return true;
    }

    private boolean confirmTargetAction(Player player, String[] args, String action, String title, String usage) {
        if (args.length < 2) {
            player.sendMessage(TextColor.color(usage));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            player.sendMessage(TextColor.color("&cThat player must be online."));
            return true;
        }

        TeamRecord playerTeam = teamService.getTeamByPlayer(player.getUniqueId());
        TeamRecord targetTeam = teamService.getTeamByPlayer(target.getUniqueId());

        if (playerTeam == null || targetTeam == null || !playerTeam.teamId().equals(targetTeam.teamId())) {
            player.sendMessage(TextColor.color("&cThat player is not in your team."));
            return true;
        }

        clearConfirmMeta(player);
        player.setMetadata(META_ACTION, new FixedMetadataValue(core, action));
        player.setMetadata(META_TARGET, new FixedMetadataValue(core, target.getUniqueId().toString()));

        MenuHistory.openRoot(core, player, () -> TeamConfirmGui.open(core, player, title));
        return true;
    }

    private boolean home(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage(TextColor.color("&cYou are not in a team."));
            return true;
        }

        org.bukkit.Location home = teamHomeService.getTeamHome(team.teamId());

        if (home == null) {
            player.sendMessage(TextColor.color("&cYour team does not have a home set."));
            return true;
        }

        teleportService.begin(player, "Team Home", () -> {
            player.teleport(home);
            player.sendMessage(TextColor.color("&aTeleported to Team Home."));
        });

        return true;
    }

    private boolean setHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage(TextColor.color("&cYou are not in a team."));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cOnly admins can set team home."));
            return true;
        }

        teamHomeService.setTeamHome(team.teamId(), player.getLocation());
        player.sendMessage(TextColor.color("&aTeam home set."));
        return true;
    }

    private boolean delHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage(TextColor.color("&cYou are not in a team."));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cOnly admins can delete team home."));
            return true;
        }

        if (!teamHomeService.deleteTeamHome(team.teamId())) {
            player.sendMessage(TextColor.color("&cYour team does not have a home set."));
            return true;
        }

        player.sendMessage(TextColor.color("&cTeam home deleted."));
        return true;
    }

    private boolean pvp(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage(TextColor.color("&cYou are not in a team."));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(TextColor.color("&cOnly admins can toggle Team PvP."));
            return true;
        }

        boolean enabled = !team.friendlyFire();
        teamService.setFriendlyFire(team.teamId(), enabled);

        String message = enabled
                ? TextColor.color("&aTeam PvP enabled.")
                : TextColor.color("&cTeam PvP disabled.");

        player.sendMessage(message);
        player.sendActionBar(Component.text(message));
        return true;
    }

    private void sendNoTeamPrompt(Player player) {
        player.sendMessage(TextColor.color("&cYou are not in a team."));
        player.sendMessage(Component.text(TextColor.color("&#bbbbbbType &d/team create <name> &#bbbbbbto create a team."))
                .clickEvent(ClickEvent.suggestCommand("/team create ")));
        player.sendMessage(Component.text(TextColor.color("&#bbbbbbType &d/team join &#bbbbbbto view invites."))
                .clickEvent(ClickEvent.runCommand("/team join")));
    }

    private void clearConfirmMeta(Player player) {
        player.removeMetadata(META_ACTION, core);
        player.removeMetadata(META_TARGET, core);
        player.removeMetadata(META_CONFIRM, core);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return completions;
        }

        if (args.length == 1) {
            List<String> options;

            if (!teamService.hasTeam(player.getUniqueId())) {
                options = List.of("create", "join");
            } else if (teamService.isAdmin(player.getUniqueId())) {
                options = List.of(
                        "invite",
                        "chat",
                        "home",
                        "pvp",
                        "promote",
                        "demote",
                        "kick",
                        "ban",
                        "leave",
                        "disband"
                );
            } else {
                options = List.of("chat", "home", "leave");
            }

            String partial = args[0].toLowerCase(Locale.ROOT);

            for (String option : options) {
                if (option.startsWith(partial)) {
                    completions.add(option);
                }
            }

            return completions;
        }

        if (args.length == 2
                && (args[0].equalsIgnoreCase("invite")
                || args[0].equalsIgnoreCase("promote")
                || args[0].equalsIgnoreCase("demote")
                || args[0].equalsIgnoreCase("kick")
                || args[0].equalsIgnoreCase("ban"))) {
            String partial = args[1].toLowerCase(Locale.ROOT);

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                    completions.add(online.getName());
                }
            }
        }

        return completions;
    }
}