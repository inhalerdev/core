package net.mineacle.core.tpa.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.mineacle.core.Core;
import net.mineacle.core.common.gui.MenuHistory;
import net.mineacle.core.common.player.DisplayNames;
import net.mineacle.core.common.text.TextColor;
import net.mineacle.core.homes.service.TeleportService;
import net.mineacle.core.tpa.gui.TpaRequestGui;
import net.mineacle.core.tpa.service.TpaRequest;
import net.mineacle.core.tpa.service.TpaRequestType;
import net.mineacle.core.tpa.service.TpaService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TpaCommand implements CommandExecutor, TabCompleter {

    private final Core core;
    private final TpaService tpaService;
    private final TeleportService teleportService;

    public TpaCommand(Core core, TpaService tpaService, TeleportService teleportService) {
        this.core = core;
        this.tpaService = tpaService;
        this.teleportService = teleportService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("mineacletpa.use")) {
            player.sendMessage(TextColor.color("&cYou do not have permission."));
            return true;
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);

        return switch (commandName) {
            case "tpa" -> handleTpa(player, args, TpaRequestType.TO_TARGET);
            case "tpahere" -> handleTpa(player, args, TpaRequestType.HERE);
            case "tpaccept" -> handleAccept(player);
            case "tpdeny" -> handleDeny(player);
            default -> true;
        };
    }

    private boolean handleTpa(Player requester, String[] args, TpaRequestType type) {
        if (args.length < 1) {
            requester.sendMessage(type == TpaRequestType.TO_TARGET
                    ? TextColor.color("&cUsage: /tpa <player>")
                    : TextColor.color("&cUsage: /tpahere <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            requester.sendMessage(TextColor.color("&cThat player is not online."));
            return true;
        }

        if (target.getUniqueId().equals(requester.getUniqueId())) {
            requester.sendMessage(TextColor.color("&cYou cannot send a teleport request to yourself."));
            return true;
        }

        if (!tpaService.createRequest(requester, target, type)) {
            requester.sendMessage(TextColor.color("&cCould not send teleport request."));
            return true;
        }

        String requesterName = DisplayNames.prefixedDisplayName(requester);
        String targetName = DisplayNames.prefixedDisplayName(target);

        requester.sendMessage(TextColor.color("&aTeleport request sent to " + targetName + "&a."));

        if (type == TpaRequestType.TO_TARGET) {
            target.sendActionBar(Component.text(TextColor.color(requesterName + " &#bbbbbbwants to teleport to you.")));
            target.sendMessage(TextColor.color(requesterName + " &#bbbbbbwants to teleport to you."));
        } else {
            target.sendActionBar(Component.text(TextColor.color(requesterName + " &#bbbbbbwants you to teleport to them.")));
            target.sendMessage(TextColor.color(requesterName + " &#bbbbbbwants you to teleport to them."));
        }

        target.sendMessage(Component.text(TextColor.color("&#bbbbbbType &d/tpaccept &#bbbbbbor click to respond."))
                .clickEvent(ClickEvent.runCommand("/tpaccept")));

        core.getServer().getScheduler().runTaskLater(core, () -> {
            TpaRequest request = tpaService.getRequest(target.getUniqueId());

            if (request == null) {
                return;
            }

            if (!request.requesterId().equals(requester.getUniqueId())) {
                return;
            }

            tpaService.removeRequest(target.getUniqueId());

            if (requester.isOnline()) {
                requester.sendMessage(TextColor.color("&cTeleport request to " + targetName + " expired."));
            }

            if (target.isOnline()) {
                target.sendMessage(TextColor.color("&cTeleport request expired."));
            }
        }, tpaService.timeoutSeconds() * 20L);

        return true;
    }

    private boolean handleAccept(Player player) {
        TpaRequest request = tpaService.getRequest(player.getUniqueId());

        if (request == null) {
            player.sendMessage(TextColor.color("&cYou have no pending teleport requests."));
            return true;
        }

        MenuHistory.openRoot(core, player, () -> TpaRequestGui.open(core, player, request));
        return true;
    }

    private boolean handleDeny(Player player) {
        TpaRequest request = tpaService.removeRequest(player.getUniqueId());

        if (request == null) {
            player.sendMessage(TextColor.color("&cYou have no pending teleport requests."));
            return true;
        }

        Player requester = tpaService.requester(request);

        player.sendMessage(TextColor.color("&cTeleport request denied."));

        if (requester != null && requester.isOnline()) {
            requester.sendMessage(TextColor.color("&c" + DisplayNames.prefixedDisplayName(player) + " denied your teleport request."));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) {
            return completions;
        }

        String commandName = command.getName().toLowerCase(Locale.ROOT);

        if ((commandName.equals("tpa") || commandName.equals("tpahere")) && args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getUniqueId().equals(player.getUniqueId())) {
                    continue;
                }

                if (online.getName().toLowerCase(Locale.ROOT).startsWith(partial)) {
                    completions.add(online.getName());
                }
            }
        }

        return completions;
    }
}