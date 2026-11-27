package moe.mchdf.quake.scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;

/**
 * 플레이어마다 개인 스코어보드를 하나씩 준다.
 *
 * <p>사이드바 내용(내 참여 상태, 내 팀)이 플레이어마다 달라서 스코어보드를 공유할 수 없다.
 * 게임 팀 표시({@code GameTeams})도 새 스코어보드로 바꾸지 않고 이 개인 스코어보드에 팀을 등록한다.
 */
public final class PlayerBoards implements Listener {
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    /** 이 플레이어의 개인 스코어보드. 없으면 만들어서 적용한다. */
    public Scoreboard boardOf(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null || player.getScoreboard() != board) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            boards.put(player.getUniqueId(), board);
            player.setScoreboard(board);
        }
        return board;
    }

    /** 이미 만들어진 개인 스코어보드. 없으면 {@code null}이고 새로 만들지 않는다. */
    public Scoreboard find(UUID playerId) {
        return boards.get(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        boardOf(event.getPlayer());
    }

    // 퇴장 처리(게임 이탈 등)가 모두 끝난 뒤에 지운다.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        boards.remove(event.getPlayer().getUniqueId());
    }

    /** 플러그인 종료 시 모든 플레이어를 서버 기본 스코어보드로 되돌린다. */
    public void shutdown() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (UUID id : boards.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.setScoreboard(main);
            }
        }
        boards.clear();
    }
}
