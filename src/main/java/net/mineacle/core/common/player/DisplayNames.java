package net.mineacle.core.common.player;

import me.clip.placeholderapi.PlaceholderAPI;
import net.mineacle.core.chat.ChatModule;
import net.mineacle.core.chat.service.NicknameService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public final class DisplayNames {

    private DisplayNames() {
    }

    public static String username(OfflinePlayer player) {
        if (player == null) {
            return "";
        }

        String name = player.getName();
        return name == null || name.isBlank() ? player.getUniqueId().toString() : name;
    }

    public static String displayName(OfflinePlayer player) {
        NicknameService service = ChatModule.nicknameService();

        if (service != null) {
            return service.displayName(player);
        }

        return username(player);
    }

    public static String prefixedDisplayName(OfflinePlayer player) {
        return luckPermsPrefix(player) + displayName(player);
    }

    public static String luckPermsPrefix(OfflinePlayer player) {
        if (player == null) {
            return "";
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return "";
        }

        try {
            String parsed = PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");

            if (parsed == null || parsed.isBlank() || parsed.equalsIgnoreCase("%luckperms_prefix%")) {
                return "";
            }

            return parsed;
        } catch (Throwable ignored) {
            return "";
        }
    }
}