package net.mineacle.core.economy;

import net.mineacle.core.Core;
import net.mineacle.core.bootstrap.Module;
import net.mineacle.core.economy.command.BalanceCommand;
import net.mineacle.core.economy.command.EcoCommand;
import net.mineacle.core.economy.command.PayCommand;
import net.mineacle.core.economy.listener.EconomyJoinListener;
import net.mineacle.core.economy.service.EconomyService;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;

public final class EconomyModule extends Module {

    private static EconomyService economyService;

    public static EconomyService economyService() {
        return economyService;
    }

    @Override
    public String name() {
        return "Economy";
    }

    @Override
    public void enable(Core core) {
        economyService = new EconomyService(core);

        BalanceCommand balanceCommand = new BalanceCommand(core, economyService);
        PayCommand payCommand = new PayCommand(core, economyService);
        EcoCommand ecoCommand = new EcoCommand(core, economyService);

        register(core, "bal", balanceCommand);
        register(core, "pay", payCommand);
        register(core, "eco", ecoCommand);

        core.getServer().getPluginManager().registerEvents(
                new EconomyJoinListener(core, economyService),
                core
        );
    }

    @Override
    public void disable() {
        if (economyService != null) {
            economyService.save();
        }

        economyService = null;
    }

    private void register(Core core, String commandName, CommandExecutor executor) {
        PluginCommand command = core.getCommand(commandName);

        if (command == null) {
            core.getLogger().warning("Missing command in plugin.yml: " + commandName);
            return;
        }

        command.setExecutor(executor);

        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }
}