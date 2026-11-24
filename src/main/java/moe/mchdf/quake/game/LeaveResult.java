package moe.mchdf.quake.game;

/** {@link GameSession#requestLeave}의 처리 결과. 안내 문구는 화면 쪽 코드가 이 값을 보고 정한다. */
public enum LeaveResult {
    /** 게임 진행 중인 참가자는 스스로 나갈 수 없음 */
    GAME_IN_PROGRESS,
    /** 참여 대기 명단 또는 다음 게임 대기열에서 빠짐 */
    LEFT,
    /** 대기 명단에도 대기열에도 없음 */
    NOT_JOINED
}
