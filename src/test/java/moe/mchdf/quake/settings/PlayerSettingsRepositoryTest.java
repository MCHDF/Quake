package moe.mchdf.quake.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import moe.mchdf.quake.database.DatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlayerSettingsRepositoryTest {
    @TempDir Path directory;

    private PlayerSettingsRepository open() throws Exception {
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.getConnection()).thenAnswer(ignored ->
                DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db")));
        return new PlayerSettingsRepository(database);
    }

    @Test
    void playerWithoutSavedSettingsGetsDefaults() throws Exception {
        assertEquals(PlayerSettings.DEFAULTS, open().load(UUID.randomUUID()));
    }

    @Test
    void savedSettingsSurviveReopeningAndStaySeparatePerPlayer() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        PlayerSettings changed = new PlayerSettings(false, false);
        open().save(first, changed);
        open().save(first, changed.withAutoRejoin(true));

        PlayerSettingsRepository reopened = open();
        assertEquals(new PlayerSettings(false, true), reopened.load(first));
        assertEquals(PlayerSettings.DEFAULTS, reopened.load(second));
    }

    @Test
    void unreadableValueFallsBackToDefault() throws Exception {
        UUID player = UUID.randomUUID();
        PlayerSettingsRepository repository = open();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db"));
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO player_settings VALUES ('" + player + "', 'sidebar', 'maybe')");
            statement.executeUpdate("INSERT INTO player_settings VALUES ('" + player + "', 'auto_rejoin', 'false')");
        }
        assertEquals(new PlayerSettings(true, false), repository.load(player));
    }
}
