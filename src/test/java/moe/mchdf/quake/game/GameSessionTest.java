package moe.mchdf.quake.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import moe.mchdf.quake.combat.HitZone;
import moe.mchdf.quake.menu.LobbyItems;
import moe.mchdf.quake.item.RailGunItem;
import moe.mchdf.quake.world.GameWorld;
import moe.mchdf.quake.world.GameWorldInfo;
import moe.mchdf.quake.world.GameWorldStatus;
import moe.mchdf.quake.world.LobbyArrival;
import moe.mchdf.quake.world.LobbyWorldManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import moe.mchdf.quake.scoreboard.PlayerBoards;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.ArgumentCaptor;

class GameSessionTest {
    private final Map<UUID, Player> online = new HashMap<>();
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private MockedConstruction<GameTeams> teams;
    private GameSession game;
    private LobbyItems lobbyItems;
    private GameWorld worlds;
    private Location lobbySpawn;
    private MockedStatic<RailGunItem> railGun;
    private final List<GameWorldInfo> ready = new ArrayList<>();

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        teams = mockConstruction(GameTeams.class, (mock, context) ->
                when(mock.getTeamOf(any(Player.class))).thenReturn("RED"));
        railGun = mockStatic(RailGunItem.class);
        railGun.when(RailGunItem::create).thenReturn(mock(ItemStack.class));
        worlds = mock(GameWorld.class);
        when(worlds.readyWorlds()).thenAnswer(invocation -> List.copyOf(ready));
        ready.add(arena("arena_a", 2, 16));
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        lobbyItems = mock(LobbyItems.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.getPlayer(any(UUID.class)))
                .thenAnswer(invocation -> online.get(invocation.getArgument(0)));
        when(scheduler.runTaskTimer(eq(plugin), any(Runnable.class), eq(20L), eq(20L)))
                .thenAnswer(invocation -> {
                    ScheduledTask task = new ScheduledTask(invocation.getArgument(1));
                    tasks.add(task);
                    return task.handle;
                });
        LobbyWorldManager lobby = mock(LobbyWorldManager.class);
        lobbySpawn = new Location(mock(World.class), 1, 64, 2);
        when(lobby.spawn()).thenReturn(lobbySpawn);
        game = new GameSession(plugin, lobbyItems, worlds, new LobbyArrival(lobby, lobbyItems), mock(PlayerBoards.class));
    }

    @AfterEach
    void tearDown() {
        railGun.close();
        teams.close();
        bukkit.close();
    }

    private GameWorldInfo arena(String name, int min, int max) {
        World world = mock(World.class);
        UUID id = UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(world.getName()).thenReturn(name);
        when(world.getUID()).thenReturn(id);
        when(world.getSpawnLocation()).thenReturn(new Location(world, 0, 65, 0));
        bukkit.when(() -> Bukkit.getWorld(name)).thenReturn(world);
        Location redSpawn = world.getSpawnLocation();
        when(worlds.teamSpawn(name, "RED")).thenReturn(redSpawn);
        when(worlds.teamSpawn(name, "BLUE")).thenReturn(new Location(world, 10, 65, 10, 90, 0));
        return new GameWorldInfo(name, id, name, GameWorldStatus.READY, null, null, min, max);
    }

    @Test
    void rotatesAfterThirtySecondsAndImmediatelyUsesNextWorldMinimum() {
        ready.set(0, arena("arena_a", 4, 16));
        ready.add(arena("arena_b", 2, 8));
        Player first = player();
        Player second = player();
        game.join(first);
        game.join(second);
        tickSeconds(29);
        assertEquals("arena_a", game.getCurrentWorld().name());
        assertEquals(GameState.WAITING, game.getState());
        tickSeconds(1);
        assertEquals("arena_b", game.getCurrentWorld().name());
        assertEquals(GameState.COUNTDOWN, game.getState());
        assertTrue(game.contains(first));
        tickSeconds(30);
        assertTrue(game.isRunning());
        verify(first).teleport(Bukkit.getWorld("arena_b").getSpawnLocation());
    }

    @Test
    void gameLogicPublishesEventsToRegisteredListeners() {
        List<GameEvent> events = new ArrayList<>();
        game.addListener(events::add);
        Player first = player();
        Player second = player();
        start(first, second);
        assertTrue(events.contains(new GameEvent.PlayerShown(first)));
        assertTrue(events.stream().anyMatch(event -> event instanceof GameEvent.Countdown));
        assertTrue(events.contains(new GameEvent.TeamAssigned(first, "RED")));
        assertTrue(events.stream().anyMatch(event -> event instanceof GameEvent.Running));

        disconnect(first);
        assertTrue(events.contains(new GameEvent.PlayerHidden(first)));
        assertTrue(events.contains(new GameEvent.GameFinished(second, "RED", 0, 0, true)));
        assertTrue(events.contains(new GameEvent.AutoJoined(second, true)));
    }

    @Test
    void lobbyStatusReflectsEachPlayersParticipation() {
        Player first = player();
        Player second = player();
        Player outsider = player();
        LobbyStatus idle = game.lobbyStatus(first);
        assertEquals(LobbyStatus.Participation.NONE, idle.participation());
        assertEquals(GameState.WAITING, idle.state());
        assertTrue(idle.hasReadyWorlds());
        assertEquals(new LobbyStatus.WorldSummary("arena_a", 2, 16), idle.world());
        assertEquals(0, idle.participantCount());

        game.join(first);
        assertEquals(LobbyStatus.Participation.WAITING, game.lobbyStatus(first).participation());
        assertEquals(1, game.lobbyStatus(outsider).participantCount());

        start(first, second);
        assertEquals(LobbyStatus.Participation.PLAYING, game.lobbyStatus(first).participation());
        assertTrue(game.isPlaying(first));
        game.join(outsider);
        LobbyStatus queued = game.lobbyStatus(outsider);
        assertEquals(LobbyStatus.Participation.QUEUED, queued.participation());
        assertTrue(queued.isRunning());
        assertFalse(game.isPlaying(outsider));
    }

    @Test
    void reachingMinimumStartsCountdownWithoutWaitingForRotationDeadline() {
        tickSeconds(7);
        game.join(player());
        game.join(player());
        assertEquals(GameState.COUNTDOWN, game.getState());
        tickSeconds(29);
        assertEquals(GameState.COUNTDOWN, game.getState());
        tickSeconds(1);
        assertTrue(game.isRunning());
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
    }

    @Test
    void rotationWrapsAndHandlesNoReadyWorlds() {
        ready.add(arena("arena_b", 2, 16));
        tickSeconds(30);
        assertEquals("arena_b", game.getCurrentWorld().name());
        tickSeconds(30);
        assertEquals("arena_a", game.getCurrentWorld().name());
        ready.clear();
        Player first = player();
        assertEquals(JoinResult.NO_READY_WORLD, game.join(first));
        assertFalse(game.contains(first));
        tickSeconds(30);
        assertNull(game.getCurrentWorld());
        Player second = player();
        game.join(second);
        assertFalse(game.contains(second));
        assertEquals(GameState.WAITING, game.getState());
        ready.add(arena("arena_c", 2, 16));
        game.join(first);
        game.join(second);
        assertEquals(GameState.COUNTDOWN, game.getState());
        assertEquals("arena_c", game.getCurrentWorld().name());
    }

    @Test
    void unavailableWorldAtCountdownEndIsSkipped() {
        game.join(player());
        game.join(player());
        ready.clear();
        ready.add(arena("arena_b", 4, 16));
        tickSeconds(30);
        assertEquals(GameState.WAITING, game.getState());
        assertEquals("arena_b", game.getCurrentWorld().name());
        verify(teams.constructed().getFirst(), never()).assign(any());
    }

    @Test
    void capacityUsesApplicationOrderAndKeepsOverflowForNextWorld() {
        ready.set(0, arena("arena_a", 2, 2));
        ready.add(arena("arena_b", 2, 16));
        game.setGameDurationSeconds(1);
        Player first = player();
        Player second = player();
        Player third = player();
        game.join(first);
        game.join(second);
        game.join(third);
        tickSeconds(30);
        assertTrue(game.contains(first));
        assertTrue(game.contains(second));
        assertFalse(game.contains(third));
        assertTrue(game.isQueued(third));
        tickSeconds(1);
        assertEquals("arena_b", game.getCurrentWorld().name());
        assertTrue(game.contains(third));
        assertFalse(game.isQueued(third));
    }

    @Test
    void leavingWhileWaitingDoesNotResetThirtySecondDeadline() {
        ready.set(0, arena("arena_a", 4, 16));
        ready.add(arena("arena_b", 4, 16));
        Player first = player();
        game.join(first);
        tickSeconds(20);
        game.leave(first);
        tickSeconds(10);
        assertEquals("arena_b", game.getCurrentWorld().name());
    }

    @Test
    void newlyReadyWorldIsAppendedAfterAlreadyWaitingWorlds() {
        ready.add(arena("arena_b", 2, 16).withStatus(GameWorldStatus.READY, 1));
        ready.add(arena("arena_c", 2, 16).withStatus(GameWorldStatus.READY, 2));
        tickSeconds(30);
        assertEquals("arena_b", game.getCurrentWorld().name());
        ready.add(arena("aaa_new", 2, 16).withStatus(GameWorldStatus.READY, 3));
        game.onWorldSettingsChanged();
        assertEquals("arena_b", game.getCurrentWorld().name());
        tickSeconds(30);
        assertEquals("arena_c", game.getCurrentWorld().name());
        tickSeconds(30);
        assertEquals("arena_a", game.getCurrentWorld().name());
        tickSeconds(30);
        assertEquals("aaa_new", game.getCurrentWorld().name());
    }

    @Test
    void maintenanceRemovesCurrentWorldImmediatelyAndReReadyJoinsTail() {
        ready.add(arena("arena_b", 2, 16).withStatus(GameWorldStatus.READY, 1));
        ready.add(arena("arena_c", 2, 16).withStatus(GameWorldStatus.READY, 2));
        GameWorldInfo original = ready.removeFirst();
        game.onWorldSettingsChanged();
        assertEquals("arena_b", game.getCurrentWorld().name());
        ready.add(original.withStatus(GameWorldStatus.READY, 3));
        game.onWorldSettingsChanged();
        tickSeconds(30);
        assertEquals("arena_c", game.getCurrentWorld().name());
        tickSeconds(30);
        assertEquals("arena_a", game.getCurrentWorld().name());
    }

    @Test
    void playersTeleportToAssignedTeamSpawns() {
        Player red = player();
        Player blue = player();
        when(teams.constructed().getFirst().getTeamOf(blue)).thenReturn("BLUE");
        start(red, blue);
        verify(red).teleport(worlds.teamSpawn("arena_a", "RED"));
        verify(blue).teleport(worlds.teamSpawn("arena_a", "BLUE"));
        verify(blue, never()).teleport(worlds.teamSpawn("arena_a", "RED"));
    }

    @Test
    void teleportFailureRestoresTeamsAndKeepsApplicants() {
        Player first = player();
        Player second = player();
        when(second.teleport(any(Location.class))).thenReturn(false);
        game.join(first);
        game.join(second);
        tickSeconds(30);
        assertFalse(game.isRunning());
        assertTrue(game.contains(first));
        assertTrue(game.contains(second));
        verify(teams.constructed().getFirst()).clear();
        Location original = first.getLocation();
        verify(first).teleport(original);
        verify(lobbyItems, never()).remove(any());
    }

    private Player player() {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenAnswer(invocation -> online.containsKey(id));
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        when(player.getLocation()).thenReturn(new Location(mock(World.class), 0, 65, 0));
        when(player.teleport(any(Location.class))).thenReturn(true);
        online.put(id, player);
        return player;
    }

    private void verifyLobbyArrival(Player player) {
        verify(player.getInventory()).clear();
        verify(player.getInventory()).setHeldItemSlot(0);
        verify(lobbyItems).give(player);
        verify(player).setGameMode(GameMode.ADVENTURE);
        verify(player).teleport(lobbySpawn);
        Location original = player.getLocation();
        verify(player, never()).teleport(original);
    }

    private void disconnect(Player player) {
        online.remove(player.getUniqueId());
        game.leave(player);
    }

    private void tickSeconds(int seconds) {
        for (int i = 0; i < seconds; i++) {
            for (ScheduledTask task : List.copyOf(tasks)) {
                if (!task.cancelled) {
                    task.action.run();
                }
            }
        }
    }

    private void start(Player first, Player second) {
        game.join(first);
        game.join(second);
        tickSeconds(30);
        assertEquals(GameState.RUNNING, game.getState());
    }

    @Test
    void disconnectDuringCountdownCancelsParticipationAndTimer() {
        Player first = player();
        Player second = player();
        game.join(first);
        game.join(second);
        tickSeconds(10);
        disconnect(second);
        assertEquals(GameState.WAITING, game.getState());
        assertFalse(game.contains(second));
        tickSeconds(30);
        assertEquals(GameState.WAITING, game.getState());
        verify(teams.constructed().getFirst(), never()).assign(any());
        game.join(player());
        tickSeconds(29);
        assertEquals(GameState.COUNTDOWN, game.getState());
        tickSeconds(1);
        assertTrue(game.isRunning());
    }

    @Test
    void leaveDuringCountdownRestartsCountdownWhenMinimumIsStillMet() {
        Player first = player();
        Player second = player();
        Player third = player();
        game.join(first);
        game.join(second);
        game.join(third);
        tickSeconds(10);
        disconnect(third);
        assertEquals(GameState.COUNTDOWN, game.getState());
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
        tickSeconds(29);
        assertEquals(GameState.COUNTDOWN, game.getState());
        tickSeconds(1);
        assertTrue(game.isRunning());
        assertTrue(game.contains(first));
        assertTrue(game.contains(second));
    }

    @Test
    void offlineParticipantsAtCountdownEndReturnSessionToWaiting() {
        Player first = player();
        Player second = player();
        game.join(first);
        game.join(second);
        online.remove(second.getUniqueId()); // Offline without a quit event reaching the session.
        tickSeconds(30);
        assertEquals(GameState.WAITING, game.getState());
        assertTrue(game.contains(first));
        assertFalse(game.contains(second));
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
        verify(teams.constructed().getFirst(), never()).assign(any());
        game.join(player());
        assertEquals(GameState.COUNTDOWN, game.getState());
    }

    @Test
    void emptyTeamCancelsGameAndRemainingPlayersWaitForNextGame() {
        Player first = player();
        Player second = player();
        Player third = player();
        game.join(first);
        game.join(second);
        game.join(third);
        tickSeconds(30);
        assertTrue(game.isRunning());
        when(teams.constructed().getFirst().hasEmptyTeam()).thenReturn(true);
        disconnect(first);
        assertEquals(GameState.COUNTDOWN, game.getState());
        assertTrue(game.contains(second));
        assertTrue(game.contains(third));
        assertFalse(game.contains(first));
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
        verifyLobbyArrival(second);
        verifyLobbyArrival(third);
        verify(first, never()).teleport(lobbySpawn);
    }

    @Test
    void oneRemainingPlayerCancelsGameAndAutomaticallyWaitsForNextGame() {
        Player first = player();
        Player second = player();
        start(first, second);
        assertEquals(LeaveResult.GAME_IN_PROGRESS, game.requestLeave(first));
        assertTrue(game.contains(first));
        disconnect(first);
        assertEquals(GameState.WAITING, game.getState());
        assertTrue(game.contains(second));
        assertFalse(game.contains(first));
        assertFalse(game.isQueued(second));
        verify(lobbyItems).give(second);
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
        tickSeconds(200);
        assertTrue(game.contains(second));
        assertEquals(GameState.WAITING, game.getState());
        assertEquals(LeaveResult.LEFT, game.requestLeave(second));
        assertFalse(game.contains(second));
        assertEquals(LeaveResult.NOT_JOINED, game.requestLeave(second));
    }

    @Test
    void playerWhoTurnedOffAutoRejoinStaysOutOfNextGame() {
        Player first = player();
        Player second = player();
        Player third = player();
        game.setAutoRejoin(player -> player != third);
        game.join(first);
        game.join(second);
        game.join(third);
        tickSeconds(30);
        assertTrue(game.isRunning());
        when(teams.constructed().getFirst().hasEmptyTeam()).thenReturn(true);
        disconnect(first);
        assertTrue(game.contains(second));
        assertFalse(game.contains(third));
        assertFalse(game.isQueued(third));
        verifyLobbyArrival(third);
    }

    @Test
    void twoRemainingPlayersContinueCurrentGame() {
        Player first = player();
        Player second = player();
        Player third = player();
        game.join(first);
        game.join(second);
        game.join(third);
        tickSeconds(30);
        disconnect(first);
        tickSeconds(5);
        assertTrue(game.isRunning());
        assertTrue(game.contains(second));
        assertTrue(game.contains(third));
    }

    @Test
    void remainingPlayerAndQueuedPlayerStartNextCountdownTogether() {
        Player first = player();
        Player remaining = player();
        start(first, remaining);
        Player next = player();
        game.join(next);
        disconnect(first);
        assertEquals(GameState.COUNTDOWN, game.getState());
        assertTrue(game.contains(remaining));
        assertTrue(game.contains(next));
        assertFalse(game.contains(first));
        assertFalse(game.isQueued(next));
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
        verify(lobbyItems).give(remaining);
        tickSeconds(29);
        assertEquals(GameState.COUNTDOWN, game.getState());
        tickSeconds(1);
        assertTrue(game.isRunning());
        verify(lobbyItems, times(2)).remove(remaining);
        verify(lobbyItems).remove(next);
    }

    @Test
    void normalEndPromotesQueueAndReturningPlayersAndStartsExactlyOneCountdown() {
        game.setGameDurationSeconds(3);
        Player first = player();
        Player second = player();
        start(first, second);
        Player next = player();
        Player other = player();
        // Current participants cannot queue for the next game.
        assertEquals(JoinResult.ALREADY_IN_GAME, game.join(first));
        assertFalse(game.isQueued(first));
        assertEquals(JoinResult.QUEUED_FOR_NEXT_GAME, game.join(next));
        // Duplicate requests must not duplicate promotion.
        assertEquals(JoinResult.ALREADY_QUEUED, game.join(next));
        game.join(other);
        assertTrue(game.isQueued(next));
        assertFalse(game.contains(next));
        verify(next, never()).showBossBar(any());
        var bar = ArgumentCaptor.forClass(BossBar.class);
        verify(first).showBossBar(bar.capture());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.function.Consumer<GameEvent.PointScored>> pointScored =
                ArgumentCaptor.forClass(java.util.function.Consumer.class);
        GameTeams gameTeams = teams.constructed().getFirst();
        verify(gameTeams).setPointScoredListener(pointScored.capture());
        when(gameTeams.getTeamScore("RED")).thenReturn(3);
        when(gameTeams.getTeamScore("BLUE")).thenReturn(2);
        pointScored.getValue().accept(new GameEvent.PointScored(first, "RED", second, "BLUE", HitZone.BODY, List.of()));
        var plainText = PlainTextComponentSerializer.plainText();
        assertEquals("RED 3  |  BLUE 2  |  목표 5점  |  00:03",
                plainText.serialize(bar.getValue().name()));
        tickSeconds(3);
        var result = ArgumentCaptor.forClass(Title.class);
        verify(first).showTitle(result.capture());
        assertEquals("승리", plainText.serialize(result.getValue().title()));
        assertEquals("최종 점수  |  RED 3  |  BLUE 2", plainText.serialize(result.getValue().subtitle()));
        assertTrue(game.contains(first));
        assertTrue(game.contains(second));
        verifyLobbyArrival(first);
        verifyLobbyArrival(second);
        verify(next, never()).teleport(lobbySpawn);
        assertTrue(game.contains(next));
        assertTrue(game.contains(other));
        assertFalse(game.isQueued(next));
        assertEquals(GameState.COUNTDOWN, game.getState());
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
        verify(next).showBossBar(any());
        tickSeconds(30);
        assertTrue(game.isRunning());
        verify(lobbyItems).remove(next);
    }

    @Test
    void cancellationPromotesOneQueuedPlayerToWaiting() {
        Player first = player();
        Player second = player();
        start(first, second);
        Player next = player();
        game.join(next);
        disconnect(first);
        disconnect(second);
        assertEquals(GameState.WAITING, game.getState());
        assertTrue(game.contains(next));
        assertFalse(game.isQueued(next));
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
        game.join(player());
        assertEquals(GameState.COUNTDOWN, game.getState());
    }

    @Test
    void cancelledDisconnectedAndOfflineApplicantsAreNotPromoted() {
        game.setGameDurationSeconds(1);
        start(player(), player());
        Player cancelled = player();
        Player disconnected = player();
        Player offline = player();
        game.join(cancelled);
        game.join(disconnected);
        game.join(offline);
        game.requestLeave(cancelled);
        disconnect(disconnected);
        online.remove(offline.getUniqueId());
        assertTrue(game.isRunning());
        tickSeconds(1);
        for (Player applicant : List.of(cancelled, disconnected, offline)) {
            assertFalse(game.isQueued(applicant));
            assertFalse(game.contains(applicant));
        }
        assertEquals(GameState.COUNTDOWN, game.getState());
    }

    @Test
    void durationChangeAppliesOnlyToNextGame() {
        game.setGameDurationSeconds(3);
        start(player(), player());
        game.setGameDurationSeconds(1);
        game.join(player());
        game.join(player());
        tickSeconds(1);
        assertTrue(game.isRunning());
        tickSeconds(2);
        assertEquals(GameState.COUNTDOWN, game.getState());
        tickSeconds(30);
        assertTrue(game.isRunning());
        tickSeconds(1);
        assertEquals(GameState.COUNTDOWN, game.getState());
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
    }

    @Test
    void shutdownClearsQueueWithoutStartingNextGame() {
        Player first = player();
        start(first, player());
        Player next = player();
        game.join(next);
        game.shutdown();
        tickSeconds(200);
        assertFalse(game.isQueued(next));
        assertFalse(game.contains(next));
        assertFalse(game.contains(first));
        assertEquals(GameState.WAITING, game.getState());
        assertTrue(tasks.stream().allMatch(task -> task.cancelled));
    }

    private static final class ScheduledTask {
        final Runnable action;
        final BukkitTask handle = mock(BukkitTask.class);
        boolean cancelled;

        ScheduledTask(Runnable action) {
            this.action = action;
            doAnswer(invocation -> {
                cancelled = true;
                return null;
            }).when(handle).cancel();
        }
    }
}
