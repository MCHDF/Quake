package moe.mchdf.quake.scoreboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import moe.mchdf.quake.game.GameState;
import moe.mchdf.quake.game.LobbyStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class SidebarTest {
    private final Map<String, Score> scores = new HashMap<>();
    private Scoreboard board;
    private Objective objective;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        // Criteria 상수는 클래스 초기화 때 Bukkit을 호출하므로 Bukkit을 먼저 가짜로 바꿔 둔다.
        bukkit = mockStatic(Bukkit.class);
        Criteria dummy = Criteria.DUMMY;
        board = mock(Scoreboard.class);
        objective = mock(Objective.class);
        when(board.registerNewObjective(eq(Sidebar.OBJECTIVE_NAME), eq(dummy), any(Component.class)))
                .thenAnswer(invocation -> {
                    when(board.getObjective(Sidebar.OBJECTIVE_NAME)).thenReturn(objective);
                    return objective;
                });
        when(objective.getScore(anyString()))
                .thenAnswer(invocation -> scores.computeIfAbsent(invocation.getArgument(0), name -> mock(Score.class)));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private static List<String> plain(List<Component> lines) {
        return lines.stream().map(SidebarTest::plain).toList();
    }

    @Test
    void firstUpdateCreatesSidebarWithHiddenNumbersAndOrderedLines() {
        Sidebar sidebar = new Sidebar(board);
        sidebar.update(Component.text("QUAKE"), List.of(Component.text("a"), Component.text("b")));

        verify(objective).setDisplaySlot(DisplaySlot.SIDEBAR);
        verify(objective).numberFormat(NumberFormat.blank());
        // 위쪽 줄일수록 점수가 커야 사이드바에서 위에 표시된다.
        verify(scores.get("#sidebar0")).setScore(2);
        verify(scores.get("#sidebar1")).setScore(1);
        verify(scores.get("#sidebar0")).customName(Component.text("a"));
    }

    @Test
    void blankLinesAreSentAsSpace() {
        Sidebar sidebar = new Sidebar(board);
        sidebar.update(Component.text("QUAKE"), List.of(Component.empty()));

        verify(scores.get("#sidebar0")).customName(Component.space());
    }

    @Test
    void onlyChangedLinesAreSentAgain() {
        Sidebar sidebar = new Sidebar(board);
        sidebar.update(Component.text("QUAKE"), List.of(Component.text("a"), Component.text("b")));
        clearInvocations(scores.values().toArray());

        sidebar.update(Component.text("QUAKE"), List.of(Component.text("a"), Component.text("c")));

        verifyNoInteractions(scores.get("#sidebar0"));
        verify(scores.get("#sidebar1")).customName(Component.text("c"));
        verify(objective, never()).displayName(any());
    }

    @Test
    void shrinkingRemovesExtraLinesAndRenumbers() {
        Sidebar sidebar = new Sidebar(board);
        sidebar.update(Component.text("QUAKE"),
                List.of(Component.text("a"), Component.text("b"), Component.text("c")));

        sidebar.update(Component.text("QUAKE"), List.of(Component.text("a")));

        verify(board).resetScores("#sidebar2");
        verify(board).resetScores("#sidebar1");
        verify(scores.get("#sidebar0")).setScore(1);
    }

    @Test
    void lobbyLinesShowServerInfoAndMyParticipation() {
        var status = new LobbyStatus(GameState.WAITING, LobbyStatus.Participation.WAITING, true,
                new LobbyStatus.WorldSummary("arena", 2, 16), 3);

        List<String> lines = plain(SidebarLines.lobby(5, status, Component.text("play.example.com")));

        assertEquals(List.of("", "접속 인원: 5명", "", "다음 맵: arena", "참여 인원: 3명 (최소 2)",
                "내 상태: 참여 대기", "", "play.example.com"), lines);
    }

    @Test
    void lobbyLinesWithoutWorldOrAddress() {
        var status = new LobbyStatus(GameState.WAITING, LobbyStatus.Participation.NONE, false, null, 0);

        List<String> lines = plain(SidebarLines.lobby(1, status, null));

        assertEquals(List.of("", "접속 인원: 1명", "", "맵: 준비된 맵 없음", "내 상태: 미참여"), lines);
    }

    @Test
    void lobbyLinesWhileAnotherGameIsRunning() {
        var status = new LobbyStatus(GameState.RUNNING, LobbyStatus.Participation.QUEUED, true,
                new LobbyStatus.WorldSummary("arena", 2, 16), 4);

        List<String> lines = plain(SidebarLines.lobby(6, status, null));

        assertTrue(lines.contains("진행 중: arena"));
        assertTrue(lines.contains("게임 인원: 4명"));
        assertTrue(lines.contains("내 상태: 다음 게임 대기"));
    }

    @Test
    void gameLinesShowMyTeamScoresAndTime() {
        List<String> lines = plain(SidebarLines.game("BLUE", 3, 2, 5, 151, null));

        assertEquals(List.of("", "내 팀: BLUE", "", "RED 3점", "BLUE 2점", "목표: 5점", "", "남은 시간: 02:31"),
                lines);
    }
}
