package moe.mchdf.quake.database;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import moe.mchdf.quake.world.GameWorldInfo;
import moe.mchdf.quake.world.GameWorldStatus;
import moe.mchdf.quake.world.TeamSpawn;
import java.util.Map;
import java.util.HashMap;

/** Stores world registrations; chunk data remains in the server's world folders. */
public final class GameWorldRepository {
    private final DatabaseManager database;

    public GameWorldRepository(DatabaseManager database) {
        this.database = database;
        try (var connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS game_worlds (name TEXT PRIMARY KEY NOT NULL)");
                var columns = new HashSet<String>();
                try (var rows = statement.executeQuery("PRAGMA table_info(game_worlds)")) {
                    while (rows.next()) {
                        columns.add(rows.getString("name"));
                    }
                }
                if (!columns.contains("world_uuid")) {
                    statement.executeUpdate("ALTER TABLE game_worlds ADD COLUMN world_uuid TEXT");
                }
                if (!columns.contains("display_name")) {
                    statement.executeUpdate("ALTER TABLE game_worlds ADD COLUMN display_name TEXT NOT NULL DEFAULT ''");
                    statement.executeUpdate("UPDATE game_worlds SET display_name = name");
                }
                if (!columns.contains("status")) {
                    statement.executeUpdate("ALTER TABLE game_worlds ADD COLUMN status TEXT NOT NULL DEFAULT 'MAINTENANCE' "
                            + "CHECK (status IN ('READY', 'MAINTENANCE', 'DISABLED'))");
                }
                if (!columns.contains("created_at")) {
                    statement.executeUpdate("ALTER TABLE game_worlds ADD COLUMN created_at TEXT");
                }
                if (!columns.contains("created_by")) {
                    statement.executeUpdate("ALTER TABLE game_worlds ADD COLUMN created_by TEXT");
                }
                if (!columns.contains("min_players")) {
                    statement.executeUpdate("ALTER TABLE game_worlds ADD COLUMN min_players INTEGER NOT NULL DEFAULT 2 "
                            + "CHECK (typeof(min_players) = 'integer' AND min_players IN (2, 4))");
                }
                if (!columns.contains("max_players")) {
                    statement.executeUpdate("ALTER TABLE game_worlds ADD COLUMN max_players INTEGER NOT NULL DEFAULT 16 "
                            + "CHECK (typeof(max_players) = 'integer' AND max_players BETWEEN 2 AND 16 "
                            + "AND max_players % 2 = 0 AND min_players <= max_players)");
                }
                if (!columns.contains("rotation_order")) {
                    statement.executeUpdate("ALTER TABLE game_worlds ADD COLUMN rotation_order INTEGER NOT NULL DEFAULT 0");
                    statement.executeUpdate("UPDATE game_worlds SET rotation_order = "
                            + "(SELECT COUNT(*) FROM game_worlds other WHERE other.name <= game_worlds.name AND other.status = 'READY') "
                            + "WHERE status = 'READY'");
                }
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS world_spawns (
                            world_name TEXT NOT NULL,
                            team TEXT NOT NULL CHECK (team IN ('RED', 'BLUE')),
                            x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                            yaw REAL NOT NULL, pitch REAL NOT NULL,
                            PRIMARY KEY (world_name, team)
                        )
                        """);
                // SQLite cannot alter a column default in place; preserve existing rows and statuses.
                boolean oldStatusDefault = false;
                try (var metadata = statement.executeQuery("PRAGMA table_info(game_worlds)")) {
                    while (metadata.next()) {
                        if (metadata.getString("name").equals("status")) {
                            oldStatusDefault = !"'MAINTENANCE'".equals(metadata.getString("dflt_value"));
                        }
                    }
                }
                if (oldStatusDefault) {
                    statement.executeUpdate("""
                            CREATE TABLE game_worlds_updated (
                                name TEXT PRIMARY KEY NOT NULL, world_uuid TEXT,
                                display_name TEXT NOT NULL DEFAULT '',
                                status TEXT NOT NULL DEFAULT 'MAINTENANCE'
                                    CHECK (status IN ('READY', 'MAINTENANCE', 'DISABLED')),
                                created_at TEXT, created_by TEXT,
                                min_players INTEGER NOT NULL DEFAULT 2
                                    CHECK (typeof(min_players) = 'integer' AND min_players IN (2, 4)),
                                max_players INTEGER NOT NULL DEFAULT 16
                                    CHECK (typeof(max_players) = 'integer' AND max_players BETWEEN 2 AND 16
                                        AND max_players % 2 = 0 AND min_players <= max_players),
                                rotation_order INTEGER NOT NULL DEFAULT 0
                            )
                            """);
                    statement.executeUpdate("""
                            INSERT INTO game_worlds_updated
                                (name, world_uuid, display_name, status, created_at, created_by, min_players, max_players, rotation_order)
                            SELECT name, world_uuid, display_name, status, created_at, created_by, min_players, max_players, rotation_order
                            FROM game_worlds
                            """);
                    statement.executeUpdate("DROP TABLE game_worlds");
                    statement.executeUpdate("ALTER TABLE game_worlds_updated RENAME TO game_worlds");
                }
                statement.executeUpdate("UPDATE game_worlds SET min_players = 4 WHERE min_players > 4");
                statement.executeUpdate("""
                        CREATE TRIGGER IF NOT EXISTS game_worlds_min_players_insert
                        BEFORE INSERT ON game_worlds
                        WHEN NEW.min_players NOT IN (2, 4)
                        BEGIN
                            SELECT RAISE(ABORT, 'min_players must be 2 or 4');
                        END
                        """);
                statement.executeUpdate("""
                        CREATE TRIGGER IF NOT EXISTS game_worlds_min_players_update
                        BEFORE UPDATE OF min_players ON game_worlds
                        WHEN NEW.min_players NOT IN (2, 4)
                        BEGIN
                            SELECT RAISE(ABORT, 'min_players must be 2 or 4');
                        END
                        """);
                statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS game_worlds_uuid ON game_worlds(world_uuid)");
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("게임 월드 테이블을 준비하지 못했습니다.", exception);
        }
    }

    public List<GameWorldInfo> findAll() {
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement("SELECT * FROM game_worlds ORDER BY name");
             var rows = statement.executeQuery()) {
            List<GameWorldInfo> worlds = new ArrayList<>();
            while (rows.next()) {
                String createdAt = rows.getString("created_at");
                worlds.add(new GameWorldInfo(rows.getString("name"), uuid(rows.getString("world_uuid")),
                        rows.getString("display_name"), GameWorldStatus.valueOf(rows.getString("status")),
                        createdAt == null ? null : Instant.parse(createdAt), uuid(rows.getString("created_by")),
                        rows.getInt("min_players"), rows.getInt("max_players"), rows.getLong("rotation_order")));
            }
            return worlds;
        } catch (SQLException exception) {
            throw new IllegalStateException("게임 월드 목록을 읽지 못했습니다.", exception);
        }
    }

    public void add(GameWorldInfo world) {
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO game_worlds (name, world_uuid, display_name, status, created_at, created_by, min_players, max_players, rotation_order)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, world.name());
            statement.setString(2, text(world.worldUuid()));
            statement.setString(3, world.displayName());
            statement.setString(4, world.status().name());
            statement.setString(5, text(world.createdAt()));
            statement.setString(6, text(world.createdBy()));
            statement.setInt(7, world.minPlayers());
            statement.setInt(8, world.maxPlayers());
            statement.setLong(9, world.rotationOrder());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("게임 월드를 등록하지 못했습니다: " + world.name(), exception);
        }
    }

    public long setStatus(String name, GameWorldStatus status) {
        try (var connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("""
                    UPDATE game_worlds SET
                      rotation_order = CASE WHEN ? = 'READY' AND status <> 'READY'
                        THEN (SELECT COALESCE(MAX(rotation_order), 0) + 1 FROM game_worlds)
                        ELSE rotation_order END,
                      status = ? WHERE name = ?
                    """)) {
                statement.setString(1, status.name());
                statement.setString(2, status.name());
                statement.setString(3, name);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Unknown world: " + name);
                }
                long order;
                try (var query = connection.prepareStatement("SELECT rotation_order FROM game_worlds WHERE name = ?")) {
                    query.setString(1, name);
                    try (var rows = query.executeQuery()) {
                        rows.next();
                        order = rows.getLong(1);
                    }
                }
                connection.commit();
                return order;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("월드 상태를 저장하지 못했습니다: " + name, exception);
        }
    }

    public Map<String, Map<String, TeamSpawn>> findSpawns() {
        try (var connection = database.getConnection();
             var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT * FROM world_spawns")) {
            Map<String, Map<String, TeamSpawn>> result = new HashMap<>();
            while (rows.next()) {
                result.computeIfAbsent(rows.getString("world_name"), ignored -> new HashMap<>())
                        .put(rows.getString("team"), new TeamSpawn(rows.getDouble("x"), rows.getDouble("y"),
                                rows.getDouble("z"), rows.getFloat("yaw"), rows.getFloat("pitch")));
            }
            return result;
        } catch (SQLException exception) {
            throw new IllegalStateException("팀 스폰을 읽지 못했습니다.", exception);
        }
    }

    public void setSpawn(String name, String team, TeamSpawn spawn) {
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO world_spawns (world_name, team, x, y, z, yaw, pitch)
                     SELECT name, ?, ?, ?, ?, ?, ? FROM game_worlds WHERE name = ?
                     ON CONFLICT(world_name, team) DO UPDATE SET
                     x=excluded.x, y=excluded.y, z=excluded.z, yaw=excluded.yaw, pitch=excluded.pitch
                     """)) {
            statement.setString(1, team);
            statement.setDouble(2, spawn.x());
            statement.setDouble(3, spawn.y());
            statement.setDouble(4, spawn.z());
            statement.setFloat(5, spawn.yaw());
            statement.setFloat(6, spawn.pitch());
            statement.setString(7, name);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Unknown world: " + name);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("팀 스폰을 저장하지 못했습니다: " + name, exception);
        }
    }

    public void setPlayerLimits(String name, int minPlayers, int maxPlayers) {
        GameWorldInfo.validatePlayerLimits(minPlayers, maxPlayers);
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(
                     "UPDATE game_worlds SET min_players = ?, max_players = ? WHERE name = ?")) {
            statement.setInt(1, minPlayers);
            statement.setInt(2, maxPlayers);
            statement.setString(3, name);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("등록된 게임 월드를 찾을 수 없습니다: " + name);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("게임 월드 인원 설정을 저장하지 못했습니다: " + name, exception);
        }
    }

    public void fillWorldUuid(String name, UUID uuid) {
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(
                     "UPDATE game_worlds SET world_uuid = ? WHERE name = ? AND world_uuid IS NULL")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("게임 월드 UUID를 저장하지 못했습니다: " + name, exception);
        }
    }

    public void remove(String name) {
        try (var connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (var spawns = connection.prepareStatement("DELETE FROM world_spawns WHERE world_name = ?");
                 var world = connection.prepareStatement("DELETE FROM game_worlds WHERE name = ?")) {
                spawns.setString(1, name);
                spawns.executeUpdate();
                world.setString(1, name);
                world.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("게임 월드 등록 정보를 삭제하지 못했습니다: " + name, exception);
        }
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }

    private static String text(Object value) {
        return value == null ? null : value.toString();
    }
}
