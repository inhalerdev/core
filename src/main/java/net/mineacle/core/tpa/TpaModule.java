package net.mineacle.core.tpa;

import net.mineacle.core.Core;
import net.mineacle.core.bootstrap.Module;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.tpa.command.TpaCommand;
import net.mineacle.core.tpa.listener.TpaGuiListener;
import net.mineacle.core.tpa.service.TpaService;
import org.bukkit.command.PluginCommand;

public final class TpaModule extends Module {

    private TpaService tpaService;
    private TeleportService teleportService;

    @Override
    public String name() {
        return "TPA";
    }

    @Override
    public void enable(Core core) {
        this.tpaService = new TpaService(core);
        this.teleportService = new TeleportService(core);

        TpaCommand command = new TpaCommand(core, tpaService, teleportService);

        registerCommand(core, "tpa", command);
        registerCommand(core, "tpahere", command);
        registerCommand(core, "tpaccept", command);
        registerCommand(core, "tpdeny", command);

        core.getServer().getPluginManager().registerEvents(
                new TpaGuiListener(core, tpaService, teleportService),
                core
        );
    }

    @Override
    public void disable() {
    }

    private void registerCommand(Core core, String name, TpaCommand command) {
        PluginCommand pluginCommand = core.getCommand(name);

        if (pluginCommand == null) {
            core.getLogger().warning("Missing command in plugin.yml: " + name);
            return;
        }

        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }
}