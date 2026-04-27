package net.mineacle.core;

import net.mineacle.core.bootstrap.Module;
import net.mineacle.core.common.gui.MenuCloseListener;
import net.mineacle.core.homes.HomesModule;
import net.mineacle.core.teams.TeamsModule;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.mineacle.core.tpa.TpaModule;
import net.mineacle.core.stats.StatsModule;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Core extends JavaPlugin {

    private static Core instance;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    private File homesFile;
    private FileConfiguration homesConfig;

    private File teamsFile;
    private FileConfiguration teamsConfig;

    private final List<Module> modules = new ArrayList<>();

    public static Core instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadMessagesFile();
        loadHomesFile();
        loadTeamsFile();

        getServer().getPluginManager().registerEvents(new MenuCloseListener(this), this);

        try {
            registerModule(new HomesModule());
            registerModule(new TeamsModule());
            registerModule(new TpaModule());
            registerModule(new HomesModule());
            registerModule(new TeamsModule());
            registerModule(new TpaModule());
            registerModule(new StatsModule());
            getLogger().info("MineacleCore enabled successfully.");
        } catch (Exception exception) {
            getLogger().severe("Failed to enable MineacleCore: " + exception.getMessage());
            exception.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        disableModules();
        saveHomesFile();
        saveTeamsFile();
        instance = null;
    }

    public void registerModule(Module module) throws Exception {
        module.enable(this);
        modules.add(module);
        getLogger().info("Enabled module: " + module.name());
    }

    private void disableModules() {
        for (int i = modules.size() - 1; i >= 0; i--) {
            Module module = modules.get(i);

            try {
                module.disable();
                getLogger().info("Disabled module: " + module.name());
            } catch (Exception exception) {
                getLogger().warning("Failed to disable module " + module.name() + ": " + exception.getMessage());
            }
        }

        modules.clear();
    }

    public void reloadCoreFiles() {
        reloadConfig();
        loadMessagesFile();
        loadHomesFile();
        loadTeamsFile();
    }

    private void loadMessagesFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadHomesFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        homesFile = new File(getDataFolder(), "homes.yml");
        if (!homesFile.exists()) {
            saveResource("homes.yml", false);
        }

        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void loadTeamsFile() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        teamsFile = new File(getDataFolder(), "teams.yml");
        if (!teamsFile.exists()) {
            saveResource("teams.yml", false);
        }

        teamsConfig = YamlConfiguration.loadConfiguration(teamsFile);
    }

    public void saveHomesFile() {
        if (homesFile == null || homesConfig == null) {
            return;
        }

        try {
            homesConfig.save(homesFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save homes.yml");
            exception.printStackTrace();
        }
    }

    public void saveTeamsFile() {
        if (teamsFile == null || teamsConfig == null) {
            return;
        }

        try {
            teamsConfig.save(teamsFile);
        } catch (IOException exception) {
            getLogger().severe("Could not save teams.yml");
            exception.printStackTrace();
        }
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getHomesConfig() {
        return homesConfig;
    }

    public FileConfiguration getTeamsConfig() {
        return teamsConfig;
    }

    public String getMessage(String path) {
        String value = messagesConfig.getString(path, "&cMissing message: " + path);
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    public List<Module> modules() {
        return Collections.unmodifiableList(modules);
    }
}