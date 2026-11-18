package moe.mchdf.quake.database;

import java.sql.SQLException;
import java.util.UUID;

public final class LobbyRepository {
    public record Spawn(UUID worldId, double x, double y, double z, float yaw, float pitch) {}

    private final DatabaseManager database;

    public LobbyRepository(DatabaseManager database) {
        this.database = database;
        try (var connection = database.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS lobby_spawn (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        world_uuid TEXT NOT NULL,
                        x REAL NOT NULL, y REAL NOT NULL, z REAL NOT NULL,
                        yaw REAL NOT NULL, pitch REAL NOT NULL
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("로비 설정 테이블을 준비하지 못했습니다.", exception);
        }
    }

    public Spawn load() {
        try (var connection = database.getConnection();
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT world_uuid, x, y, z, yaw, pitch FROM lobby_spawn WHERE id = 1")) {
            return result.next() ? new Spawn(UUID.fromString(result.getString("world_uuid")),
                    result.getDouble("x"), result.getDouble("y"), result.getDouble("z"),
                    result.getFloat("yaw"), result.getFloat("pitch")) : null;
        } catch (SQLException exception) {
            throw new IllegalStateException("로비 설정을 불러오지 못했습니다.", exception);
        }
    }

    public void save(Spawn spawn) {
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO lobby_spawn (id, world_uuid, x, y, z, yaw, pitch)
                     VALUES (1, ?, ?, ?, ?, ?, ?)
                     ON CONFLICT(id) DO UPDATE SET world_uuid = excluded.world_uuid,
                         x = excluded.x, y = excluded.y, z = excluded.z,
                         yaw = excluded.yaw, pitch = excluded.pitch
                     """)) {
            statement.setString(1, spawn.worldId().toString());
            statement.setDouble(2, spawn.x());
            statement.setDouble(3, spawn.y());
            statement.setDouble(4, spawn.z());
            statement.setFloat(5, spawn.yaw());
            statement.setFloat(6, spawn.pitch());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("로비 설정을 저장하지 못했습니다.", exception);
        }
    }
}
