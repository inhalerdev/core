package net.mineacle.core.teams.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.mineacle.core.Core;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.teams.gui.TeamBansGui;
import net.mineacle.core.teams.gui.TeamGuiSession;
import net.mineacle.core.teams.gui.TeamInviteGui;
import net.mineacle.core.teams.gui.TeamManageGui;
import net.mineacle.core.teams.gui.TeamsMainGui;
import net.mineacle.core.teams.model.TeamInviteRecord;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamBanService;
import net.mineacle.core.teams.service.TeamChatService;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
    private final TeamBanService banService;
    private final TeamInviteService inviteService;
    private final TeamHomeService teamHomeService;
    private final TeamChatService teamChatService;
    private final TeleportService teleportService;

    public TeamCommand(
            Core core,
            TeamService teamService,
            TeamBanService banService,
            TeamInviteService inviteService,
            TeamHomeService teamHomeService,
            TeamChatService teamChatService,
            TeleportService teleportService
    ) {
        this.core = core;
        this.teamService = teamService;
        this.banService = banService;
        this.inviteService = inviteService;
        this.teamHomeService = teamHomeService;
        this.teamChatService = teamChatService;
        this.teleportService = teleportService;
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

        String commandName = command.getName().toLowerCase(Locale.ROOT);

        if (commandName.equals("teamchat")) {
            return handleDirectTeamChat(player, args);
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
            case "invites":
                return handleInvites(player);
            case "home":
                return handleHome(player);
            case "sethome":
                return handleSetHome(player);
            case "delhome":
                return handleDeleteHome(player);
            case "bans":
                return handleBans(player);
            case "unban":
                return handleUnban(player, args);
            case "chat":
                return handleTeamChatSubcommand(player, args);
            case "manage":
                return handleManage(player);
            default:
                player.sendMessage("§cUnknown subcommand.");
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!canCreateOrJoinTeam(player, "create")) {
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§cUsage: /team create <name>");
            return true;
        }

        String teamName = joinArgs(args, 1);

        if (teamName.contains("&") || teamName.contains("§") || teamName.contains("#")) {
            player.sendMessage(core.getMessage("teams.no-color-codes-in-name"));
            player.sendMessage(core.getMessage("teams.manage-colors-instead"));
            return true;
        }

        if (!teamService.isValidTeamName(teamName)) {
            player.sendMessage(core.getMessage("teams.invalid-name"));
            return true;
        }

        if (teamService.hasTeam(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.already-in-team"));
            return true;
        }

        if (teamService.getTeamByName(teamName) != null) {
            player.sendMessage(core.getMessage("teams.taken-name"));
            return true;
        }

        if (!teamService.createTeam(player.getUniqueId(), teamName)) {
            player.sendMessage("§cCould not create that team.");
            return true;
        }

        TeamRecord created = teamService.getTeamByPlayer(player.getUniqueId());
        String teamDisplay = created == null ? teamName : teamService.formatTeamName(created);
        player.sendMessage(core.getMessage("teams.created").replace("%team%", teamDisplay));
        TeamsMainGui.open(core, player, teamService, inviteService);
        return true;
    }

    private boolean handleLeave(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        if (teamService.isFounder(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.founders-cannot-leave"));
            return true;
        }

        teamService.removeMember(player.getUniqueId());
        teamChatService.clear(player.getUniqueId());
        TeamGuiSession.clear(player.getUniqueId());
        player.sendMessage(core.getMessage("teams.left-team"));
        return true;
    }

    private boolean handleDisband(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        if (!teamService.isFounder(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.only-founder-disband"));
            return true;
        }

        for (UUID memberId : teamService.getTeamMembers(team.teamId())) {
            teamChatService.clear(memberId);
            TeamGuiSession.clear(memberId);
        }

        teamService.disbandTeam(team.teamId());
        player.sendMessage(core.getMessage("teams.disbanded"));
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.invite.no-permission"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(core.getMessage("teams.invite.usage"));
            return true;
        }

        if (teamService.getTeamMembers(team.teamId()).size() >= TeamsMainGui.TEAM_SIZE_LIMIT) {
            player.sendMessage("§cYour team is full.");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(core.getMessage("teams.invite.offline"));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§cYou cannot invite yourself.");
            return true;
        }

        if (teamService.hasTeam(target.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.invite.target-in-team"));
            return true;
        }

        if (banService.isBanned(team.teamId(), target.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.invite.banned-target"));
            return true;
        }

        if (!inviteService.createInvite(team.teamId(), player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.invite.failed"));
            return true;
        }

        player.sendMessage(core.getMessage("teams.invite.sent").replace("%player%", target.getName()));
        sendInviteNotification(target, player, team);
        return true;
    }

    private void sendInviteNotification(Player target, Player inviter, TeamRecord team) {
        String teamName = teamService.formatTeamName(team);

        target.sendActionBar(Component.text("§dTeam invite received from §f" + inviter.getName()));

        target.sendMessage(" ");
        target.sendMessage(Component.text("§7You were invited to join ").append(Component.text(teamName)));
        target.sendMessage(Component.text("§7Invited by §d" + inviter.getName()));

        Component open = Component.text("§a[VIEW INVITE]")
                .clickEvent(ClickEvent.runCommand("/team invites"));

        target.sendMessage(open);
        target.sendMessage(" ");
    }

    private boolean handleInvites(Player player) {
        if (!inviteService.hasInvite(player.getUniqueId())) {
            String message = "§cYou have no current team invites.";
            player.sendActionBar(Component.text(message));
            player.sendMessage(message);
            return true;
        }

        TeamInviteGui.open(core, player, inviteService, teamService);
        return true;
    }

    private boolean handleAccept(Player player) {
        if (!canCreateOrJoinTeam(player, "join")) {
            return true;
        }

        TeamInviteRecord invite = inviteService.getInvite(player.getUniqueId());
        if (invite == null) {
            player.sendMessage(core.getMessage("teams.invite.none"));
            return true;
        }

        TeamRecord team = teamService.getTeamById(invite.teamId());
        if (team == null) {
            inviteService.denyInvite(player.getUniqueId());
            player.sendMessage(core.getMessage("teams.invite.team-gone"));
            return true;
        }

        if (teamService.getTeamMembers(team.teamId()).size() >= TeamsMainGui.TEAM_SIZE_LIMIT) {
            inviteService.denyInvite(player.getUniqueId());
            player.sendMessage("§cThat team is full.");
            return true;
        }

        if (banService.isBanned(invite.teamId(), player.getUniqueId())) {
            inviteService.denyInvite(player.getUniqueId());
            player.sendMessage(core.getMessage("teams.invite.self-banned"));
            return true;
        }

        if (!inviteService.acceptInvite(player.getUniqueId())) {
            player.sendMessage("§cCould not accept that invite.");
            return true;
        }

        TeamRecord joined = teamService.getTeamByPlayer(player.getUniqueId());
        player.sendMessage(core.getMessage("teams.invite.accepted").replace("%team%", joined == null ? team.name() : teamService.formatTeamName(joined)));
        TeamsMainGui.open(core, player, teamService, inviteService);
        return true;
    }

    private boolean handleDeny(Player player) {
        TeamInviteRecord invite = inviteService.getInvite(player.getUniqueId());
        if (invite == null) {
            player.sendMessage(core.getMessage("teams.invite.none"));
            return true;
        }

        inviteService.denyInvite(player.getUniqueId());
        player.sendMessage(core.getMessage("teams.invite.denied"));
        return true;
    }

    private boolean handleHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        org.bukkit.Location home = teamHomeService.getTeamHome(team.teamId());
        if (home == null) {
            player.sendMessage(core.getMessage("teams.home.no-home"));
            return true;
        }

        teleportService.begin(player, "Team Home", () -> {
            player.teleport(home);
            player.sendMessage(core.getMessage("teams.home.teleported"));
        });
        return true;
    }

    private boolean handleSetHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.home.no-set-permission"));
            return true;
        }

        teamHomeService.setTeamHome(team.teamId(), player.getLocation());
        player.sendMessage(core.getMessage("teams.home.set"));
        return true;
    }

    private boolean handleDeleteHome(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.home.no-delete-permission"));
            return true;
        }

        if (!teamHomeService.deleteTeamHome(team.teamId())) {
            player.sendMessage(core.getMessage("teams.home.no-home"));
            return true;
        }

        player.sendMessage(core.getMessage("teams.home.deleted"));
        return true;
    }

    private boolean handleBans(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.bans.no-permission"));
            return true;
        }

        TeamBansGui.open(core, player, team.teamId(), banService, teamService);
        return true;
    }

    private boolean handleUnban(Player player, String[] args) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.bans.no-permission"));
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(core.getMessage("teams.bans.usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!banService.isBanned(team.teamId(), target.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.bans.not-banned"));
            return true;
        }

        banService.clearBan(team.teamId(), target.getUniqueId());
        player.sendMessage(core.getMessage("teams.bans.unbanned"));
        return true;
    }

    private boolean handleTeamChatSubcommand(Player player, String[] args) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.chat.not-in-team"));
            return true;
        }

        if (args.length == 1) {
            boolean enabled = teamChatService.toggle(player.getUniqueId());
            player.sendMessage(enabled ? core.getMessage("teams.chat.enabled") : core.getMessage("teams.chat.disabled"));
            return true;
        }

        String message = joinArgs(args, 1);
        if (message.isBlank()) {
            player.sendMessage(core.getMessage("teams.chat.usage"));
            return true;
        }

        teamChatService.sendTeamMessage(player, message);
        return true;
    }

    private boolean handleDirectTeamChat(Player player, String[] args) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.chat.not-in-team"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§cUsage: /teamchat <message>");
            return true;
        }

        String message = joinArgs(args, 0);
        if (message.isBlank()) {
            player.sendMessage("§cUsage: /teamchat <message>");
            return true;
        }

        teamChatService.sendTeamMessage(player, message);
        return true;
    }

    private boolean handleManage(Player player) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return true;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.management.no-permission"));
            return true;
        }

        TeamManageGui.open(core, player, teamService);
        return true;
    }

    private boolean canCreateOrJoinTeam(Player player, String action) {
        boolean required = core.getConfig().getBoolean("teams.require-discord-link", false);
        if (!required) {
            return true;
        }

        if (player.hasPermission("mineacle.discord.linked")) {
            return true;
        }

        if (action.equalsIgnoreCase("join")) {
            player.sendActionBar(Component.text("§cLink Discord with /link to join a team"));
            player.sendMessage("§cYou must link your Discord to join a team.");
        } else {
            player.sendActionBar(Component.text("§cLink Discord with /link to create a team"));
            player.sendMessage("§cYou must link your Discord to create a team.");
        }

        player.sendMessage("§7Type §d/link §7to link your Discord and in-game profile.");
        return false;
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
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();

        if (commandName.equals("teamchat")) {
            return completions;
        }

        if (args.length == 1) {
            for (String option : List.of("create", "invite", "accept", "deny", "invites", "leave", "disband", "home", "sethome", "delhome", "bans", "unban", "chat", "manage")) {
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