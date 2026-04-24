package net.mineacle.core.stats;

import net.mineacle.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class StatsCommand implements CommandExecutor {

    private final Core core;
    private final PlayerStatisticsGui playerStatisticsGui;

    public StatsCommand(Core core, PlayerStatisticsGui playerStatisticsGui) {
        this.core = core;
        this.playerStatisticsGui = playerStatisticsGui;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(core.getMessage("general.players-only"));
            return true;
        }

        if (args.length < 1 || args[0].isBlank()) {
            playerStatisticsGui.open(player, player.getUniqueId());
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            target = Bukkit.getOnlinePlayers().stream()
                    .filter(other -> other.getName().equalsIgnoreCase(args[0]))
                    .findFirst()
                    .orElse(null);
        }

        if (target == null) {
            player.sendMessage("§cThat player is not online.");
            return true;
        }

        playerStatisticsGui.open(player, target.getUniqueId());
        return true;
    }
}