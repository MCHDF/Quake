package moe.mchdf.quake.game;

/** {@link GameSession#join}의 처리 결과. 안내 문구는 화면 쪽 코드가 이 값을 보고 정한다. */
public enum JoinResult {
    /** 로테이션에 준비된 게임 월드가 없어 참여할 수 없음 */
    NO_READY_WORLD,
    /** 이미 진행 중인 게임의 참가자 */
    ALREADY_IN_GAME,
    /** 게임 진행 중이라 다음 게임 대기열에 등록함 */
    QUEUED_FOR_NEXT_GAME,
    /** 이미 다음 게임 대기열에 있음 */
    ALREADY_QUEUED,
    /** 참여 대기 명단에 등록함 */
    JOINED,
    /** 이미 참여 대기 명단에 있음 */
    ALREADY_WAITING
}
