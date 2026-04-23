package net.mineacle.core.teams.command;

import net.mineacle.core.Core;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TeamCommand implements CommandExecutor, TabCompleter {

    private final Core core;
    private final TeamService teamService;
    private final TeamInviteService inviteService;
    private final TeamHomeService teamHomeService;

    public TeamCommand(Core core, TeamService teamService, TeamInviteService inviteService, TeamHomeService teamHomeService) {
        this.core = core;
        this.teamService = teamService;
        this.inviteService = inviteService;
        this.teamHomeService = teamHomeService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(core.getMessage("general.players-only"));
            return true;
        }

        if (!player.hasPermission("mineacleteams.use")) {
            player.sendMessage(core.getMessage("general.no-permission"));
            return true;
        }

        if (args.length == 0) {
            TeamsMainGui.open(core, player, teamService, inviteService);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "create":
                return handleCreate(player, args);
            case "leave":
                return handleLeave(player);
            case "disband":
                return handleDisband(player);
            case "invite":
                return handleInvite(player, args);
            case "accept":
                return handleAccept(player);
            case "deny":
                return handleDeny(player);
            case "home":
                return handleHome(player);
            case "sethome":
                return handleSetHome(player);
            case "delhome":
                return handleDeleteHome(player);
            default:
                player.sendMessage("§cUnknown subcommand.");
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /team create <name>");
            return true;
        }

        String teamName = joinArgs(args, 1);

        if (!teamService.isValidTeamName(teamName)) {
            player.sendMessage("§cThat team name is not valid.");
            return true;
        }

        if (teamService.hasTeam(player.getUniqueId())) {
            player.sendMessage("§cYou are already in a team.");
            return true;
        }

        if (teamService.getTeamByName(teamName) != null) {
            player.sendMessage("§cThat team name is already taken.");
            return true;
        }

        if (!teamService.createTeam(player.getUniqueId(), teamName)) {
            player.sendMessage("§cCould not create that team.");
            return true;
        }

        player.sendMessage("§7Created team §d" + teamName);
        TeamsMainGui.open(core, player, teamService, inviteService);
        return true;
    }

    private boolean handleLeave(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (teamService.isFounder(player.getUniqueId())) {
            player.sendMessage("§cFounders cannot leave. Disband the team instead.");
            return true;
        }

        teamService.removeMember(player.getUniqueId());
        player.sendMessage("§cYou left your team.");
        return true;
    }

    private boolean handleDisband(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (!teamService.isFounder(player.getUniqueId())) {
            player.sendMessage("§cOnly the founder can disband the team.");
            return true;
        }

        teamService.disbandTeam(team.teamId());
        player.sendMessage("§cTeam disbanded.");
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage("§cYou do not have permission to invite players.");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage("§cUsage: /team invite <player>");
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
            player.sendMessage("§cCould not invite that player.");
            return true;
        }

        player.sendMessage("§7Invited §d" + target.getName() + " §7to the team.");
        target.sendMessage("§7You were invited to team §d" + team.name());
        target.sendMessage("§7Use §d/team accept §7or §d/team deny");
        return true;
    }

    private boolean handleAccept(Player player) {
        TeamInviteRecord invite = inviteService.getInvite(player.getUniqueId());
        if (invite == null) {
            player.sendMessage("§cYou do not have a pending team invite.");
            return true;
        }

        TeamRecord team = teamService.getTeamById(invite.teamId());
        if (team == null) {
            inviteService.denyInvite(player.getUniqueId());
            player.sendMessage("§cThat team no longer exists.");
            return true;
        }

        if (!inviteService.acceptInvite(player.getUniqueId())) {
            player.sendMessage("§cCould not accept that invite.");
            return true;
        }

        player.sendMessage("§7Joined team §d" + team.name());
        TeamsMainGui.open(core, player, teamService, inviteService);
        return true;
    }

    private boolean handleDeny(Player player) {
        TeamInviteRecord invite = inviteService.getInvite(player.getUniqueId());
        if (invite == null) {
            player.sendMessage("§cYou do not have a pending team invite.");
            return true;
        }

        inviteService.denyInvite(player.getUniqueId());
        player.sendMessage("§cTeam invite denied.");
        return true;
    }

    private boolean handleHome(Player player) {
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

        player.teleport(home);
        player.sendMessage("§7Teleported to §dTeam Home");
        return true;
    }

    private boolean handleSetHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage("§cYou do not have permission to set the team home.");
            return true;
        }

        teamHomeService.setTeamHome(team.teamId(), player.getLocation());
        player.sendMessage("§7Team Home set to your current location");
        return true;
    }

    private boolean handleDeleteHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage("§cYou are not in a team.");
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage("§cYou do not have permission to delete the team home.");
            return true;
        }

        if (!teamHomeService.deleteTeamHome(team.teamId())) {
            player.sendMessage("§cYour team does not have a home set.");
            return true;
        }

        player.sendMessage("§cTeam Home deleted.");
        return true;
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (String option : List.of("create", "invite", "accept", "deny", "leave", "disband", "home", "sethome", "delhome")) {
                if (option.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    completions.add(option);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
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