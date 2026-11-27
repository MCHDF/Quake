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
import moe.mchdf.quake.combat.HitZone;
import moe.mchdf.quake.scoreboard.PlayerBoards;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class GameTeamsTest {
    private final Map<UUID, Player> online = new HashMap<>();
    private final Map<UUID, Scoreboard> playerBoards = new HashMap<>();
    // 스코어보드별 처치 수 점수: 스코어보드 → (플레이어 이름 → Score)
    private final Map<Scoreboard, Map<String, Score>> scores = new HashMap<>();
    private MockedStatic<Bukkit> bukkit;
    private PlayerBoards boards;
    private GameTeams teams;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.getPlayer(any(UUID.class)))
                .thenAnswer(invocation -> online.get(invocation.getArgument(0)));
        boards = mock(PlayerBoards.class);
        when(boards.boardOf(any(Player.class)))
                .thenAnswer(invocation -> playerBoards.get(((Player) invocation.getArgument(0)).getUniqueId()));
        when(boards.find(any(UUID.class))).thenAnswer(invocation -> playerBoards.get(invocation.getArgument(0)));
        teams = new GameTeams(boards);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Scoreboard board() {
        Scoreboard board = mock(Scoreboard.class);
        Map<String, Team> registeredTeams = new HashMap<>();
        when(board.registerNewTeam(anyString())).thenAnswer(invocation -> {
            Team team = mock(Team.class);
            String name = invocation.getArgument(0);
            when(team.getName()).thenReturn(name);
            when(team.color()).thenReturn(GameTeams.colorOf(name));
            registeredTeams.put(name, team);
            return team;
        });
        when(board.getTeam(anyString())).thenAnswer(invocation -> registeredTeams.get(invocation.getArgument(0)));
        Objective objective = mock(Objective.class);
        // Criteria 상수는 클래스 초기화 때 Bukkit을 호출하므로 매처를 만들기 전에 먼저 읽어 둔다.
        Criteria dummy = Criteria.DUMMY;
        when(board.registerNewObjective(eq("kills"), eq(dummy), any(Component.class))).thenAnswer(invocation -> {
            when(board.getObjective("kills")).thenReturn(objective);
            return objective;
        });
        Map<String, Score> boardScores = scores.computeIfAbsent(board, key -> new HashMap<>());
        when(objective.getScore(anyString()))
                .thenAnswer(invocation -> boardScores.computeIfAbsent(invocation.getArgument(0), name -> mock(Score.class)));
        return board;
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn(name);
        online.put(id, player);
        playerBoards.put(id, board());
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

        teams.awardPoint(shooter, target, HitZone.BODY);
        teams.awardPoint(shooter, target, HitZone.BODY);

        assertEquals(2, teams.getKills(shooter));
        assertEquals(0, teams.getKills(target));
        assertEquals(2, teams.getTeamScore("RED"));
        assertEquals(0, teams.getTeamScore("BLUE"));
        // 처치 수는 모든 참가자의 개인 스코어보드 탭 목록에 똑같이 표시된다.
        for (Scoreboard board : List.of(playerBoards.get(first.getUniqueId()), playerBoards.get(second.getUniqueId()))) {
            verify(scores.get(board).get(shooter.getName())).setScore(2);
        }

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

        teams.awardPoint(first, second, HitZone.HEAD);

        assertEquals(1, events.size());
        GameEvent.PointScored event = events.getFirst();
        assertSame(first, event.shooter());
        assertEquals(firstTeam, event.shooterTeam());
        assertSame(second, event.target());
        assertEquals(HitZone.HEAD, event.zone());
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
    void teamsAreRegisteredOnEachParticipantsOwnBoardWithoutSwappingScoreboards() {
        Player first = player("first");
        Player second = player("second");
        teams.assign(List.of(first.getUniqueId(), second.getUniqueId()));

        for (Player viewer : List.of(first, second)) {
            Scoreboard board = playerBoards.get(viewer.getUniqueId());
            verify(board).registerNewTeam("RED");
            verify(board).registerNewTeam("BLUE");
            // 두 참가자 모두 각자의 스코어보드에서 팀에 들어가 있어야 이름표 숨김이 동작한다.
            Team red = board.getTeam("RED");
            Team blue = board.getTeam("BLUE");
            for (Player member : List.of(first, second)) {
                Team expected = "RED".equals(teams.getTeamOf(member)) ? red : blue;
                verify(expected).addEntry(member.getName());
            }
            verify(viewer, never()).setScoreboard(any());
        }
    }

    @Test
    void clearRemovesOnlyTeamDisplayFromPersonalBoards() {
        Player first = player("first");
        Player second = player("second");
        teams.assign(List.of(first.getUniqueId(), second.getUniqueId()));
        Scoreboard board = playerBoards.get(first.getUniqueId());
        Team red = board.getTeam("RED");
        Objective kills = board.getObjective("kills");

        teams.clear();

        verify(red).unregister();
        verify(kills).unregister();
        verify(board, never()).clearSlot(any());
    }

    @Test
    void removedPlayerIsTakenOffTheOtherParticipantsBoards() {
        Player first = player("first");
        Player second = player("second");
        Player third = player("third");
        teams.assign(List.of(first.getUniqueId(), second.getUniqueId(), third.getUniqueId()));
        Scoreboard secondBoard = playerBoards.get(second.getUniqueId());
        Team firstTeam = secondBoard.getTeam(teams.getTeamOf(first));
        when(secondBoard.getEntryTeam("first")).thenReturn(firstTeam);
        Team ownRed = playerBoards.get(first.getUniqueId()).getTeam("RED");

        teams.remove(first);

        verify(firstTeam).removeEntry("first");
        verify(scores.get(secondBoard).get("first")).resetScore();
        verify(ownRed).unregister();
        assertNull(teams.getTeamOf(first));
    }

    @Test
    void colorOfUsesTeamConstants() {
        assertEquals(NamedTextColor.RED, GameTeams.colorOf("RED"));
        assertEquals(GameTeams.BLUE_COLOR, GameTeams.colorOf("BLUE"));
    }
}
