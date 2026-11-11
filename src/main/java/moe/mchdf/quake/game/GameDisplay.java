package moe.mchdf.quake.game;

import java.util.Collection;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Owns the shared boss bar and countdown sounds, without changing game state. */
final class GameDisplay {
    private final BossBar bar = BossBar.bossBar(Component.empty(), 0,
            BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);

    void show(Player player) {
        player.showBossBar(bar);
    }

    void hide(Player player) {
        player.hideBossBar(bar);
    }

    void waiting(int count, int minimum) {
        bar.name(Component.text("게임 시작 최소 인원 " + count + " / " + minimum));
        bar.progress(Math.min(1.0f, count / (float) minimum));
        bar.color(BossBar.Color.GREEN);
    }

    void countdown(int remaining, int total, Collection<UUID> participants) {
        bar.name(Component.text("게임을 시작합니다! | 시작까지 " + remaining + "초"));
        bar.progress(remaining / (float) total);
        bar.color(remaining <= 10 ? BossBar.Color.RED : BossBar.Color.YELLOW);
        if (remaining <= 5) {
            float pitch = remaining == 0 ? 2.0f : 1.0f;
            for (UUID playerId : participants) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    player.playSound(player.getLocation(), "minecraft:block.note_block.pling", 1.0f, pitch);
                }
            }
        }
    }

    void running(int remaining, int total) {
        bar.name(Component.text("게임 진행 중 | 남은 시간 " + remaining + "초"));
        bar.progress(remaining / (float) total);
        bar.color(BossBar.Color.BLUE);
    }
}
