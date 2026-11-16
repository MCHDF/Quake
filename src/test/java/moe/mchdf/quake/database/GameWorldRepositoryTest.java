package moe.mchdf.quake.database;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import moe.mchdf.quake.world.GameWorldInfo;
import moe.mchdf.quake.world.GameWorldStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameWorldRepositoryTest {
    @TempDir Path directory;

    private GameWorldRepository open() throws Exception {
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.getConnection()).thenAnswer(ignored ->
                DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db")));
        return new GameWorldRepository(database);
    }

    private GameWorldInfo info(String name) {
        return new GameWorldInfo(name, UUID.randomUUID(), "표시 " + name,
                GameWorldStatus.READY, Instant.parse("2026-11-16T00:00:00Z"), UUID.randomUUID());
    }

    private List<String> names(GameWorldRepository repository) {
        return repository.findAll().stream().map(GameWorldInfo::name).toList();
    }

    @Test
    void migratesOldReadyDefaultWithoutChangingExistingStatus() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db"));
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE game_worlds (name TEXT PRIMARY KEY NOT NULL, status TEXT NOT NULL DEFAULT 'READY')");
            statement.executeUpdate("INSERT INTO game_worlds (name) VALUES ('old_arena')");
        }
        GameWorldRepository repository = open();
        assertEquals(GameWorldStatus.READY, repository.findAll().getFirst().status());
        assertEquals(1, repository.findAll().getFirst().rotationOrder());
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db"));
             var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO game_worlds (name) VALUES ('new_arena')");
        }
        assertEquals(GameWorldStatus.MAINTENANCE, open().findAll().getFirst().status());
        assertEquals(GameWorldStatus.READY, open().findAll().getLast().status());
    }

    @Test
    void readinessOrderPersistsAndRepeatedReadyDoesNotMoveWorld() throws Exception {
        GameWorldRepository repository = open();
        repository.add(info("z_first").withStatus(GameWorldStatus.MAINTENANCE, 0));
        repository.add(info("a_second").withStatus(GameWorldStatus.MAINTENANCE, 0));
        long first = repository.setStatus("z_first", GameWorldStatus.READY);
        long second = repository.setStatus("a_second", GameWorldStatus.READY);
        assertTrue(second > first);
        assertEquals(first, repository.setStatus("z_first", GameWorldStatus.READY));
        assertEquals(second, open().findAll().getFirst().rotationOrder());
        repository.setStatus("z_first", GameWorldStatus.MAINTENANCE);
        assertTrue(repository.setStatus("z_first", GameWorldStatus.READY) > second);
    }

    @Test
    void teamSpawnsPersistUpdateAndAreRemovedWithWorld() throws Exception {
        GameWorldRepository repository = open();
        repository.add(info("arena"));
        var red = new moe.mchdf.quake.world.TeamSpawn(1.25, 65, -3.5, 90, 20);
        var blue = new moe.mchdf.quake.world.TeamSpawn(-10, 67, 11, -90, 0);
        repository.setSpawn("arena", "RED", red);
        repository.setSpawn("arena", "BLUE", blue);
        assertEquals(Map.of("RED", red, "BLUE", blue), open().findSpawns().get("arena"));
        repository.setSpawn("arena", "RED", blue);
        assertEquals(blue, open().findSpawns().get("arena").get("RED"));
        assertThrows(IllegalStateException.class, () -> repository.setSpawn("missing", "RED", red));
        assertThrows(IllegalStateException.class, () -> repository.setSpawn("arena", "OTHER", red));
        repository.remove("arena");
        assertTrue(open().findSpawns().isEmpty());
    }

    @Test
    void playerLimitsPersistAndUuidReplacementPreservesThem() throws Exception {
        GameWorldRepository repository = open();
        repository.add(info("arena"));
        assertEquals(2, repository.findAll().getFirst().minPlayers());
        assertEquals(16, repository.findAll().getFirst().maxPlayers());
        repository.setPlayerLimits("arena", 4, 12);
        GameWorldInfo stored = open().findAll().getFirst();
        assertEquals(4, stored.minPlayers());
        assertEquals(12, stored.maxPlayers());
        assertEquals(4, stored.withWorldUuid(UUID.randomUUID()).minPlayers());
        repository.setPlayerLimits("arena", 2, 2);
        repository.setPlayerLimits("arena", 4, 16);
        assertEquals(4, open().findAll().getFirst().minPlayers());
    }

    @Test
    void invalidPlayerLimitsAreRejectedWithoutChangingData() throws Exception {
        GameWorldRepository repository = open();
        GameWorldInfo original = info("arena");
        repository.add(original);
        int[][] invalid = {{0, 16}, {2, 18}, {3, 16}, {2, 15}, {6, 16}, {-2, 16}, {2, 0}};
        for (int[] limits : invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> original.withPlayerLimits(limits[0], limits[1]));
            assertThrows(IllegalArgumentException.class,
                    () -> repository.setPlayerLimits("arena", limits[0], limits[1]));
        }
        assertThrows(IllegalStateException.class, () -> repository.setPlayerLimits("missing", 2, 16));
        assertEquals(List.of(original), open().findAll());
    }

    @Test
    void directSqlCannotBypassPlayerLimitConstraints() throws Exception {
        open().add(info("arena"));
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db"));
             var statement = connection.createStatement()) {
            for (String assignment : List.of("min_players = 3", "max_players = 15", "min_players = 0",
                    "max_players = 18", "min_players = 12, max_players = 4", "min_players = NULL",
                    "min_players = 2.5", "max_players = 'invalid'")) {
                assertThrows(java.sql.SQLException.class,
                        () -> statement.executeUpdate("UPDATE game_worlds SET " + assignment));
            }
            assertThrows(java.sql.SQLException.class,
                    () -> statement.executeUpdate("UPDATE game_worlds SET min_players = 6"));
            statement.executeUpdate("UPDATE game_worlds SET max_players = 4");
        }
    }

    @Test
    void allMetadataSurvivesReopening() throws Exception {
        GameWorldInfo world = new GameWorldInfo("arena", UUID.randomUUID(), "사막 경기장",
                GameWorldStatus.MAINTENANCE, Instant.now(), UUID.randomUUID());
        open().add(world);
        assertEquals(List.of(world), open().findAll());
    }

    @Test
    void migratesLegacyNamesAndFillsUuidWithoutInventingHistory() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("quake.db"));
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE game_worlds (name TEXT PRIMARY KEY NOT NULL)");
            statement.executeUpdate("INSERT INTO game_worlds VALUES ('legacy')");
        }
        GameWorldRepository repository = open();
        assertEquals(List.of(new GameWorldInfo("legacy", null, "legacy", GameWorldStatus.MAINTENANCE, null, null)),
                repository.findAll());
        UUID uuid = UUID.randomUUID();
        repository.fillWorldUuid("legacy", uuid);
        repository.fillWorldUuid("legacy", UUID.randomUUID());
        assertEquals(List.of(new GameWorldInfo("legacy", uuid, "legacy", GameWorldStatus.MAINTENANCE, null, null)),
                open().findAll());
    }

    @Test
    void duplicateWorldUuidIsRejected() throws Exception {
        GameWorldInfo world = info("arena");
        GameWorldRepository repository = open();
        repository.add(world);
        GameWorldInfo alias = new GameWorldInfo("alias", world.worldUuid(), "Alias",
                GameWorldStatus.DISABLED, world.createdAt(), world.createdBy());
        assertThrows(IllegalStateException.class, () -> repository.add(alias));
        assertEquals(List.of(world), open().findAll());
    }

    @Test
    void registrationsSurviveReopeningAndDeletion() throws Exception {
        GameWorldRepository repository = open();
        assertEquals(List.of(), repository.findAll());
        repository.add(info("arena_b"));
        repository.add(info("arena_a"));
        GameWorldRepository reopened = open();
        assertEquals(List.of("arena_a", "arena_b"), names(reopened));
        reopened.remove("arena_a");
        assertEquals(List.of("arena_b"), names(open()));
    }

    @Test
    void duplicateRegistrationDoesNotOverwriteExistingWorld() throws Exception {
        GameWorldRepository repository = open();
        repository.add(info("arena"));
        assertThrows(IllegalStateException.class, () -> repository.add(info("arena")));
        assertEquals(List.of("arena"), names(open()));
    }

    @Test
    void namesAreBoundAsValues() throws Exception {
        GameWorldRepository repository = open();
        String name = "arena'); DROP TABLE game_worlds; --";
        repository.add(info(name));
        repository.add(info("safe"));
        repository.remove(name);
        assertEquals(List.of("safe"), names(open()));
    }
}
