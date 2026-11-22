package moe.mchdf.quake.world;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import moe.mchdf.quake.database.GameWorldRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;

public final class LobbyWorldManager {
    private final GameWorldRepository repository;
    private LobbyWorldInfo lobbyWorld;

    public LobbyWorldManager(GameWorldRepository repository) {
        this.repository = repository;
        this.lobbyWorld = repository.findLobbyWorld().orElse(null);
    }

    public Optional<LobbyWorldInfo> lobbyWorldInfo() {
        return Optional.ofNullable(lobbyWorld);
    }

    public Optional<World> lobbyWorld() {
        if (lobbyWorld == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(Bukkit.getWorld(lobbyWorld.worldUuid()));
    }

    public Location spawn() {
        World world = lobbyWorld().orElse(null);
        if (world == null) {
            World overworld = Bukkit.getWorld(NamespacedKey.minecraft("overworld"));
            return overworld == null ? null : overworld.getSpawnLocation();
        }
        var spawn = lobbyWorld.spawn();
        return spawn == null ? world.getSpawnLocation()
                : new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
    }

    public void setLobbyWorld(Location location, UUID updatedBy) {
        World world = location.getWorld();
        var spawn = new LobbyWorldInfo.Spawn(location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        LobbyWorldInfo updated = new LobbyWorldInfo(world.getName(), world.getUID(), Instant.now(), updatedBy, spawn);
        repository.setLobbyWorld(updated);
        lobbyWorld = updated;
    }

    public void clearLobbyWorld() {
        repository.clearLobbyWorld();
        lobbyWorld = null;
    }

    public boolean isConfiguredWorld(World world) {
        return lobbyWorld != null && lobbyWorld.worldUuid().equals(world.getUID());
    }
}
