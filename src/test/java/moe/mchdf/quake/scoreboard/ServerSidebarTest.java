package moe.mchdf.quake.scoreboard;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.game.GameState;
import moe.mchdf.quake.game.GameTeams;
import moe.mchdf.quake.game.LobbyStatus;
import moe.mchdf.quake.mod.ModSidebar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class ServerSidebarTest {
    private final Set<UUID> modUsers = new HashSet<>();
    private MockedStatic<Bukkit> bukkit;
    private Player player;
    private Scoreboard board;
    private Objective objective;
    private ModSidebar modSidebar;
    private ServerSidebar sidebar;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        // Criteria 상수는 클래스 초기화 때 Bukkit을 호출하므로 Bukkit을 먼저 가짜로 바꿔 둔다.
        bukkit = mockStatic(Bukkit.class);
        Criteria dummy = Criteria.DUMMY;
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn((Collection) List.of(player));

        board = mock(Scoreboard.class);
        objective = mock(Objective.class);
        when(objective.getScore(anyString())).thenAnswer(invocation -> mock(Score.class));
        when(board.registerNewObjective(eq(Sidebar.OBJECTIVE_NAME), eq(dummy), any(Component.class)))
                .thenAnswer(invocation -> {
                    when(board.getObjective(Sidebar.OBJECTIVE_NAME)).thenReturn(objective);
                    return objective;
                });
        PlayerBoards boards = mock(PlayerBoards.class);
        when(boards.boardOf(player)).thenReturn(board);

        GameSession game = mock(GameSession.class);
        when(game.getGameTeams()).thenReturn(mock(GameTeams.class));
        when(game.lobbyStatus(player)).thenReturn(
                new LobbyStatus(GameState.WAITING, LobbyStatus.Participation.NONE, false, null, 0));

        modSidebar = mock(ModSidebar.class);
        when(modSidebar.usesModUi(any(Player.class))).thenAnswer(invocation ->
                modUsers.contains(((Player) invocation.getArgument(0)).getUniqueId()));

        sidebar = new ServerSidebar(game, boards, modSidebar, new YamlConfiguration());
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void vanillaPlayerSeesSidebar() {
        sidebar.refreshAll();

        verify(board).registerNewObjective(eq(Sidebar.OBJECTIVE_NAME), nullable(Criteria.class), any(Component.class));
        verify(modSidebar, never()).show(any(), any(), any());
    }

    @Test
    void modUserGetsSameContentThroughModInsteadOfVanillaSidebar() {
        modUsers.add(player.getUniqueId());

        sidebar.refreshAll();

        verify(board, never()).registerNewObjective(anyString(), nullable(Criteria.class), any(Component.class));
        SidebarContent expected = SidebarLines.lobby(1,
                new LobbyStatus(GameState.WAITING, LobbyStatus.Participation.NONE, false, null, 0), null);
        verify(modSidebar).show(eq(player), any(Component.class), eq(expected));
    }

    @Test
    void sidebarIsRemovedAsSoonAsModHandshakeIsAccepted() {
        sidebar.refreshAll();
        modUsers.add(player.getUniqueId());

        sidebar.onModAccepted(player);

        verify(objective).unregister();
        verify(modSidebar).show(eq(player), any(Component.class), any(SidebarContent.class));
        // 이후 1초 갱신에서도 다시 만들지 않는다.
        sidebar.refreshAll();
        verify(board, times(1)).registerNewObjective(anyString(), nullable(Criteria.class), any(Component.class));
    }
}
