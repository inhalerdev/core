package net.mineacle.core.teams.model;

import org.bukkit.Material;

public enum TeamNameColor {
    WHITE("White", "&f", Material.WHITE_DYE),
    RED("Red", "&c", Material.RED_DYE),
    GOLD("Gold", "&6", Material.ORANGE_DYE),
    YELLOW("Yellow", "&e", Material.YELLOW_DYE),
    GREEN("Green", "&a", Material.LIME_DYE),
    AQUA("Aqua", "&b", Material.CYAN_DYE),
    BLUE("Blue", "&9", Material.BLUE_DYE),
    LIGHT_PURPLE("Light Purple", "&d", Material.MAGENTA_DYE),
    DARK_PURPLE("Dark Purple", "&5", Material.PURPLE_DYE);

    private final String displayName;
    private final String colorCode;
    private final Material material;

    TeamNameColor(String displayName, String colorCode, Material material) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.material = material;
    }

    public String displayName() {
        return displayName;
    }

    public String colorCode() {
        return colorCode;
    }

    public Material material() {
        return material;
    }

    public static TeamNameColor fromString(String value) {
        if (value == null || value.isBlank()) {
            return WHITE;
        }

        for (TeamNameColor color : values()) {
            if (color.colorCode.equalsIgnoreCase(value) || color.name().equalsIgnoreCase(value)) {
                return color;
            }
        }

        return WHITE;
    }
}