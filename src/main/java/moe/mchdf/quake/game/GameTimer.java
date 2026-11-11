package moe.mchdf.quake.game;

import java.util.function.IntConsumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Runs one countdown at a time on the server thread. */
final class GameTimer {
    private final JavaPlugin plugin;
    private BukkitTask task;

    GameTimer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    void start(int durationSeconds, IntConsumer onTick, Runnable onComplete) {
        if (durationSeconds <= 0) {
            throw new IllegalArgumentException("Timer duration must be positive");
        }
        cancel();
        onTick.accept(durationSeconds);
        int[] remaining = {durationSeconds};
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            onTick.accept(--remaining[0]);
            if (remaining[0] == 0) {
                // Cancel the old task before the callback starts the next timer.
                cancel();
                onComplete.run();
            }
        }, 20L, 20L);
    }

    void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
