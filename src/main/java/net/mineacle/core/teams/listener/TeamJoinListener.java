package net.mineacle.core.teams.listener;

import net.kyori.adventure.text.Component;
import net.mineacle.core.Core;
import net.mineacle.core.teams.service.TeamService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class TeamJoinListener implements Listener {

    private final Core core;
    private final TeamService teamService;

    public TeamJoinListener(Core core, TeamService teamService) {
        this.core = core;
        this.teamService = teamService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!teamService.isTeamChatEnabled(player.getUniqueId())) {
            return;
        }

        core.getServer().getScheduler().runTaskLater(core, () -> {
            if (!player.isOnline()) {
                return;
            }

            String message = "§7Team chat enabled";
            player.sendMessage(message);
            player.sendActionBar(Component.text(message));
        }, 20L);
    }
}