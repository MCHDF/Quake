package moe.mchdf.quake.weapon;

import moe.mchdf.quake.item.RailGunItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class RailGunListener implements Listener {
    private final RailGunWeapon weapon;

    public RailGunListener(RailGunWeapon weapon) {
        this.weapon = weapon;
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (RailGunItem.isRailGun(event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!RailGunItem.isRailGun(event.getItem())) {
            return;
        }

        event.setCancelled(true);
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            weapon.fire(event.getPlayer());
        }
    }
}
