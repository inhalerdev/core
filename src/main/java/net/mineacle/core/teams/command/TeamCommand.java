package net.mineacle.core.teams.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.mineacle.core.Core;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.teams.gui.TeamInviteGui;
import net.mineacle.core.teams.gui.TeamsMainGui;
import net.mineacle.core.teams.model.TeamInviteRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.model.TeamRole;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TeamCommand implements CommandExecutor, TabCompleter {

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
            player.sendMessage("§cYou do not have permission.");
            return true;
        }

        if (args.length == 0) {
            if (teamService.hasTeam(player.getUniqueId())) {
                TeamsMainGui.open(core, player, teamService, inviteService);
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

            case "leave" -> {
                return leave(player);
            }

            case "disband" -> {
                return disband(player);
            }

            case "kick" -> {
                return kick(player, args);
            }

            case "promote" -> {
                return role(player, args, TeamRole.ADMIN);
            }

            case "demote" -> {
                return role(player, args, TeamRole.MEMBER);
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

            case "friendlyfire", "pvp" -> {
                return friendlyFire(player);
            }

            default -> {
                player.sendMessage("§cUnknown team command.");
                return true;
            }
        }
    }

    private boolean create(Player player, String[] args) {
        if (teamService.hasTeam(player.getUniqueId())) {
            player.sendMessage("§cYou are already in a team.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /team create <name>");
            return true;
        }

        String name = args[1];

        if (!teamService.createTeam(player.getUniqueId(), name)) {
            player.sendMessage("§cCould not create that team. Use 3-16 letters, numbers, or underscores.");
            return true;
        }

        player.sendMessage("§aTeam created.");
        TeamsMainGui.open(core, player, teamService, inviteService);
        return true;
    }

    private boolean join(Player player) {
        if (!inviteService.hasInvite(player.getUniqueId())) {
            player.sendActionBar(Component.text("§cYou have no current team invites."));
            player.sendMessage("§cYou have no current team invites.");
            return true;
        }

        TeamInviteGui.open(core, player, inviteService, teamService);
        return true;
    }

    private boolean accept(Player player) {
        TeamInviteRecord invite = inviteService.getInvite(player.getUniqueId());

        if (invite == null) {
            player.sendMessage("§cYou have no current team invites.");
            return true;
        }

        if (!inviteService.acceptInvite(player.getUniqueId())) {
            player.sendMessage("§cCould not accept invite.");
            return true;
        }

        player.sendMessage("§aInvite accepted.");
        TeamsMainGui.open(core, player, teamService, inviteService);
        return true;
    }

    private boolean decline(Player player) {
        if (!inviteService.denyInvite(player.getUniqueId())) {
            player.sendMessage("§cYou have no current team invites.");
            return true;
        }

        player.sendMessage("§cInvite declined.");
        return true;
    }

    private boolean invite(Player player, String[] args) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage("§cOnly admins can invite players.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /team invite <player>");
            return true;
        }

        if (teamService.getTeamMembers(team.teamId()).size() >= teamService.maxMembers()) {
            player.sendMessage("§cYour team is full.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            player.sendMessage("§cThat player is not online.");
            return true;
        }

        if (teamService.hasTeam(target.getUniqueId())) {
            player.sendMessage("§cThat player is already in a team.");
            return true;
        }

        if (!inviteService.createInvite(team.teamId(), player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage("§cCould not send invite.");
            return true;
        }

        player.sendMessage("§aInvite sent to §f" + target.getName() + "§a.");

        target.sendActionBar(Component.text("§dTeam invite from §f" + player.getName()));
        target.sendMessage("§7You were invited to join §d" + team.name() + "§7.");
        target.sendMessage(Component.text("§a[VIEW INVITE]")
                .clickEvent(ClickEvent.runCommand("/team join")));

        return true;
    }

    private boolean leave(Player player) {
        if (!teamService.removeMember(player.getUniqueId())) {
            player.sendMessage("§cYou cannot leave as founder. Use /team disband.");
            return true;
        }

        player.sendMessage("§cYou left your team.");
        return true;
    }

    private boolean disband(Player player) {
        if (!teamService.disbandTeam(player.getUniqueId())) {
            player.sendMessage("§cOnly the founder can disband the team.");
            return true;
        }

        player.sendMessage("§cTeam disbanded.");
        return true;
    }

    private boolean kick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /team kick <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cThat player must be online.");
            return true;
        }

        if (!teamService.kickMember(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage("§cYou cannot kick that player.");
            return true;
        }

        player.sendMessage("§cPlayer kicked.");
        target.sendMessage("§cYou were kicked from your team.");
        return true;
    }

    private boolean role(Player player, String[] args, TeamRole role) {
        if (args.length < 2) {
            player.sendMessage(role == TeamRole.ADMIN ? "§cUsage: /team promote <player>" : "§cUsage: /team demote <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§cThat player must be online.");
            return true;
        }

        if (!teamService.setMemberRole(player.getUniqueId(), target.getUniqueId(), role)) {
            player.sendMessage("§cYou cannot change that player's role.");
            return true;
        }

        player.sendMessage(role == TeamRole.ADMIN ? "§aPlayer promoted." : "§aPlayer demoted.");
        return true;
    }

    private boolean home(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        org.bukkit.Location home = teamHomeService.getTeamHome(team.teamId());

        if (home == null) {
            player.sendMessage("§cYour team does not have a home set.");
            return true;
        }

        teleportService.begin(player, "Team Home", () -> {
            player.teleport(home);
            player.sendMessage("§aTeleported to Team Home.");
        });

        return true;
    }

    private boolean setHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage("§cOnly admins can set team home.");
            return true;
        }

        teamHomeService.setTeamHome(team.teamId(), player.getLocation());
        player.sendMessage("§aTeam home set.");
        return true;
    }

    private boolean delHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage("§cOnly admins can delete team home.");
            return true;
        }

        if (!teamHomeService.deleteTeamHome(team.teamId())) {
            player.sendMessage("§cYour team does not have a home set.");
            return true;
        }

        player.sendMessage("§cTeam home deleted.");
        return true;
    }

    private boolean friendlyFire(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());

        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage("§cOnly admins can toggle friendly fire.");
            return true;
        }

        boolean newValue = !team.friendlyFire();
        teamService.setFriendlyFire(team.teamId(), newValue);

        player.sendMessage(newValue ? "§aFriendly fire enabled." : "§cFriendly fire disabled.");
        return true;
    }

    private void sendNoTeamPrompt(Player player) {
        player.sendMessage("§cYou are not in a team.");
        player.sendMessage(Component.text("§7Type §d/team create <name> §7to create a team.")
                .clickEvent(ClickEvent.suggestCommand("/team create ")));
        player.sendMessage(Component.text("§7Type §d/team join §7to view invites.")
                .clickEvent(ClickEvent.runCommand("/team join")));
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
                        "home",
                        "sethome",
                        "delhome",
                        "friendlyfire",
                        "pvp",
                        "promote",
                        "demote",
                        "kick",
                        "leave",
                        "disband"
                );
            } else {
                options = List.of("home", "leave");
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
                || args[0].equalsIgnoreCase("kick"))) {
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