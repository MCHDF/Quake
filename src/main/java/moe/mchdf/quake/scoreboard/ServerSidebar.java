package moe.mchdf.quake.scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import moe.mchdf.quake.game.GameEvent;
import moe.mchdf.quake.game.GameListener;
import moe.mchdf.quake.game.GameSession;
import moe.mchdf.quake.mod.ModSidebar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 화면 오른쪽 사이드바. 로비에서는 서버 정보, 게임 참가자에게는 팀 점수와 남은 시간을 보여 준다.
 *
 * <p>게임 이벤트를 구독해 점수 변화를 바로 반영하고, 접속 인원처럼 이벤트가 없는 정보는 1초마다 갱신한다.
 * 제목과 아래쪽 주소 줄은 config.yml의 {@code sidebar.title}, {@code sidebar.address}(MiniMessage)로 정한다.
 *
 * <p>Quake 모드를 쓰는 플레이어에게는 바닐라 사이드바 대신 같은 제목·줄을 패킷으로 보내
 * 모드가 반투명 화면으로 그리게 한다({@link ModSidebar}).
 * 모드 핸드셰이크는 접속 직후에 도착하므로, 접속하자마자 그리지 않고 첫 1초 갱신 때 그려서
 * 모드 사용자에게 바닐라 사이드바가 잠깐 보였다 사라지지 않게 한다.
 */
public final class ServerSidebar implements GameListener, Listener {
    static final String TITLE_KEY = "sidebar.title";
    static final String ADDRESS_KEY = "sidebar.address";
    static final String DEFAULT_TITLE = "<gold><bold>QUAKE</bold></gold>";
    private static final long REFRESH_TICKS = 20L;

    private final GameSession game;
    private final PlayerBoards boards;
    private final ModSidebar modSidebar;
    private final Map<UUID, Sidebar> sidebars = new HashMap<>();
    private final Component title;
    private final Component address;
    private GameEvent.Running lastRunning;

    public ServerSidebar(GameSession game, PlayerBoards boards, ModSidebar modSidebar, FileConfiguration config) {
        this.game = game;
        this.boards = boards;
        this.modSidebar = modSidebar;
        MiniMessage miniMessage = MiniMessage.miniMessage();
        title = miniMessage.deserialize(config.getString(TITLE_KEY, DEFAULT_TITLE));
        String addressText = config.getString(ADDRESS_KEY, "");
        address = addressText.isBlank() ? null : miniMessage.deserialize(addressText);
    }

    /** config.yml에 사이드바 설정 항목이 없으면 기본값을 넣는다. 저장은 호출한 쪽에서 한다. */
    public static void addDefaults(FileConfiguration config) {
        config.addDefault(TITLE_KEY, DEFAULT_TITLE);
        config.addDefault(ADDRESS_KEY, "");
        config.options().copyDefaults(true);
    }

    public void start(JavaPlugin plugin) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshAll, REFRESH_TICKS, REFRESH_TICKS);
    }

    @Override
    public void onGameEvent(GameEvent event) {
        switch (event) {
            case GameEvent.Running running -> {
                lastRunning = running;
                refreshAll();
            }
            case GameEvent.TeamAssigned assigned -> refresh(assigned.player());
            case GameEvent.GameFinished finished -> {
                lastRunning = null;
                refresh(finished.player());
            }
            default -> {
                // 대기·카운트다운 정보는 1초 갱신으로 충분하다.
            }
        }
    }

    /** 모드 핸드셰이크가 수락되면 호출한다. 바닐라 사이드바를 지우고 모드 화면으로 바로 보낸다. */
    public void onModAccepted(Player player) {
        refresh(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sidebars.remove(event.getPlayer().getUniqueId());
    }

    void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    void refresh(Player player) {
        if (!player.isOnline()) {
            return;
        }
        if (modSidebar.usesModUi(player)) {
            Sidebar removed = sidebars.remove(player.getUniqueId());
            if (removed != null) {
                removed.remove();
            }
            modSidebar.show(player, title, contentFor(player));
            return;
        }
        var board = boards.boardOf(player);
        Sidebar sidebar = sidebars.get(player.getUniqueId());
        if (sidebar == null || sidebar.board() != board) {
            sidebar = new Sidebar(board);
            sidebars.put(player.getUniqueId(), sidebar);
        }
        sidebar.update(title, contentFor(player).vanillaLines());
    }

    private SidebarContent contentFor(Player player) {
        String team = game.getGameTeams().getTeamOf(player);
        if (game.isPlaying(player) && team != null && lastRunning != null) {
            return SidebarLines.game(team, lastRunning.redScore(), lastRunning.blueScore(),
                    lastRunning.targetScore(), lastRunning.remainingSeconds(), lastRunning.totalSeconds(), address);
        }
        return SidebarLines.lobby(Bukkit.getOnlinePlayers().size(), game.lobbyStatus(player), address);
    }
}
