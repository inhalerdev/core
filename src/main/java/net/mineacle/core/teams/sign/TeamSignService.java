package net.mineacle.core.teams.sign;

import net.kyori.adventure.text.Component;
import net.mineacle.core.Core;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;

public final class TeamSignService {

    private final Core core;

    public TeamSignService(Core core) {
        this.core = core;
    }

    public void openSearchSign(Player player) {
        open(player, TeamSignInputType.MEMBER_SEARCH, "TYPE HERE", "^^^^^^^^^^^^", "Search", "");
    }

    public void openInviteSign(Player player) {
        open(player, TeamSignInputType.INVITE_PLAYER, "TYPE HERE", "^^^^^^^^^^^^", "Invite", "");
    }

    private void open(Player player, TeamSignInputType type, String line1, String line2, String line3, String line4) {
        Location signLocation = player.getLocation().getBlock().getLocation().clone().subtract(0, 4, 0);
        Block block = signLocation.getBlock();

        TeamSignSession.remove(player.getUniqueId());

        TeamSignSession.set(
                player.getUniqueId(),
                new TeamSignSession.Session(
                        type,
                        signLocation.clone(),
                        block.getBlockData().clone()
                )
        );

        block.setType(Material.OAK_SIGN, false);

        if (!(block.getState() instanceof Sign sign)) {
            player.sendMessage("§cCould not open sign input.");
            return;
        }

        sign.getSide(Side.FRONT).line(0, Component.text(line1));
        sign.getSide(Side.FRONT).line(1, Component.text(line2));
        sign.getSide(Side.FRONT).line(2, Component.text(line3));
        sign.getSide(Side.FRONT).line(3, Component.text(line4));
        sign.update(true, false);

        Bukkit.getScheduler().runTask(core, () -> player.openSign(sign, Side.FRONT));
    }

    public void restore(Player player) {
        TeamSignSession.Session session = TeamSignSession.remove(player.getUniqueId());
        if (session == null) {
            return;
        }

        Block block = session.signLocation().getBlock();
        block.setBlockData(session.previousBlockData(), false);
    }
}