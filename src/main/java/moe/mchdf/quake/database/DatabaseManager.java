package moe.mchdf.quake.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import org.bukkit.plugin.java.JavaPlugin;

public final class DatabaseManager implements AutoCloseable {
    private final HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            throw new IllegalStateException("Quake 플러그인 데이터 폴더를 생성하지 못했습니다.");
        }
        Path databasePath = plugin.getDataFolder().toPath().resolve("quake.db");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);
        config.setPoolName("QuakeDatabasePool");
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionInitSql("PRAGMA busy_timeout = 5000");
        config.setConnectionTestQuery("SELECT 1");
        dataSource = new HikariDataSource(config);

        try (Connection ignored = dataSource.getConnection()) {
            plugin.getComponentLogger().info("Quake 데이터베이스 연결을 준비했습니다.");
        } catch (SQLException exception) {
            dataSource.close();
            throw new IllegalStateException("Quake 데이터베이스에 연결하지 못했습니다.", exception);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
