package moe.mchdf.quake.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import moe.mchdf.quake.database.GameWorldRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;

class LobbyWorldManagerTest {
    @Test
    void matchesConfiguredWorldByUuidAndFailedSavePreservesConfiguration() {
        var repository = mock(GameWorldRepository.class);
        when(repository.findLobbyWorld()).thenReturn(java.util.Optional.empty());
        var manager = new LobbyWorldManager(repository);
        World world = mock(World.class);
        UUID id = UUID.randomUUID();
        UUID editor = UUID.randomUUID();
        when(world.getName()).thenReturn("clear");
        when(world.getUID()).thenReturn(id);

        var location = new Location(world, 1.25, 65.5, -2.75, 45, -10);
        manager.setLobbyWorld(location, editor);
        LobbyWorldInfo saved = manager.lobbyWorldInfo().orElseThrow();
        assertEquals("clear", saved.name());
        assertEquals(id, saved.worldUuid());
        assertEquals(editor, saved.updatedBy());
        assertEquals(new LobbyWorldInfo.Spawn(1.25, 65.5, -2.75, 45, -10), saved.spawn());
        verify(repository).setLobbyWorld(saved);

        doThrow(new IllegalStateException("save failed")).when(repository).setLobbyWorld(any());
        assertThrows(IllegalStateException.class, () -> manager.setLobbyWorld(location, editor));
        assertEquals(saved, manager.lobbyWorldInfo().orElseThrow());

        when(world.getUID()).thenReturn(UUID.randomUUID());
        assertFalse(manager.isConfiguredWorld(world));
        assertEquals(saved, manager.lobbyWorldInfo().orElseThrow());

        when(world.getUID()).thenReturn(id);
        assertTrue(manager.isConfiguredWorld(world));
    }

    @Test
    void lobbyWorldUsesConfiguredUuidAndCanBeCleared() {
        var repository = mock(GameWorldRepository.class);
        UUID configuredUuid = UUID.randomUUID();
        LobbyWorldInfo lobby = new LobbyWorldInfo("hub", configuredUuid, Instant.now(), UUID.randomUUID());
        when(repository.findLobbyWorld()).thenReturn(java.util.Optional.of(lobby));

        World matching = mock(World.class);
        when(matching.getName()).thenReturn("hub");
        when(matching.getUID()).thenReturn(configuredUuid);
        var defaultSpawn = new Location(matching, 0, 65, 0);
        when(matching.getSpawnLocation()).thenReturn(defaultSpawn);

        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld(configuredUuid)).thenReturn(matching);
            LobbyWorldManager worlds = new LobbyWorldManager(repository);
            assertTrue(worlds.lobbyWorld().isPresent());
            assertEquals("hub", worlds.lobbyWorldInfo().orElseThrow().name());
            assertEquals(defaultSpawn, worlds.spawn());

            bukkit.when(() -> Bukkit.getWorld(configuredUuid)).thenReturn(null);
            assertTrue(worlds.lobbyWorld().isEmpty());

            worlds.clearLobbyWorld();
            assertTrue(worlds.lobbyWorldInfo().isEmpty());
            verify(repository).clearLobbyWorld();
        }
    }
}
