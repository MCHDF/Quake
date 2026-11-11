package moe.mchdf.quake.game;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import moe.mchdf.quake.menu.MenuBook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameSession {
    private final MenuBook book;
    // During a game, this set contains only the players who joined before it started.
    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> nextGameQueue = new HashSet<>();
    private final GameTeams gameTeams = new GameTeams();
    private final GameDisplay display = new GameDisplay();
    private final GameTimer timer;
    private final GameSettings settings;
    private GameState state = GameState.WAITING;

    public GameSession(JavaPlugin plugin, MenuBook book) {
        this.book = book;
        timer = new GameTimer(plugin);
        settings = new GameSettings(plugin);
        showPlayerCount();
    }

    public GameState getState() {
        return state;
    }

    public boolean isRunning() {
        return state == GameState.RUNNING;
    }

    public boolean contains(Player player) {
        return participants.contains(player.getUniqueId());
    }

    public boolean isQueued(Player player) {
        return nextGameQueue.contains(player.getUniqueId());
    }

    public void join(Player player) {
        if (isRunning()) {
            if (contains(player)) {
                player.sendMessage(Component.text("이미 현재 게임에 참여 중입니다.", NamedTextColor.YELLOW));
                return;
            }
            boolean added = nextGameQueue.add(player.getUniqueId());
            player.sendMessage(Component.text(added
                    ? "다음 게임 대기열에 등록했습니다. 현재 게임 종료 후 자동으로 참여합니다."
                    : "이미 다음 게임 대기열에 등록되어 있습니다.", NamedTextColor.GREEN));
            return;
        }
        boolean added = participants.add(player.getUniqueId());
        if (added) {
            updateAfterJoin();
            display.show(player);
        }
        player.sendMessage(Component.text(added ? "미니게임 참여 대기 명단에 등록했습니다." : "이미 참여 대기 중입니다.", NamedTextColor.GREEN, TextDecoration.BOLD));
    }

    public void setGameDurationSeconds(int seconds) {
        settings.setDurationSeconds(seconds);
    }

    private void updateAfterJoin() {
        if (state != GameState.WAITING) {
            return;
        }
        if (participants.size() >= GameSettings.MIN_PLAYERS) {
            state = GameState.COUNTDOWN;
            timer.start(GameSettings.COUNTDOWN_SECONDS,
                    remaining -> display.countdown(remaining, GameSettings.COUNTDOWN_SECONDS, participants),
                    this::startGame);
        } else {
            showPlayerCount();
        }
    }

    public void requestLeave(Player player) {
        if (isRunning() && contains(player)) {
            player.sendMessage(Component.text("게임 진행 중에는 참여를 취소할 수 없습니다.", NamedTextColor.RED));
            return;
        }
        boolean removed = leave(player);
        player.sendMessage(Component.text(removed ? "미니게임 참여를 취소했습니다." : "참여 대기 중이 아닙니다.", NamedTextColor.YELLOW, TextDecoration.BOLD));
    }

    // Also used for actual disconnections, which must still remove the participant.
    public boolean leave(Player player) {
        if (nextGameQueue.remove(player.getUniqueId())) {
            return true;
        }
        if (!participants.remove(player.getUniqueId())) {
            return false;
        }
        display.hide(player);
        if (state == GameState.RUNNING) {
            gameTeams.remove(player);
            if (participants.size() <= 1 || gameTeams.hasEmptyTeam()) {
                finishGame(true);
            }
            return true;
        }
        // Any participant leaving resets the countdown; it restarts if the minimum is still met.
        cancelCountdown();
        showPlayerCount();
        updateAfterJoin();
        return true;
    }

    private void showPlayerCount() {
        display.waiting(participants.size(), GameSettings.MIN_PLAYERS);
    }

    private void startGame() {
        // Drop participants who went offline without a quit event so team assignment cannot fail.
        participants.removeIf(playerId -> Bukkit.getPlayer(playerId) == null);
        if (participants.size() < GameSettings.MIN_PLAYERS) {
            state = GameState.WAITING;
            showPlayerCount();
            return;
        }
        gameTeams.assign(participants);
        state = GameState.RUNNING;
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.closeInventory();
                book.removeMenuBooks(player);
            }
        }
        // Capture the duration so configuration changes apply only to the next game.
        int duration = settings.durationSeconds();
        timer.start(duration, remaining -> {
            display.running(remaining, duration);
            gameTeams.updateTabListFooter();
        },
                () -> finishGame(false));
    }

    private void finishGame(boolean cancelled) {
        timer.cancel();
        gameTeams.clear();
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                display.hide(player);
                player.sendMessage(Component.text(cancelled
                        ? "참여 인원 부족으로 게임이 취소되었습니다."
                        : "게임이 종료되었습니다.", NamedTextColor.GOLD));
                book.giveMenuBook(player);
                if (cancelled && player.isOnline()) {
                    nextGameQueue.add(playerId);
                }
            }
        }
        participants.clear();
        state = GameState.WAITING;
        showPlayerCount();
        promoteQueue(cancelled);
    }

    private void promoteQueue(boolean previousGameCancelled) {
        // Move all online applicants before deciding whether to start the countdown.
        for (UUID playerId : nextGameQueue) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                participants.add(playerId);
                player.sendMessage(Component.text(previousGameCancelled
                        ? "이전 게임이 인원 부족으로 취소되어 다음 게임에 자동 참여했습니다."
                        : "이전 게임이 종료되어 다음 게임에 자동 참여했습니다.", NamedTextColor.GREEN));
            }
        }
        nextGameQueue.clear();
        updateAfterJoin();
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                display.show(player);
            }
        }
    }

    private void cancelCountdown() {
        timer.cancel();
        state = GameState.WAITING;
    }

    public void shutdown() {
        timer.cancel();
        state = GameState.WAITING;
        gameTeams.clear();
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                display.hide(player);
                book.giveMenuBook(player);
            }
        }
        participants.clear();
        nextGameQueue.clear();
    }
}
