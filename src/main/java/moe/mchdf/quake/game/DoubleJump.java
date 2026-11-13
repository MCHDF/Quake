package moe.mchdf.quake.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

public final class DoubleJump implements Listener {
    private static final double DASH_POWER = 0.9;
    private static final double MINIMUM_VERTICAL_DIRECTION = 0.25;

    private final Map<UUID, Boolean> previousAllowFlight = new HashMap<>();
    private final Map<UUID, Boolean> available = new HashMap<>();

    public void enable(Player player) {
        UUID playerId = player.getUniqueId();
        previousAllowFlight.putIfAbsent(playerId, player.getAllowFlight());
        available.put(playerId, true);
        player.setAllowFlight(true);
        player.setFlying(false);
    }

    public void disable(Player player) {
        UUID playerId = player.getUniqueId();
        Boolean allowFlight = previousAllowFlight.remove(playerId);
        available.remove(playerId);
        if (allowFlight != null) {
            player.setFlying(false);
            player.setAllowFlight(allowFlight);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (!previousAllowFlight.containsKey(playerId)) {
            return;
        }

        event.setCancelled(true);
        if (player.isOnGround()) {
            available.put(playerId, true);
            return;
        }
        if (!available.getOrDefault(playerId, false)) {
            return;
        }

        available.put(playerId, false);
        Vector direction = player.getEyeLocation().getDirection().normalize();
        direction.setY(Math.max(direction.getY(), MINIMUM_VERTICAL_DIRECTION));
        player.setVelocity(direction.normalize().multiply(DASH_POWER));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (previousAllowFlight.containsKey(playerId) && player.isOnGround()) {
            available.put(playerId, true);
        }
    }
}
