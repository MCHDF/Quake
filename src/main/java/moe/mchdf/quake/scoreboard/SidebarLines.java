package moe.mchdf.quake.scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import moe.mchdf.quake.game.GameTeams;
import moe.mchdf.quake.game.LobbyStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/** 사이드바에 표시할 줄을 만든다. 화면 갱신 없이 문구만 정하므로 서버 없이 테스트할 수 있다. */
final class SidebarLines {
    private SidebarLines() {
    }

    /** 로비(게임에 참가하지 않은 플레이어)용 서버 정보 */
    static List<Component> lobby(int onlinePlayers, LobbyStatus status, Component address) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(labeled("접속 인원", Component.text(onlinePlayers + "명", NamedTextColor.WHITE)));
        lines.add(Component.empty());
        if (status.world() == null) {
            lines.add(labeled("맵", Component.text("준비된 맵 없음", NamedTextColor.RED)));
        } else if (status.isRunning()) {
            lines.add(labeled("진행 중", Component.text(status.world().displayName(), NamedTextColor.WHITE)));
            lines.add(labeled("게임 인원", Component.text(status.participantCount() + "명", NamedTextColor.WHITE)));
        } else {
            lines.add(labeled("다음 맵", Component.text(status.world().displayName(), NamedTextColor.WHITE)));
            lines.add(labeled("참여 인원", Component.text(status.participantCount() + "명", NamedTextColor.WHITE)
                    .append(Component.text(" (최소 " + status.world().minPlayers() + ")", NamedTextColor.GRAY))));
        }
        lines.add(labeled("내 상태", participation(status.participation())));
        addFooter(lines, address);
        return lines;
    }

    /** 게임 참가자용 게임 정보 */
    static List<Component> game(String myTeam, int redScore, int blueScore, int targetScore,
                                int remainingSeconds, Component address) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(labeled("내 팀", Component.text(myTeam, GameTeams.colorOf(myTeam))));
        lines.add(Component.empty());
        lines.add(teamScore("RED", redScore));
        lines.add(teamScore("BLUE", blueScore));
        lines.add(labeled("목표", Component.text(targetScore + "점", NamedTextColor.WHITE)));
        lines.add(Component.empty());
        lines.add(labeled("남은 시간", Component.text(time(remainingSeconds), NamedTextColor.WHITE)));
        addFooter(lines, address);
        return lines;
    }

    static String time(int seconds) {
        int safe = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", safe / 60, safe % 60);
    }

    private static Component participation(LobbyStatus.Participation participation) {
        return switch (participation) {
            case NONE -> Component.text("미참여", NamedTextColor.GRAY);
            case WAITING -> Component.text("참여 대기", NamedTextColor.GREEN);
            case QUEUED -> Component.text("다음 게임 대기", NamedTextColor.YELLOW);
            case PLAYING -> Component.text("게임 중", NamedTextColor.GOLD);
        };
    }

    private static Component teamScore(String team, int score) {
        TextColor color = GameTeams.colorOf(team);
        return Component.text(team + " ", color).append(Component.text(score + "점", NamedTextColor.WHITE));
    }

    private static Component labeled(String label, Component value) {
        return Component.text(label + ": ", NamedTextColor.GRAY).append(value);
    }

    private static void addFooter(List<Component> lines, Component address) {
        if (address != null) {
            lines.add(Component.empty());
            lines.add(address);
        }
    }
}
