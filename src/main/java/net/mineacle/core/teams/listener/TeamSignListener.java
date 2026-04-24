package net.mineacle.core.teams.listener;

import net.mineacle.core.Core;
import net.mineacle.core.teams.gui.TeamGuiSession;
import net.mineacle.core.teams.gui.TeamsMainGui;
import net.mineacle.core.teams.model.TeamRecord;
import net.mineacle.core.teams.service.TeamBanService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import net.mineacle.core.teams.sign.TeamSignInputType;
import net.mineacle.core.teams.sign.TeamSignService;
import net.mineacle.core.teams.sign.TeamSignSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TeamSignListener implements Listener {

    private final Core core;
    private final TeamService teamService;
    private final TeamBanService banService;
    private final TeamInviteService inviteService;
    private final TeamSignService signService;

    public TeamSignListener(
            Core core,
            TeamService teamService,
            TeamBanService banService,
            TeamInviteService inviteService,
            TeamSignService signService
    ) {
        this.core = core;
        this.teamService = teamService;
        this.banService = banService;
        this.inviteService = inviteService;
        this.signService = signService;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();

        TeamSignSession.Session session = TeamSignSession.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        if (!event.getBlock().getLocation().equals(session.signLocation())) {
            return;
        }

        event.setCancelled(true);

        String input = firstNonEmptyLine(event.getLine(0), event.getLine(1), event.getLine(2), event.getLine(3));
        signService.restore(player);

        Bukkit.getScheduler().runTask(core, () -> {
            if (session.type() == TeamSignInputType.MEMBER_SEARCH) {
                handleMemberSearch(player, input);
                return;
            }

            if (session.type() == TeamSignInputType.INVITE_PLAYER) {
                handleInvitePlayer(player, input);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        signService.restore(event.getPlayer());
    }

    private void handleMemberSearch(Player player, String input) {
        if (input.isBlank()) {
            TeamGuiSession.setMemberSearch(player.getUniqueId(), "");
        } else {
            TeamGuiSession.setMemberSearch(player.getUniqueId(), input);
        }

        TeamGuiSession.setPage(player.getUniqueId(), 0);
        TeamsMainGui.open(core, player, teamService, inviteService);
    }

    private void handleInvitePlayer(Player player, String input) {
        TeamRecord team = teamService.getTeamByPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(core.getMessage("teams.no-team"));
            return;
        }

        if (!teamService.isAdmin(player.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.invite.no-permission"));
            TeamsMainGui.open(core, player, teamService, inviteService);
            return;
        }

        if (input.isBlank()) {
            TeamsMainGui.open(core, player, teamService, inviteService);
            return;
        }

        Player target = Bukkit.getPlayerExact(input);
        if (target == null) {
            player.sendMessage(core.getMessage("teams.invite.offline"));
            TeamsMainGui.open(core, player, teamService, inviteService);
            return;
        }

        if (teamService.hasTeam(target.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.invite.target-in-team"));
            TeamsMainGui.open(core, player, teamService, inviteService);
            return;
        }

        if (banService.isBanned(team.teamId(), target.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.invite.banned-target"));
            TeamsMainGui.open(core, player, teamService, inviteService);
            return;
        }

        if (!inviteService.createInvite(team.teamId(), player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(core.getMessage("teams.invite.failed"));
            TeamsMainGui.open(core, player, teamService, inviteService);
            return;
        }

        player.sendMessage(core.getMessage("teams.invite.sent").replace("%player%", target.getName()));
        target.sendMessage(core.getMessage("teams.invite.received-1").replace("%team%", team.name()));

        net.kyori.adventure.text.Component accept = net.kyori.adventure.text.Component.text("§a[ACCEPT]")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/team accept"));
        net.kyori.adventure.text.Component spacer = net.kyori.adventure.text.Component.text(" §7or ");
        net.kyori.adventure.text.Component deny = net.kyori.adventure.text.Component.text("§c[DENY]")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/team deny"));
        target.sendMessage(accept.append(spacer).append(deny));

        TeamsMainGui.open(core, player, teamService, inviteService);
    }

    private String firstNonEmptyLine(String... lines) {
        for (String line : lines) {
            if (line != null && !line.trim().isBlank()) {
                return line.trim();
            }
        }
        return "";
    }
}