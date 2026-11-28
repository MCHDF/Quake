package moe.mchdf.quake.settings;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import moe.mchdf.quake.database.DatabaseManager;

/** 플레이어 설정을 {@code player_settings} 테이블에 UUID·항목별 한 줄로 저장한다. */
public final class PlayerSettingsRepository {
    private final DatabaseManager database;

    public PlayerSettingsRepository(DatabaseManager database) {
        this.database = database;
        try (var connection = database.getConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_settings (
                        player_uuid TEXT NOT NULL,
                        setting_key TEXT NOT NULL,
                        value TEXT NOT NULL,
                        PRIMARY KEY (player_uuid, setting_key)
                    )
                    """);
        } catch (SQLException exception) {
            throw new IllegalStateException("플레이어 설정 테이블을 준비하지 못했습니다.", exception);
        }
    }

    public PlayerSettings load(UUID player) {
        try (var connection = database.getConnection();
             var statement = connection.prepareStatement(
                     "SELECT setting_key, value FROM player_settings WHERE player_uuid = ?")) {
            statement.setString(1, player.toString());
            Map<String, String> values = new HashMap<>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    values.put(rows.getString("setting_key"), rows.getString("value"));
                }
            }
            return PlayerSettings.fromValues(values);
        } catch (SQLException exception) {
            throw new IllegalStateException("플레이어 설정을 읽지 못했습니다: " + player, exception);
        }
    }

    public void save(UUID player, PlayerSettings settings) {
        try (var connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("""
                    INSERT INTO player_settings (player_uuid, setting_key, value) VALUES (?, ?, ?)
                    ON CONFLICT(player_uuid, setting_key) DO UPDATE SET value = excluded.value
                    """)) {
                for (var entry : settings.values().entrySet()) {
                    statement.setString(1, player.toString());
                    statement.setString(2, entry.getKey());
                    statement.setString(3, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("플레이어 설정을 저장하지 못했습니다: " + player, exception);
        }
    }
}
