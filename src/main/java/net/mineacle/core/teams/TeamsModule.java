package net.mineacle.core.teams;

import net.mineacle.core.Core;
import net.mineacle.core.bootstrap.Module;
import net.mineacle.core.teams.command.TeamCommand;
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

    @Override
    public String name() {
        return "Teams";
    }

    @Override
    public void enable(Core core) {
        this.core = core;
        this.teamService = new TeamService(core);
        this.inviteService = new TeamInviteService(core, teamService);
        this.teamHomeService = new TeamHomeService(core, teamService);

        TeamCommand teamCommand = new TeamCommand(core, teamService, inviteService, teamHomeService);
        registerCommand("team", teamCommand);

        core.getServer().getPluginManager().registerEvents(
                new TeamsGuiListener(core, teamService, inviteService, teamHomeService),
                core
        );
    }

    @Override
    public void disable() {
        if (core != null) {
            core.saveTeamsFile();
        }
    }

    private void registerCommand(String name, TeamCommand executor) {
        PluginCommand command = core.getCommand(name);
        if (command == null) {
            core.getLogger().warning("Missing command in plugin.yml: " + name);
            return;
        }

        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    public TeamService teamService() {
        return teamService;
    }

    public TeamInviteService inviteService() {
        return inviteService;
    }

    public TeamHomeService teamHomeService() {
        return teamHomeService;
    }
}