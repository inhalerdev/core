package net.mineacle.core.teams.listener;

import net.mineacle.core.teams.service.TeamChatService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

@SuppressWarnings("deprecation")
public final class TeamChatListener implements Listener {

    private final TeamChatService teamChatService;

    public TeamChatListener(TeamChatService teamChatService) {
        this.teamChatService = teamChatService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (!teamChatService.isToggled(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        teamChatService.sendTeamMessage(player, event.getMessage());
    }
}