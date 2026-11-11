package moe.mchdf.quake.weapon;

import moe.mchdf.quake.item.RailGunItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class RailGunListener implements Listener {
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (RailGunItem.isRailGun(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (RailGunItem.isRailGun(event.getItem())) {
            event.setCancelled(true);
        }
    }
}

