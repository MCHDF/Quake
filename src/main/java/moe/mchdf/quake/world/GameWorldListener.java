package moe.mchdf.quake.world;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class GameWorldListener implements Listener {
    private final GameWorld gameWorld;

    public GameWorldListener(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (gameWorld.isGameWorld(event.getLocation().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }
}
