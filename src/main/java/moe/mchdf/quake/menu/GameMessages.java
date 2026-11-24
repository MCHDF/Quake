package moe.mchdf.quake.menu;

import moe.mchdf.quake.game.JoinResult;
import moe.mchdf.quake.game.LeaveResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/** 게임 로직이 돌려준 결과를 채팅 안내 문구로 바꾼다. */
public final class GameMessages {
    private GameMessages() {
    }

    public static Component join(JoinResult result) {
        return switch (result) {
            case NO_READY_WORLD -> Component.text("로테이션에 준비된 게임 맵이 없어 참여할 수 없습니다.", NamedTextColor.RED);
            case ALREADY_IN_GAME -> Component.text("이미 현재 게임에 참여 중입니다.", NamedTextColor.YELLOW);
            case QUEUED_FOR_NEXT_GAME -> Component.text(
                    "다음 게임 대기열에 등록했습니다. 현재 게임 종료 후 자동으로 참여합니다.", NamedTextColor.GREEN);
            case ALREADY_QUEUED -> Component.text("이미 다음 게임 대기열에 등록되어 있습니다.", NamedTextColor.GREEN);
            case JOINED -> Component.text("미니게임 참여 대기 명단에 등록했습니다.", NamedTextColor.GREEN, TextDecoration.BOLD);
            case ALREADY_WAITING -> Component.text("이미 참여 대기 중입니다.", NamedTextColor.GREEN, TextDecoration.BOLD);
        };
    }

    public static Component leave(LeaveResult result) {
        return switch (result) {
            case GAME_IN_PROGRESS -> Component.text("게임 진행 중에는 참여를 취소할 수 없습니다.", NamedTextColor.RED);
            case LEFT -> Component.text("미니게임 참여를 취소했습니다.", NamedTextColor.YELLOW, TextDecoration.BOLD);
            case NOT_JOINED -> Component.text("참여 대기 중이 아닙니다.", NamedTextColor.YELLOW, TextDecoration.BOLD);
        };
    }
}
