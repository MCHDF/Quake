package moe.mchdf.quake.game;

import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import moe.mchdf.quake.world.GameWorld;
import moe.mchdf.quake.world.GameWorldInfo;
import moe.mchdf.quake.world.LobbyArrival;
import org.bukkit.Location;
import org.bukkit.World;
import java.util.Set;
import java.util.UUID;
import moe.mchdf.quake.item.RailGunItem;
import moe.mchdf.quake.menu.MenuBook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameSession {
    private static final int ROTATION_WAIT_SECONDS = 30;
    private static final float WALK_SPEED_MULTIPLIER = 2.0F;
    private final MenuBook book;
    private final GameWorld worlds;
    private final LobbyArrival lobbyArrival;
    private GameWorldInfo currentWorld;
    private final WorldRotation worldRotation = new WorldRotation();
    private int waitingRemaining;
    private int gameRemaining;
    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Map<UUID, Float> previousWalkSpeeds = new HashMap<>();
    // During a game, this set contains only the players who joined before it started.
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> nextGameQueue = new LinkedHashSet<>();
    private final GameTeams gameTeams = new GameTeams();
    private final GameDisplay display = new GameDisplay();
    private final DoubleJump doubleJump = new DoubleJump();
    private final GameTimer timer;
    private final GameSettings settings;
    private GameState state = GameState.WAITING;

    public GameSession(JavaPlugin plugin, MenuBook book, GameWorld worlds, LobbyArrival lobbyArrival) {
        this.lobbyArrival = lobbyArrival;
        this.worlds = worlds;
        this.book = book;
        timer = new GameTimer(plugin);
        settings = new GameSettings(plugin);
        rotateWorld();
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

    public boolean hasReadyWorlds() {
        return !worlds.readyWorlds().isEmpty();
    }

    public void join(Player player) {
        if (!hasReadyWorlds()) {
            player.sendMessage(Component.text("로테이션에 준비된 게임 맵이 없어 참여할 수 없습니다.", NamedTextColor.RED));
            return;
        }
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

    public GameWorldInfo getCurrentWorld() {
        return currentWorld;
    }

    private void rotateWorld() {
        timer.cancel();
        state = GameState.WAITING;
        currentWorld = worldRotation.next(worlds.readyWorlds(), currentWorld);
        beginWaiting();
    }

    public void onWorldSettingsChanged() {
        if (isRunning()) {
            return;
        }
        if (!refreshCurrentWorld()) {
            rotateWorld();
        } else if (state == GameState.COUNTDOWN && participants.size() < currentWorld.minPlayers()) {
            timer.cancel();
            beginWaiting();
        } else {
            updateAfterJoin();
        }
    }

    private boolean refreshCurrentWorld() {
        if (currentWorld == null) {
            return false;
        }
        GameWorldInfo fresh = worlds.readyWorlds().stream()
                .filter(info -> WorldRotation.sameRotationEntry(info, currentWorld))
                .findFirst().orElse(null);
        if (fresh == null) {
            return false;
        }
        currentWorld = fresh;
        return true;
    }

    private void beginWaiting() {
        state = GameState.WAITING;
        waitingRemaining = ROTATION_WAIT_SECONDS;
        // This is distinct from the existing game-start countdown.
        timer.start(ROTATION_WAIT_SECONDS, remaining -> {
            waitingRemaining = remaining;
            showPlayerCount();
        }, this::rotateWorld);
        updateAfterJoin();
    }

    private void updateAfterJoin() {
        if (state != GameState.WAITING) {
            return;
        }
        participants.removeIf(id -> Bukkit.getPlayer(id) == null);
        if (currentWorld == null) {
            if (!worlds.readyWorlds().isEmpty()) {
                rotateWorld();
            }
            return;
        }
        if (!refreshCurrentWorld()) {
            rotateWorld();
            return;
        }
        if (participants.size() >= currentWorld.minPlayers()) {
            state = GameState.COUNTDOWN;
            timer.start(GameSettings.COUNTDOWN_SECONDS,
                    remaining -> display.countdown(currentWorld.displayName(), remaining, GameSettings.COUNTDOWN_SECONDS, participants),
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
        doubleJump.disable(player);
        restoreWalkSpeed(player);
        display.hide(player);
        if (state == GameState.RUNNING) {
            gameTeams.remove(player);
            if (participants.size() <= 1 || gameTeams.hasEmptyTeam()) {
                finishGame(true);
            }
            return true;
        }
        // Any participant leaving resets the countdown; it restarts if the minimum is still met.
        if (state == GameState.COUNTDOWN) {
            timer.cancel();
            beginWaiting();
        } else {
            showPlayerCount();
        }
        return true;
    }

    private void showPlayerCount() {
        if (currentWorld == null) {
            display.noWorld();
        } else {
            display.rotating(currentWorld.displayName(), participants.size(), currentWorld.minPlayers(),
                    currentWorld.maxPlayers(), waitingRemaining);
        }
    }

    private void startGame() {
        // Drop participants who went offline without a quit event so team assignment cannot fail.
        participants.removeIf(playerId -> Bukkit.getPlayer(playerId) == null);
        if (!refreshCurrentWorld()) {
            rotateWorld();
            return;
        }
        if (participants.size() < currentWorld.minPlayers()) {
            beginWaiting();
            return;
        }
        List<UUID> selected = participants.stream().limit(currentWorld.maxPlayers()).toList();
        // Assign teams before choosing each player's spawn, but restore scoreboards if a teleport fails.
        gameTeams.assign(selected);
        for (UUID id : selected) {
            Player player = Bukkit.getPlayer(id);
            returnLocations.put(id, player.getLocation());
            if (!player.teleport(worlds.teamSpawn(currentWorld.name(), gameTeams.getTeamOf(player)))) {
                gameTeams.clear();
                returnPlayers();
                for (UUID applicant : participants) {
                    Player waiting = Bukkit.getPlayer(applicant);
                    if (waiting != null) {
                        waiting.sendMessage(Component.text("월드 이동에 실패하여 다음 월드를 선택합니다.", NamedTextColor.RED));
                    }
                }
                rotateWorld();
                return;
            }
        }
        // Preserve application order; overflow waits for the next game.
        int admitted = 0;
        var applicants = participants.iterator();
        while (applicants.hasNext()) {
            UUID id = applicants.next();
            if (++admitted > currentWorld.maxPlayers()) {
                nextGameQueue.add(id);
                applicants.remove();
                Player player = Bukkit.getPlayer(id);
                if (player != null) {
                    display.hide(player);
                    player.sendMessage(Component.text("이번 월드의 정원을 초과하여 다음 게임을 기다립니다.", NamedTextColor.YELLOW));
                }
            }
        }
        state = GameState.RUNNING;
        int targetScore = settings.targetScore();
        gameTeams.setTargetScore(targetScore);
        gameTeams.setTargetReachedListener(() -> finishGame(false));
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.closeInventory();
                book.removeMenuBooks(player);
                RailGunItem.removeFromInventory(player);
                player.getInventory().addItem(RailGunItem.create());
                doubleJump.enable(player);
                float walkSpeed = player.getWalkSpeed();
                previousWalkSpeeds.put(playerId, walkSpeed);
                player.setWalkSpeed(Math.max(-1.0F, Math.min(1.0F, walkSpeed * WALK_SPEED_MULTIPLIER)));
            }
        }
        // Capture the duration so configuration changes apply only to the next game.
        int duration = settings.durationSeconds();
        Runnable updateScoreDisplay = () -> display.running(gameRemaining, duration,
                gameTeams.getTeamScore("RED"), gameTeams.getTeamScore("BLUE"), targetScore);
        gameTeams.setScoreChangedListener(updateScoreDisplay);
        timer.start(duration, remaining -> {
            gameRemaining = remaining;
            updateScoreDisplay.run();
            gameTeams.updateTabListFooter();
        },
                () -> finishGame(false));
    }

    private void restoreWalkSpeed(Player player) {
        Float walkSpeed = previousWalkSpeeds.remove(player.getUniqueId());
        if (walkSpeed != null) {
            player.setWalkSpeed(walkSpeed);
        }
    }

    private void finishGame(boolean cancelled) {
        timer.cancel();
        int redScore = gameTeams.getTeamScore("RED");
        int blueScore = gameTeams.getTeamScore("BLUE");
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                doubleJump.disable(player);
                restoreWalkSpeed(player);
                display.hide(player);
                lobbyArrival.resetAndSend(player);
                String team = gameTeams.getTeamOf(player);
                if (!cancelled && team != null) {
                    display.showResult(player, team, redScore, blueScore);
                }
                player.sendMessage(Component.text(cancelled
                        ? "참여 인원 부족으로 게임이 취소되었습니다."
                        : "게임이 종료되었습니다.", NamedTextColor.GOLD));
                if (player.isOnline()) {
                    nextGameQueue.add(playerId);
                }
            }
        }
        gameTeams.clear();
        previousWalkSpeeds.clear();
        returnLocations.clear();
        participants.clear();
        state = GameState.WAITING;
        promoteQueue(cancelled);
        rotateWorld();
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
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                display.show(player);
            }
        }
    }

    private void returnPlayers() {
        returnLocations.forEach((id, location) -> {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.teleport(location);
            }
        });
        returnLocations.clear();
    }

    public void shutdown() {
        timer.cancel();
        state = GameState.WAITING;
        gameTeams.clear();
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                doubleJump.disable(player);
                restoreWalkSpeed(player);
                RailGunItem.removeFromInventory(player);
                display.hide(player);
                book.giveMenuBook(player);
            }
        }
        previousWalkSpeeds.clear();
        returnPlayers();
        participants.clear();
        nextGameQueue.clear();
        worldRotation.clear();
    }

    public DoubleJump getDoubleJump() {
        return doubleJump;
    }

    public GameTeams getGameTeams() {
        return gameTeams;
    }

    public void setTargetScore(int score) {
        if (score <= 0) {
            throw new IllegalArgumentException("Target score must be positive");
        }
        settings.setTargetScore(score);
    }

    public int getTargetScore() {
        return settings.targetScore();
    }
}
