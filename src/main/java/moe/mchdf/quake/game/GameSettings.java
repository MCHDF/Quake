package moe.mchdf.quake.game;

import org.bukkit.plugin.java.JavaPlugin;

final class GameSettings {
    static final int MIN_PLAYERS = 2;
    static final int COUNTDOWN_SECONDS = 30;
    private static final int DEFAULT_GAME_SECONDS = 180;
    private static final int DEFAULT_TARGET_SCORE = 5;
    private static final String DURATION_KEY = "game-timer-seconds";
    private static final String TARGET_SCORE_KEY = "game-target-score";
    private final JavaPlugin plugin;
    private int durationSeconds;
    private int targetScore;

    GameSettings(JavaPlugin plugin) {
        this.plugin = plugin;
        durationSeconds = Math.max(1, plugin.getConfig().getInt(DURATION_KEY, DEFAULT_GAME_SECONDS));
        targetScore = Math.max(1, plugin.getConfig().getInt(TARGET_SCORE_KEY, DEFAULT_TARGET_SCORE));
    }

    int durationSeconds() {
        return durationSeconds;
    }

    int targetScore() {
        return targetScore;
    }

    void setDurationSeconds(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Game duration must be positive");
        }
        plugin.getConfig().set(DURATION_KEY, seconds);
        plugin.saveConfig();
        durationSeconds = seconds;
    }

    void setTargetScore(int score) {
        if (score <= 0) {
            throw new IllegalArgumentException("Target score must be positive");
        }
        plugin.getConfig().set(TARGET_SCORE_KEY, score);
        plugin.saveConfig();
        targetScore = score;
    }
}