package net.mineacle.core.teams.model;

import org.bukkit.Material;

public enum TeamBannerColor {
    WHITE("White", Material.WHITE_BANNER, Material.WHITE_DYE),
    GRAY("Gray", Material.GRAY_BANNER, Material.GRAY_DYE),
    LIGHT_GRAY("Light Gray", Material.LIGHT_GRAY_BANNER, Material.LIGHT_GRAY_DYE),
    RED("Red", Material.RED_BANNER, Material.RED_DYE),
    ORANGE("Orange", Material.ORANGE_BANNER, Material.ORANGE_DYE),
    YELLOW("Yellow", Material.YELLOW_BANNER, Material.YELLOW_DYE),
    LIME("Lime", Material.LIME_BANNER, Material.LIME_DYE),
    GREEN("Green", Material.GREEN_BANNER, Material.GREEN_DYE),
    CYAN("Cyan", Material.CYAN_BANNER, Material.CYAN_DYE),
    LIGHT_BLUE("Light Blue", Material.LIGHT_BLUE_BANNER, Material.LIGHT_BLUE_DYE),
    BLUE("Blue", Material.BLUE_BANNER, Material.BLUE_DYE),
    PURPLE("Purple", Material.PURPLE_BANNER, Material.PURPLE_DYE),
    MAGENTA("Magenta", Material.MAGENTA_BANNER, Material.MAGENTA_DYE),
    PINK("Pink", Material.PINK_BANNER, Material.PINK_DYE);

    private final String displayName;
    private final Material bannerMaterial;
    private final Material dyeMaterial;

    TeamBannerColor(String displayName, Material bannerMaterial, Material dyeMaterial) {
        this.displayName = displayName;
        this.bannerMaterial = bannerMaterial;
        this.dyeMaterial = dyeMaterial;
    }

    public String displayName() {
        return displayName;
    }

    public Material bannerMaterial() {
        return bannerMaterial;
    }

    public Material dyeMaterial() {
        return dyeMaterial;
    }

    public static TeamBannerColor fromString(String value) {
        if (value == null || value.isBlank()) {
            return PURPLE;
        }

        try {
            return TeamBannerColor.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return PURPLE;
        }
    }
}