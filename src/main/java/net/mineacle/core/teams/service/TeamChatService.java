package net.mineacle.core.teams.service;

import net.mineacle.core.Core;
import net.mineacle.core.teams.model.TeamRecord;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class TeamChatService {

    private final Core core;
    private final TeamService teamService;
    private final Set<UUID> chatToggled = new HashSet<>();

    public TeamChatService(Core core, TeamService teamService) {
        this.core = core;
        this.teamService = teamService;
    }

    public boolean isToggled(UUID playerId) {
        return chatToggled.contains(playerId);
    }

    public boolean toggle(UUID playerId) {
        if (chatToggled.contains(playerId)) {
            chatToggled.remove(playerId);
            return false;
        }

        chatToggled.add(playerId);
        return true;
    }

    public void clear(UUID playerId) {
        chatToggled.remove(playerId);
    }

    public boolean sendTeamMessage(Player sender, String message) {
        TeamRecord team = teamService.getTeamByPlayer(sender.getUniqueId());
        if (team == null) {
            sender.sendMessage(core.getMessage("teams.chat.not-in-team"));
            return false;
        }

        String teamName = teamService.formatTeamName(team);
        String senderName = sender.getDisplayName() == null || sender.getDisplayName().isBlank()
                ? sender.getName()
                : sender.getDisplayName();

        String rawFormat = core.getMessage("teams.chat.format");
        if (rawFormat == null || rawFormat.equalsIgnoreCase("teams.chat.format")) {
            rawFormat = "&8[%team%&8] &f%player%&8: &f%message%";
        }

        String formatted = ChatColor.translateAlternateColorCodes(
                '&',
                rawFormat
                        .replace("%team%", teamName)
                        .replace("%player%", senderName)
                        .replace("%message%", message)
        );

        for (UUID memberId : teamService.getTeamMembers(team.teamId())) {
            Player target = Bukkit.getPlayer(memberId);
            if (target != null && target.isOnline()) {
                target.sendMessage(formatted);
            }
        }

        return true;
    }
}