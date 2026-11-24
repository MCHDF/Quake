package moe.mchdf.quake.game;

import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * 게임 로직이 발행하는 이벤트. 보스바·채팅·(예정) 모드 패킷 전송은 {@link GameListener}로 이 이벤트를 구독해 화면을 갱신한다.
 * 게임 로직은 화면을 직접 건드리지 않는다.
 */
public sealed interface GameEvent {

    /** 이 플레이어를 게임 화면(보스바 등) 표시 대상에 넣는다. */
    record PlayerShown(Player player) implements GameEvent {
    }

    /** 이 플레이어를 게임 화면 표시 대상에서 뺀다. */
    record PlayerHidden(Player player) implements GameEvent {
    }

    /** 월드 로테이션 대기 상태. {@code world}가 {@code null}이면 준비된 월드가 없다. */
    record Waiting(LobbyStatus.WorldSummary world, int participantCount, int remainingSeconds) implements GameEvent {
    }

    /** 게임 시작 카운트다운 (매초) */
    record Countdown(String worldName, int remainingSeconds, int totalSeconds, List<UUID> participants)
            implements GameEvent {
    }

    /** 게임 진행 상황 (매초, 그리고 점수가 바뀔 때) */
    record Running(int remainingSeconds, int totalSeconds, int redScore, int blueScore, int targetScore)
            implements GameEvent {
    }

    /** 게임 시작 시 팀 배정 */
    record TeamAssigned(Player player, String team) implements GameEvent {
    }

    /** 득점(명중). {@code recipients}는 알림을 받을 이번 게임 참가자 */
    record PointScored(Player shooter, String shooterTeam, Player target, String targetTeam, List<UUID> recipients)
            implements GameEvent {
    }

    /** 게임 월드 이동에 실패해 다음 월드로 넘어감. {@code applicants}는 영향을 받은 참여 대기자 */
    record TeleportFailed(List<UUID> applicants) implements GameEvent {
    }

    /** 이번 월드 정원을 넘어 다음 게임 대기열로 옮겨짐 */
    record NotAdmitted(Player player) implements GameEvent {
    }

    /** 게임 종료(플레이어별). {@code team}은 배정된 팀이며 없으면 {@code null} */
    record GameFinished(Player player, String team, int redScore, int blueScore, boolean cancelled)
            implements GameEvent {
    }

    /** 이전 게임이 끝나 다음 게임 대기 명단에 자동으로 들어감 */
    record AutoJoined(Player player, boolean previousGameCancelled) implements GameEvent {
    }
}
