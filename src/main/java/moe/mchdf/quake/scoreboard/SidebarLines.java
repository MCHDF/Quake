package moe.mchdf.quake.scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import moe.mchdf.quake.game.GameTeams;
import moe.mchdf.quake.game.LobbyStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * 사이드바 내용을 만든다. 화면 갱신 없이 문구만 정하므로 서버 없이 테스트할 수 있다.
 *
 * <p>사이드바 배경이 어두워 회색 글씨는 잘 보이지 않으므로 쓰지 않는다. 값은 노란색이고, 항목 이름은 색을 정하지 않아
 * 바닐라는 흰색, 모드는 자기 테마 색으로 그린다.
 */
final class SidebarLines {
    private static final NamedTextColor VALUE = NamedTextColor.YELLOW;
    private static final NamedTextColor NOTE = NamedTextColor.WHITE;
    private static final NamedTextColor JOIN_BAR = NamedTextColor.GREEN;
    private static final NamedTextColor TIME_BAR = NamedTextColor.GOLD;

    private SidebarLines() {
    }

    /** 로비(게임에 참가하지 않은 플레이어)용 서버 정보 */
    static SidebarContent lobby(int onlinePlayers, LobbyStatus status, Component address) {
        List<SidebarRow> rows = new ArrayList<>();
        rows.add(value("접속 인원", Component.text(onlinePlayers + "명", VALUE)));
        rows.add(new SidebarRow.Spacer());
        if (status.world() == null) {
            rows.add(value("맵", Component.text("준비된 맵 없음", NamedTextColor.RED)));
        } else if (status.isRunning()) {
            rows.add(value("진행 중", Component.text(status.world().displayName(), VALUE)));
            rows.add(value("게임 인원", Component.text(status.participantCount() + "명", VALUE)));
        } else {
            int minimum = status.world().minPlayers();
            rows.add(value("다음 맵", Component.text(status.world().displayName(), VALUE)));
            // 막대는 게임 시작까지 모인 정도(최소 인원 대비)를 보여 준다.
            rows.add(new SidebarRow.Bar(Component.text("참여 인원"),
                    Component.text(status.participantCount() + "명", VALUE)
                            .append(Component.text(" (최소 " + minimum + ")", NOTE)),
                    status.participantCount() / (float) minimum, JOIN_BAR));
        }
        rows.add(value("내 상태", participation(status.participation())));
        addFooter(rows, address);
        return new SidebarContent(null, rows);
    }

    /** 게임 참가자용 게임 정보. 강조색은 내 팀 색이다. */
    static SidebarContent game(String myTeam, int redScore, int blueScore, int targetScore,
                               int remainingSeconds, int totalSeconds, Component address) {
        List<SidebarRow> rows = new ArrayList<>();
        rows.add(value("내 팀", Component.text(myTeam, GameTeams.colorOf(myTeam))));
        rows.add(new SidebarRow.Spacer());
        rows.add(teamScore("RED", redScore, targetScore));
        rows.add(teamScore("BLUE", blueScore, targetScore));
        rows.add(value("목표", Component.text(targetScore + "점", VALUE)));
        rows.add(new SidebarRow.Spacer());
        rows.add(new SidebarRow.Bar(Component.text("남은 시간"), Component.text(time(remainingSeconds), VALUE),
                totalSeconds <= 0 ? 0f : remainingSeconds / (float) totalSeconds, TIME_BAR));
        addFooter(rows, address);
        return new SidebarContent(GameTeams.colorOf(myTeam), rows);
    }

    static String time(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", safe / 60, safe % 60);
    }

    private static Component participation(LobbyStatus.Participation participation) {
        return switch (participation) {
            case NONE -> Component.text("미참여", NOTE);
            case WAITING -> Component.text("참여 대기", NamedTextColor.GREEN);
            case QUEUED -> Component.text("다음 게임 대기", NamedTextColor.YELLOW);
            case PLAYING -> Component.text("게임 중", NamedTextColor.GOLD);
        };
    }

    private static SidebarRow teamScore(String team, int score, int targetScore) {
        NamedTextColor color = GameTeams.colorOf(team);
        return new SidebarRow.Bar(Component.text(team, color), Component.text(score + "점", VALUE),
                targetScore <= 0 ? 0f : score / (float) targetScore, color);
    }

    private static SidebarRow value(String label, Component value) {
        return new SidebarRow.Value(Component.text(label), value);
    }

    private static void addFooter(List<SidebarRow> rows, Component address) {
        if (address != null) {
            rows.add(new SidebarRow.Spacer());
            rows.add(new SidebarRow.Text(address, true));
        }
    }
}
