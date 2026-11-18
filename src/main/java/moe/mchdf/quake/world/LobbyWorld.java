package moe.mchdf.quake.world;

import java.util.List;
import moe.mchdf.quake.database.LobbyRepository;
import moe.mchdf.quake.database.LobbyRepository.Spawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class LobbyWorld {
    private final LobbyRepository repository;
    private Spawn configuredSpawn;

    public LobbyWorld(LobbyRepository repository) {
        this.repository = repository;
        configuredSpawn = repository.load();
    }

    public Location spawn() {
        if (configuredSpawn != null) {
            World world = Bukkit.getWorld(configuredSpawn.worldId());
            if (world != null) {
                return new Location(world, configuredSpawn.x(), configuredSpawn.y(), configuredSpawn.z(),
                        configuredSpawn.yaw(), configuredSpawn.pitch());
            }
        }
        World overworld = Bukkit.getWorld(NamespacedKey.minecraft("overworld"));
        return overworld == null ? null : overworld.getSpawnLocation();
    }

    public int setSpawn(Location location) {
        Location previousSpawn = spawn();
        List<Player> previousPlayers = previousSpawn == null
                ? List.of() : List.copyOf(previousSpawn.getWorld().getPlayers());
        Spawn updated = new Spawn(location.getWorld().getUID(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        repository.save(updated);
        configuredSpawn = updated;

        int failed = 0;
        for (Player player : previousPlayers) {
            if (!player.teleport(location)) {
                failed++;
            }
        }
        return failed;
    }
}
