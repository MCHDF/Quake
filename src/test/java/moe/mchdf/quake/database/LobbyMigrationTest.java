package moe.mchdf.quake.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Instant;
import java.util.UUID;
import moe.mchdf.quake.world.LobbyWorldInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LobbyMigrationTest {
    @TempDir Path directory;

    @Test
    void preservesLegacySpawnAcrossRestartAndDoesNotRestoreItAfterReset() throws Exception {
        var database = mock(DatabaseManager.class);
        when(database.getConnection()).thenAnswer(ignored ->
                DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db")));
        var worldId = UUID.randomUUID();
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE lobby_spawn (
                        id INTEGER PRIMARY KEY, world_uuid TEXT NOT NULL,
                        x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                        yaw REAL NOT NULL, pitch REAL NOT NULL
                    )
                    """);
            try (var insert = connection.prepareStatement(
                    "INSERT INTO lobby_spawn VALUES (1, ?, -10.25, 70.5, 12.75, -45.5, 20.25)")) {
                insert.setString(1, worldId.toString());
                insert.executeUpdate();
            }
        }

        var repository = new GameWorldRepository(database);
        var migrated = repository.findLobbyWorld().orElseThrow();
        assertEquals(worldId, migrated.worldUuid());
        assertEquals(new LobbyWorldInfo.Spawn(-10.25, 70.5, 12.75, -45.5f, 20.25f), migrated.spawn());
        assertEquals(migrated, new GameWorldRepository(database).findLobbyWorld().orElseThrow());

        var moved = new LobbyWorldInfo("new_hub", UUID.randomUUID(), Instant.now(), UUID.randomUUID(),
                new LobbyWorldInfo.Spawn(2.5, 63, 5.25, 135, -20));
        repository.setLobbyWorld(moved);
        assertEquals(moved, new GameWorldRepository(database).findLobbyWorld().orElseThrow());

        repository.clearLobbyWorld();
        assertTrue(new GameWorldRepository(database).findLobbyWorld().isEmpty());
    }
}
