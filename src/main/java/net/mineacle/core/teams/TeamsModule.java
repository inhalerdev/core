package net.mineacle.core.teams;

import net.mineacle.core.Core;
import net.mineacle.core.bootstrap.Module;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.teams.command.TeamCommand;
import net.mineacle.core.teams.listener.TeamChatListener;
import net.mineacle.core.teams.listener.TeamsGuiListener;
import net.mineacle.core.teams.service.TeamBanService;
import net.mineacle.core.teams.service.TeamChatService;
import net.mineacle.core.teams.service.TeamHomeService;
import net.mineacle.core.teams.service.TeamInviteService;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

public final class TeamsModule extends Module {

    private Core core;
    private TeamService teamService;
    private TeamBanService banService;
    private TeamInviteService inviteService;
    private TeamHomeService teamHomeService;
    private TeamChatService teamChatService;
    private TeleportService teleportService;

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
        this.teamChatService = new TeamChatService(core, teamService);
        this.teleportService = new TeleportService(core);

        TeamCommand teamCommand = new TeamCommand(
                core,
                teamService,
                banService,
                inviteService,
                teamHomeService,
                teamChatService,
                teleportService
        );

        registerCommand("team", teamCommand, teamCommand);
        registerCommand("teamchat", teamCommand, teamCommand);

        core.getServer().getPluginManager().registerEvents(
                new TeamsGuiListener(core, teamService, banService, inviteService, teamHomeService, teleportService),
                core
        );

        core.getServer().getPluginManager().registerEvents(
                new TeamChatListener(teamChatService),
                core
        );
    }

    @Override
    public void disable() {
        if (core != null) {
            core.saveTeamsFile();
        }
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = core.getCommand(name);
        if (command == null) {
            core.getLogger().warning("Missing command in plugin.yml: " + name);
            return;
        }

        command.setExecutor(executor);
        command.setTabCompleter(completer);
    }
}