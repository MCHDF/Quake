package moe.mchdf.quake.game;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class GameTeamsTest {
    private final Map<UUID, Player> online = new HashMap<>();
    private final Map<String, Score> scores = new HashMap<>();
    private MockedStatic<Bukkit> bukkit;
    private Scoreboard scoreboard;
    private final GameTeams teams = new GameTeams();

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getPlayer(any(UUID.class)))
                .thenAnswer(invocation -> online.get(invocation.getArgument(0)));
        ScoreboardManager manager = mock(ScoreboardManager.class);
        bukkit.when(Bukkit::getScoreboardManager).thenReturn(manager);
        scoreboard = mock(Scoreboard.class);
        when(manager.getNewScoreboard()).thenReturn(scoreboard);
        when(scoreboard.getTeams()).thenReturn(Set.of());
        when(scoreboard.registerNewTeam(anyString())).thenAnswer(invocation -> {
            Team team = mock(Team.class);
            String name = invocation.getArgument(0);
            when(team.getName()).thenReturn(name);
            when(team.color()).thenReturn(GameTeams.colorOf(name));
            return team;
        });
        Objective objective = mock(Objective.class);
        // Criteria 상수는 클래스 초기화 때 Bukkit을 호출하므로 매처를 만들기 전에 먼저 읽어 둔다.
        Criteria dummy = Criteria.DUMMY;
        when(scoreboard.registerNewObjective(eq("kills"), eq(dummy), any(Component.class))).thenReturn(objective);
        when(objective.getScore(anyString()))
                .thenAnswer(invocation -> scores.computeIfAbsent(invocation.getArgument(0), name -> mock(Score.class)));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn(name);
        online.put(id, player);
        return player;
    }

    @Test
    void killsAreKeptByPluginAndOnlyMirroredToScoreboard() {
        Player first = player("first");
        Player second = player("second");
        teams.assign(List.of(first.getUniqueId(), second.getUniqueId()));
        assertTrue(teams.areEnemies(first, second));
        Player shooter = "RED".equals(teams.getTeamOf(first)) ? first : second;
        Player target = shooter == first ? second : first;

        teams.awardPoint(shooter, target);
        teams.awardPoint(shooter, target);

        assertEquals(2, teams.getKills(shooter));
        assertEquals(0, teams.getKills(target));
        assertEquals(2, teams.getTeamScore("RED"));
        assertEquals(0, teams.getTeamScore("BLUE"));
        verify(scores.get(shooter.getName())).setScore(2);

        teams.clear();
        assertEquals(0, teams.getKills(shooter));
        assertNull(teams.getTeamOf(shooter));
    }

    @Test
    void scoringPublishesEventToEveryParticipantInsteadOfSendingChat() {
        Player first = player("first");
        Player second = player("second");
        teams.assign(List.of(first.getUniqueId(), second.getUniqueId()));
        List<GameEvent.PointScored> events = new java.util.ArrayList<>();
        teams.setPointScoredListener(events::add);
        String firstTeam = teams.getTeamOf(first);

        teams.awardPoint(first, second);

        assertEquals(1, events.size());
        GameEvent.PointScored event = events.getFirst();
        assertSame(first, event.shooter());
        assertEquals(firstTeam, event.shooterTeam());
        assertSame(second, event.target());
        assertEquals(Set.of(first.getUniqueId(), second.getUniqueId()), Set.copyOf(event.recipients()));
        verify(first, never()).sendMessage(any(Component.class));
        verify(second, never()).sendMessage(any(Component.class));
    }

    @Test
    void emptyTeamIsDetectedFromPluginData() {
        Player first = player("first");
        Player second = player("second");
        teams.assign(List.of(first.getUniqueId(), second.getUniqueId()));
        assertFalse(teams.hasEmptyTeam());
        teams.remove(first);
        assertTrue(teams.hasEmptyTeam());
    }

    @Test
    void colorOfUsesTeamConstants() {
        assertEquals(NamedTextColor.RED, GameTeams.colorOf("RED"));
        assertEquals(GameTeams.BLUE_COLOR, GameTeams.colorOf("BLUE"));
    }
}
