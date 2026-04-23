package net.mineacle.core.homes.gui;

import net.mineacle.core.Core;
import net.mineacle.core.homes.service.HomeService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public final class HomesMainGui {

    public static final int[] BED_SLOTS = {12, 13, 14, 15, 16};
    public static final int[] DYE_SLOTS = {21, 22, 23, 24, 25};

    private HomesMainGui() {
    }

    public static void open(Core core, Player player, HomeService homeService) {
        String title = ChatColor.translateAlternateColorCodes('&', core.getMessage("homes.gui.title"));
        Inventory inventory = Bukkit.createInventory(null, 9 * 4, title);

        int maxHomes = homeService.getMaxHomes(player);
        UUID uuid = player.getUniqueId();

        for (int i = 0; i < 5; i++) {
            int id = i + 1;
            int bedSlot = BED_SLOTS[i];
            int dyeSlot = DYE_SLOTS[i];
            String displayName = homeService.getDisplayName(uuid, id);

            if (maxHomes >= id) {
                boolean exists = homeService.exists(uuid, id);

                if (exists) {
                    inventory.setItem(
                            bedSlot,
                            item(
                                    Material.PURPLE_BED,
                                    "&d" + displayName,
                                    List.of("&fClick to &dteleport &fto this home")
                            )
                    );
                    inventory.setItem(
                            dyeSlot,
                            item(
                                    Material.PURPLE_DYE,
                                    "&d" + displayName,
                                    List.of("&fClick to &cdelete &fthis home")
                            )
                    );
                } else {
                    inventory.setItem(
                            bedSlot,
                            item(
                                    Material.WHITE_BED,
                                    "&f" + displayName,
                                    List.of("&7Click to save this location")
                            )
                    );
                    inventory.setItem(
                            dyeSlot,
                            item(
                                    Material.GRAY_DYE,
                                    "&f" + displayName,
                                    List.of("&7Click to save this location")
                            )
                    );
                }
            } else {
                inventory.setItem(
                        bedSlot,
                        item(
                                Material.LIGHT_GRAY_BED,
                                "&cHome Locked",
                                List.of("&dMineacle+&f required to use this feature.")
                        )
                );
                inventory.setItem(
                        dyeSlot,
                        item(
                                Material.GRAY_DYE,
                                "&cHome Locked",
                                List.of("&dMineacle+&f required to use this feature.")
                        )
                );
            }
        }

        setupTeamHomePlaceholder(core, inventory);
        player.openInventory(inventory);
    }

    private static void setupTeamHomePlaceholder(Core core, Inventory inventory) {
        int bannerSlot = core.getConfig().getInt("homes.team-home.banner-slot", 10);
        int dyeSlot = core.getConfig().getInt("homes.team-home.dye-slot", 19);
        boolean teamsEnabled = core.getConfig().getBoolean("teams.enabled", false);

        if (!core.getConfig().getBoolean("homes.team-home.enabled", true)) {
            return;
        }

        if (!teamsEnabled) {
            inventory.setItem(
                    bannerSlot,
                    item(
                            Material.LIGHT_GRAY_BANNER,
                            core.getMessage("teams.gui.placeholder-title"),
                            List.of(
                                    core.getMessage("teams.gui.placeholder-lore-1"),
                                    core.getMessage("teams.gui.placeholder-lore-2")
                            )
                    )
            );

            inventory.setItem(
                    dyeSlot,
                    item(
                            Material.GRAY_DYE,
                            core.getMessage("teams.gui.placeholder-title"),
                            List.of(
                                    core.getMessage("teams.gui.placeholder-lore-1"),
                                    core.getMessage("teams.gui.placeholder-lore-2")
                            )
                    )
            );
            return;
        }

        inventory.setItem(
                bannerSlot,
                item(
                        Material.LIGHT_GRAY_BANNER,
                        core.getMessage("teams.gui.no-team-title"),
                        List.of(
                                core.getMessage("teams.gui.no-team-lore-1"),
                                core.getMessage("teams.gui.no-team-lore-2")
                        )
                )
        );

        inventory.setItem(
                dyeSlot,
                item(
                        Material.GRAY_DYE,
                        core.getMessage("teams.gui.no-team-title"),
                        List.of(
                                core.getMessage("teams.gui.no-team-lore-1"),
                                core.getMessage("teams.gui.no-team-lore-2")
                        )
                )
        );
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(lore.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());

        item.setItemMeta(meta);
        return item;
    }
}