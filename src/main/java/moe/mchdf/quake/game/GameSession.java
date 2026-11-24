package moe.mchdf.quake.game;

import java.util.ArrayList;
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
    private final List<GameListener> listeners = new ArrayList<>();
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
        listeners.add(new GameDisplay());
        rotateWorld();
    }

    /** 게임 이벤트 구독자 등록 (채팅 알림, 모드 패킷 전송 등) */
    public void addListener(GameListener listener) {
        listeners.add(listener);
    }

    private void publish(GameEvent event) {
        for (GameListener listener : listeners) {
            listener.onGameEvent(event);
        }
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

    /** 진행 중인 게임의 참가자인지 */
    public boolean isPlaying(Player player) {
        return isRunning() && contains(player);
    }

    /** 이 플레이어 기준의 상태 스냅샷. 참여 메뉴와 모드 화면이 같은 데이터를 쓰도록 한다. */
    public LobbyStatus lobbyStatus(Player player) {
        LobbyStatus.Participation participation;
        if (contains(player)) {
            participation = isRunning() ? LobbyStatus.Participation.PLAYING : LobbyStatus.Participation.WAITING;
        } else if (isQueued(player)) {
            participation = LobbyStatus.Participation.QUEUED;
        } else {
            participation = LobbyStatus.Participation.NONE;
        }
        return new LobbyStatus(state, participation, hasReadyWorlds(), currentWorldSummary(), participants.size());
    }

    private LobbyStatus.WorldSummary currentWorldSummary() {
        return currentWorld == null ? null
                : new LobbyStatus.WorldSummary(currentWorld.displayName(),
                        currentWorld.minPlayers(), currentWorld.maxPlayers());
    }

    public boolean hasReadyWorlds() {
        return !worlds.readyWorlds().isEmpty();
    }

    /** 참여를 처리하고 결과를 돌려준다. 안내 메시지는 호출한 화면 쪽에서 출력한다. */
    public JoinResult join(Player player) {
        if (!hasReadyWorlds()) {
            return JoinResult.NO_READY_WORLD;
        }
        if (isRunning()) {
            if (contains(player)) {
                return JoinResult.ALREADY_IN_GAME;
            }
            return nextGameQueue.add(player.getUniqueId())
                    ? JoinResult.QUEUED_FOR_NEXT_GAME
                    : JoinResult.ALREADY_QUEUED;
        }
        if (!participants.add(player.getUniqueId())) {
            return JoinResult.ALREADY_WAITING;
        }
        updateAfterJoin();
        publish(new GameEvent.PlayerShown(player));
        return JoinResult.JOINED;
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
                    remaining -> publish(new GameEvent.Countdown(currentWorld.displayName(), remaining,
                            GameSettings.COUNTDOWN_SECONDS, List.copyOf(participants))),
                    this::startGame);
        } else {
            showPlayerCount();
        }
    }

    /** 플레이어가 직접 요청한 참여 취소. 안내 메시지는 호출한 화면 쪽에서 출력한다. */
    public LeaveResult requestLeave(Player player) {
        if (isPlaying(player)) {
            return LeaveResult.GAME_IN_PROGRESS;
        }
        return leave(player) ? LeaveResult.LEFT : LeaveResult.NOT_JOINED;
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
        publish(new GameEvent.PlayerHidden(player));
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
        publish(new GameEvent.Waiting(currentWorldSummary(), participants.size(), waitingRemaining));
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
                publish(new GameEvent.TeleportFailed(List.copyOf(participants)));
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
                    publish(new GameEvent.PlayerHidden(player));
                    publish(new GameEvent.NotAdmitted(player));
                }
            }
        }
        state = GameState.RUNNING;
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            String team = gameTeams.getTeamOf(player);
            if (player != null && team != null) {
                publish(new GameEvent.TeamAssigned(player, team));
            }
        }
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
        Runnable updateScoreDisplay = () -> publish(new GameEvent.Running(gameRemaining, duration,
                gameTeams.getTeamScore("RED"), gameTeams.getTeamScore("BLUE"), targetScore));
        gameTeams.setPointScoredListener(event -> {
            publish(event);
            updateScoreDisplay.run();
        });
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
                publish(new GameEvent.PlayerHidden(player));
                lobbyArrival.resetAndSend(player);
                publish(new GameEvent.GameFinished(player, gameTeams.getTeamOf(player), redScore, blueScore, cancelled));
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
                publish(new GameEvent.AutoJoined(player, previousGameCancelled));
            }
        }
        nextGameQueue.clear();
        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                publish(new GameEvent.PlayerShown(player));
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
                publish(new GameEvent.PlayerHidden(player));
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
