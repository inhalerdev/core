package net.mineacle.core.teams;

import net.mineacle.core.Core;
import net.mineacle.core.bootstrap.Module;
import net.mineacle.core.teams.command.TeamCommand;
import net.mineacle.core.teams.listener.TeamsGuiListener;
import net.mineacle.core.teams.service.TeamBanService;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.command.PluginCommand;

public final class TeamsModule extends Module {

    private Core core;
    private TeamService teamService;
    private TeamBanService banService;
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
        this.banService = new TeamBanService(core);
        this.inviteService = new TeamInviteService(core, teamService, banService);
        this.teamHomeService = new TeamHomeService(core, teamService);

        TeamCommand teamCommand = new TeamCommand(core, teamService, banService, inviteService, teamHomeService);
        registerCommand("team", teamCommand);

        core.getServer().getPluginManager().registerEvents(
                new TeamsGuiListener(core, teamService, banService, inviteService, teamHomeService),
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
}