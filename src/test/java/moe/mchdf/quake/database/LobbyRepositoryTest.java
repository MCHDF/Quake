package moe.mchdf.quake.database;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import moe.mchdf.quake.database.LobbyRepository.Spawn;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LobbyRepositoryTest {
    @TempDir Path directory;

    @Test
    void replacesLobbySpawnAndRetainsItWhenReopened() throws Exception {
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.getConnection()).thenAnswer(ignored ->
                DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db")));

        LobbyRepository repository = new LobbyRepository(database);
        assertNull(repository.load());

        Spawn initial = new Spawn(UUID.randomUUID(), 1.5, 64, 3.5, 90, 0);
        repository.save(initial);
        assertEquals(initial, new LobbyRepository(database).load());

        Spawn updated = new Spawn(UUID.randomUUID(), -10.25, 70.5, 12.75, -45.5f, 20.25f);
        repository.save(updated);
        assertEquals(updated, new LobbyRepository(database).load());
    }
}
