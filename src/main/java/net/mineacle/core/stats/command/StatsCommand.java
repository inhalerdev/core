package net.mineacle.core.stats.command;

import net.mineacle.core.stats.PlayerStatisticsGui;
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

public final class StatsCommand implements CommandExecutor, TabCompleter {

    private final PlayerStatisticsGui playerStatisticsGui;

    public StatsCommand(PlayerStatisticsGui playerStatisticsGui) {
        this.playerStatisticsGui = playerStatisticsGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player viewer)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!viewer.hasPermission("mineaclestats.use")) {
            viewer.sendMessage("§cYou do not have permission.");
            return true;
        }

        if (args.length < 1) {
            playerStatisticsGui.open(viewer, viewer.getUniqueId());
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (target.getName() == null && !target.hasPlayedBefore()) {
            viewer.sendMessage("§cThat player could not be found.");
            return true;
        }

        playerStatisticsGui.open(viewer, target.getUniqueId());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        if (args.length != 1) {
            return completions;
        }

        String partial = args[0].toLowerCase(Locale.ROOT);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                completions.add(online.getName());
            }
        }

        return completions;
    }
}