package net.mineacle.core.teams;

import net.mineacle.core.Core;
import net.mineacle.core.bootstrap.Module;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.teams.command.TeamCommand;
import net.mineacle.core.teams.listener.TeamCombatListener;
import net.mineacle.core.teams.listener.TeamsGuiListener;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.command.PluginCommand;

public final class TeamsModule extends Module {

    private Core core;
    private TeamService teamService;
    private TeamInviteService inviteService;
    private TeamHomeService teamHomeService;
    private TeleportService teleportService;

    @Override
    public String name() {
        return "Teams";
    }

    @Override
    public void enable(Core core) {
        this.core = core;

        this.teamService = new TeamService(core);
        this.inviteService = new TeamInviteService(teamService);
        this.teamHomeService = new TeamHomeService(core, teamService);
        this.teleportService = new TeleportService(core);

        TeamCommand command = new TeamCommand(
                core,
                teamService,
                inviteService,
                teamHomeService,
                teleportService
        );

        PluginCommand team = core.getCommand("team");
        if (team != null) {
            team.setExecutor(command);
            team.setTabCompleter(command);
        } else {
            core.getLogger().warning("Missing command in plugin.yml: team");
        }

        core.getServer().getPluginManager().registerEvents(
                new TeamsGuiListener(core, teamService, inviteService, teamHomeService, teleportService),
                core
        );

        core.getServer().getPluginManager().registerEvents(
                new TeamCombatListener(teamService),
                core
        );
    }

    @Override
    public void disable() {
        if (core != null) {
            core.saveTeamsFile();
        }
    }
}