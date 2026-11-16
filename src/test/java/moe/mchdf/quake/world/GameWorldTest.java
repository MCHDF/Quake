package moe.mchdf.quake.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import moe.mchdf.quake.database.GameWorldRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class GameWorldTest {
    @Test
    void readinessRequiresBothSpawnsAndValidWorldAndUsesSavedOrder() {
        var repository = mock(GameWorldRepository.class);
        UUID id = UUID.randomUUID();
        var info = new GameWorldInfo("arena", id, "Arena", GameWorldStatus.MAINTENANCE, null, null);
        when(repository.findAll()).thenReturn(List.of(info));
        when(repository.findSpawns()).thenReturn(Map.of());
        when(repository.setStatus("arena", GameWorldStatus.READY)).thenReturn(7L);
        World world = mock(World.class);
        when(world.getUID()).thenReturn(id);
        when(world.getName()).thenReturn("arena");
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("arena")).thenReturn(world);
            GameWorld worlds = new GameWorld(mock(JavaPlugin.class), repository);
            assertTrue(worlds.readyWorlds().isEmpty());
            assertThrows(IllegalArgumentException.class, () -> worlds.setStatus("arena", GameWorldStatus.READY));
            worlds.setSpawn("arena", "RED", new Location(world, 1, 65, 2, 90, 20));
            assertThrows(IllegalArgumentException.class, () -> worlds.setStatus("arena", GameWorldStatus.READY));
            worlds.setSpawn("arena", "BLUE", new Location(world, -1, 66, -2, -90, 0));
            worlds.setStatus("arena", GameWorldStatus.READY);
            assertEquals(7, worlds.readyWorlds().getFirst().rotationOrder());
            assertEquals(new Location(world, 1, 65, 2, 90, 20), worlds.teamSpawn("arena", "RED"));
            assertThrows(IllegalArgumentException.class,
                    () -> worlds.setSpawn("arena", "RED", new Location(world, 2, 65, 2)));
            bukkit.when(() -> Bukkit.getWorld("arena")).thenReturn(null);
            assertTrue(worlds.readyWorlds().isEmpty());
        }
    }

    @Test
    void wrongWorldCannotBeSavedAndReadyWithoutSpawnsIsSkipped() {
        var repository = mock(GameWorldRepository.class);
        var info = new GameWorldInfo("arena", UUID.randomUUID(), "Arena", GameWorldStatus.READY, null, null);
        when(repository.findAll()).thenReturn(List.of(info));
        when(repository.findSpawns()).thenReturn(Map.of());
        try (var bukkit = mockStatic(Bukkit.class)) {
            GameWorld worlds = new GameWorld(mock(JavaPlugin.class), repository);
            assertTrue(worlds.readyWorlds().isEmpty());
            worlds.setStatus("arena", GameWorldStatus.MAINTENANCE);
            World other = mock(World.class);
            when(other.getUID()).thenReturn(UUID.randomUUID());
            assertThrows(IllegalArgumentException.class,
                    () -> worlds.setSpawn("arena", "RED", new Location(other, 0, 65, 0)));
            verify(repository, never()).setSpawn(anyString(), anyString(), any());
        }
    }
}
