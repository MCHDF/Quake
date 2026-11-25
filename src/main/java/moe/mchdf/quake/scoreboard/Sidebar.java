package moe.mchdf.quake.scoreboard;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * 스코어보드 하나의 사이드바. 줄마다 고정 항목({@code quake.sidebar.0}, {@code quake.sidebar.1} …)을 두고,
 * 화면에는 항목 이름 대신 {@link Score#customName}을 보여 준다. 오른쪽 숫자는 숨긴다.
 *
 * <p>바뀐 줄만 다시 보내서 매초 갱신해도 패킷이 거의 나가지 않는다.
 */
final class Sidebar {
    static final String OBJECTIVE_NAME = "quake_sidebar";
    // '#'으로 시작하는 항목은 바닐라 클라이언트가 사이드바에서 숨기므로 쓰면 안 된다.
    // '.'은 플레이어 이름에 쓸 수 없어 실제 플레이어 이름과 겹치지 않는다.
    static final String ENTRY_PREFIX = "quake.sidebar.";

    private final Scoreboard board;
    private final List<Component> shown = new ArrayList<>();
    private Objective objective;
    private Component title;

    Sidebar(Scoreboard board) {
        this.board = board;
    }

    Scoreboard board() {
        return board;
    }

    void update(Component newTitle, List<Component> lines) {
        if (objective == null || board.getObjective(OBJECTIVE_NAME) == null) {
            objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, newTitle);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            objective.numberFormat(NumberFormat.blank());
            title = newTitle;
            shown.clear();
        } else if (!newTitle.equals(title)) {
            objective.displayName(newTitle);
            title = newTitle;
        }

        // 줄 순서는 점수로 정해지므로, 줄 수가 바뀌면 모든 줄의 점수를 다시 매긴다.
        boolean resized = lines.size() != shown.size();
        for (int i = shown.size() - 1; i >= lines.size(); i--) {
            board.resetScores(entry(i));
        }
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            if (!resized && line.equals(shown.get(i))) {
                continue;
            }
            Score score = objective.getScore(entry(i));
            score.setScore(lines.size() - i);
            // 빈 이름이면 클라이언트가 항목 이름(quake.sidebar.0)을 보여 줄 수 있어 공백 한 칸으로 보낸다.
            score.customName(line.equals(Component.empty()) ? Component.space() : line);
        }
        shown.clear();
        shown.addAll(lines);
    }

    /** 사이드바를 스코어보드에서 지운다. 다시 {@link #update}하면 새로 만든다. */
    void remove() {
        Objective registered = board.getObjective(OBJECTIVE_NAME);
        if (registered != null) {
            registered.unregister();
        }
        objective = null;
        title = null;
        shown.clear();
    }

    private static String entry(int index) {
        return ENTRY_PREFIX + index;
    }
}
