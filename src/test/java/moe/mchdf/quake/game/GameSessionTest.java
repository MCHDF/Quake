package moe.mchdf.quake.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import moe.mchdf.quake.menu.MenuBook;
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

class GameSessionTest {
    private final Map<UUID, Player> online = new HashMap<>();
    private final List<ScheduledTask> tasks = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private MockedConstruction<GameTeams> teams;
    private GameSession game;
    private MenuBook book;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        teams = mockConstruction(GameTeams.class);
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        book = mock(MenuBook.class);
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
        game = new GameSession(plugin, book);
    }

    @AfterEach
    void tearDown() {
        teams.close();
        bukkit.close();
    }

    private Player player() {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenAnswer(invocation -> online.containsKey(id));
        online.put(id, player);
        return player;
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
        assertTrue(tasks.stream().allMatch(task -> task.cancelled));
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
        verify(book).giveMenuBook(second);
        verify(book).giveMenuBook(third);
    }

    @Test
    void oneRemainingPlayerCancelsGameAndAutomaticallyWaitsForNextGame() {
        Player first = player();
        Player second = player();
        start(first, second);
        game.requestLeave(first);
        assertTrue(game.contains(first));
        disconnect(first);
        assertEquals(GameState.WAITING, game.getState());
        assertTrue(game.contains(second));
        assertFalse(game.contains(first));
        assertFalse(game.isQueued(second));
        verify(book).giveMenuBook(second);
        assertTrue(tasks.stream().allMatch(task -> task.cancelled));
        tickSeconds(200);
        assertTrue(game.contains(second));
        assertEquals(GameState.WAITING, game.getState());
        game.requestLeave(second);
        assertFalse(game.contains(second));
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
        verify(book).giveMenuBook(remaining);
        tickSeconds(29);
        assertEquals(GameState.COUNTDOWN, game.getState());
        tickSeconds(1);
        assertTrue(game.isRunning());
        verify(book, times(2)).removeMenuBooks(remaining);
        verify(book).removeMenuBooks(next);
    }

    @Test
    void normalEndPromotesQueueAndStartsExactlyOneCountdown() {
        game.setGameDurationSeconds(3);
        Player first = player();
        Player second = player();
        start(first, second);
        Player next = player();
        Player other = player();
        game.join(first); // Current participants cannot queue for the next game.
        assertFalse(game.isQueued(first));
        game.join(next);
        game.join(next); // Duplicate requests must not duplicate promotion.
        game.join(other);
        assertTrue(game.isQueued(next));
        assertFalse(game.contains(next));
        verify(next, never()).showBossBar(any());
        tickSeconds(3);
        assertFalse(game.contains(first));
        assertFalse(game.contains(second));
        verify(book).giveMenuBook(first);
        assertTrue(game.contains(next));
        assertTrue(game.contains(other));
        assertFalse(game.isQueued(next));
        assertEquals(GameState.COUNTDOWN, game.getState());
        assertEquals(1, tasks.stream().filter(task -> !task.cancelled).count());
        verify(next).showBossBar(any());
        tickSeconds(30);
        assertTrue(game.isRunning());
        verify(book).removeMenuBooks(next);
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
        assertTrue(tasks.stream().allMatch(task -> task.cancelled));
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
        assertEquals(GameState.WAITING, game.getState());
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
        assertEquals(GameState.WAITING, game.getState());
        assertTrue(tasks.stream().allMatch(task -> task.cancelled));
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
