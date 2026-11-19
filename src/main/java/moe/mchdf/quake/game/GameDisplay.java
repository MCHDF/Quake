package moe.mchdf.quake.game;

import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Owns the boss bar, countdown sounds, and result titles. */
final class GameDisplay {
    private static final Title.Times RESULT_TIMES =
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500));
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

    void rotating(String worldName, int count, int minimum, int maximum, int remaining) {
        bar.name(Component.text(worldName + " | 참여 " + count + "명 (최소 " + minimum
                + ", 최대 " + maximum + ") | 다음 월드까지 " + remaining + "초"));
        bar.progress(Math.min(1.0f, count / (float) minimum));
        bar.color(BossBar.Color.GREEN);
    }

    void noWorld() {
        bar.name(Component.text("준비된 게임 월드를 기다리고 있습니다."));
        bar.progress(0);
        bar.color(BossBar.Color.GREEN);
    }

    void countdown(String worldName, int remaining, int total, Collection<UUID> participants) {
        bar.name(Component.text(worldName + " | 게임을 시작합니다! | 시작까지 " + remaining + "초"));
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

    void running(int remaining, int total, int redScore, int blueScore, int targetScore) {
        String time = String.format(Locale.ROOT, "%02d:%02d", remaining / 60, remaining % 60);
        bar.name(teamScores(redScore, blueScore)
                .append(Component.text("  |  목표 " + targetScore + "점  |  " + time)));
        bar.progress(remaining / (float) total);
        bar.color(BossBar.Color.BLUE);
    }

    void showResult(Player player, String team, int redScore, int blueScore) {
        int teamScore = team.equals("RED") ? redScore : blueScore;
        int opponentScore = team.equals("RED") ? blueScore : redScore;
        Component result = switch (Integer.compare(teamScore, opponentScore)) {
            case 1 -> Component.text("승리", NamedTextColor.GREEN, TextDecoration.BOLD);
            case -1 -> Component.text("패배", NamedTextColor.RED, TextDecoration.BOLD);
            default -> Component.text("무승부", NamedTextColor.GOLD, TextDecoration.BOLD);
        };
        Component subtitle = Component.text("최종 점수  |  ").append(teamScores(redScore, blueScore));
        player.showTitle(Title.title(result, subtitle, RESULT_TIMES));
    }

    private Component teamScores(int redScore, int blueScore) {
        return Component.text("RED " + redScore, NamedTextColor.RED)
                .append(Component.text("  |  ", NamedTextColor.WHITE))
                .append(Component.text("BLUE " + blueScore, NamedTextColor.BLUE));
    }
}
