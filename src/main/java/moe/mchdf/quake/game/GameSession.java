package moe.mchdf.quake.game;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class GameSession {
    private static final int MIN_PLAYERS = 2;
    private static final int COUNTDOWN_SECONDS = 30;
    private static final int GAME_SECONDS = 180;
    private final JavaPlugin plugin;
    // During a game, this set contains only the players who joined before it started.
    private final Set<UUID> waitingPlayers = new HashSet<>();
    private final GameTeams gameTeams = new GameTeams();
    private final BossBar waitingBar = BossBar.bossBar(
            Component.text("게임 시작 최소 인원 0 / " + MIN_PLAYERS), 0,
            BossBar.Color.GREEN, BossBar.Overlay.PROGRESS);
    private BukkitTask countdownTask;
    private BukkitTask gameTask;
    private int gameDurationSeconds;
    private int activeGameDurationSeconds;
    private int gameSecondsRemaining;
    // -1 means waiting for participants.
    private int secondsRemaining = -1;

    public GameSession(JavaPlugin plugin) {
        this.plugin = plugin;
        gameDurationSeconds = Math.max(1, plugin.getConfig().getInt("game-timer-seconds", GAME_SECONDS));
    }

    public boolean isRunning() {
        return gameTask != null;
    }

    public boolean contains(Player player) {
        return waitingPlayers.contains(player.getUniqueId());
    }

    public void join(Player player) {
        if (isRunning()) {
            player.sendMessage(Component.text("게임이 진행 중입니다. 종료 후 참여해 주세요.", NamedTextColor.YELLOW));
            return;
        }
        boolean added = waitingPlayers.add(player.getUniqueId());
        if (added) {
            updateAfterJoin();
            player.showBossBar(waitingBar);
        }
        player.sendMessage(Component.text(added ? "미니게임 참여 대기 명단에 등록했습니다." : "이미 참여 대기 중입니다.", NamedTextColor.GREEN, TextDecoration.BOLD));
    }

    public void setGameDurationSeconds(int seconds) {
        if (seconds <= 0) {
            throw new IllegalArgumentException("Game duration must be positive");
        }
        plugin.getConfig().set("game-timer-seconds", seconds);
        plugin.saveConfig();
        gameDurationSeconds = seconds;
    }

    private void updateAfterJoin() {
        if (waitingPlayers.size() >= MIN_PLAYERS && secondsRemaining == -1) {
            secondsRemaining = COUNTDOWN_SECONDS;
            showCountdown();
            countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                secondsRemaining--;
                showCountdown();
                if (secondsRemaining >= 0 && secondsRemaining <= 5) {
                    float pitch = secondsRemaining == 0 ? 2.0f : 1.0f;
                    for (UUID playerId : waitingPlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, pitch);
                        }
                    }
                }
                if (secondsRemaining == 0) {
                    countdownTask.cancel();
                    countdownTask = null;
                    startGame();
                }
            }, 20L, 20L);
        } else if (secondsRemaining == -1) {
            showPlayerCount();
        }
    }

    public boolean leave(Player player) {
        if (!waitingPlayers.remove(player.getUniqueId())) {
            return false;
        }
        player.hideBossBar(waitingBar);
        if (gameTask != null) {
            gameTeams.remove(player);
            if (waitingPlayers.isEmpty()) {
                finishGame();
            }
            return true;
        }
        // Any participant leaving resets the countdown, even if the minimum is still met.
        cancelCountdown();
        showPlayerCount();
        return true;
    }

    private void showPlayerCount() {
        waitingBar.name(Component.text("게임 시작 최소 인원 " + waitingPlayers.size() + " / " + MIN_PLAYERS));
        waitingBar.progress(Math.min(1.0f, waitingPlayers.size() / (float) MIN_PLAYERS));
        waitingBar.color(BossBar.Color.GREEN);
    }

    private void showCountdown() {
        waitingBar.name(Component.text("게임을 시작합니다! | 시작까지 " + secondsRemaining + "초"));
        waitingBar.progress(secondsRemaining / (float) COUNTDOWN_SECONDS);
        waitingBar.color(secondsRemaining <= 10 ? BossBar.Color.RED : BossBar.Color.YELLOW);
    }

    private void startGame() {
        gameTeams.assign(waitingPlayers);
        activeGameDurationSeconds = gameDurationSeconds;
        gameSecondsRemaining = activeGameDurationSeconds;
        showGameTimer();
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            gameSecondsRemaining--;
            showGameTimer();
            if (gameSecondsRemaining == 0) {
                finishGame();
            }
        }, 20L, 20L);
    }

    private void showGameTimer() {
        waitingBar.name(Component.text("게임 진행 중 | 남은 시간 " + gameSecondsRemaining + "초"));
        waitingBar.progress(gameSecondsRemaining / (float) activeGameDurationSeconds);
        waitingBar.color(BossBar.Color.BLUE);
    }

    private void finishGame() {
        gameTask.cancel();
        gameTask = null;
        gameTeams.clear();
        for (UUID playerId : waitingPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.hideBossBar(waitingBar);
                player.sendMessage(Component.text("게임이 종료되었습니다.", NamedTextColor.GOLD));
            }
        }
        waitingPlayers.clear();
        secondsRemaining = -1;
        showPlayerCount();
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        secondsRemaining = -1;
    }

    public void shutdown() {
        cancelCountdown();
        gameTeams.clear();
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        for (UUID playerId : waitingPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.hideBossBar(waitingBar);
            }
        }
        waitingPlayers.clear();
    }
}
