package net.mineacle.core.stats;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;

public final class VaultStatsHook {

    private VaultStatsHook() {
    }

    public static String balance(OfflinePlayer player) {
        if (player == null) {
            return "Unavailable";
        }

        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object economy = Bukkit.getServicesManager().load(economyClass);

            if (economy == null) {
                return "Unavailable";
            }

            Method getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            Object result = getBalance.invoke(economy, player);

            if (!(result instanceof Number number)) {
                return "Unavailable";
            }

            return "$" + String.format("%,.2f", number.doubleValue());
        } catch (Throwable ignored) {
            return "Unavailable";
        }
    }
}