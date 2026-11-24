package moe.mchdf.quake.game;

/**
 * 한 플레이어 기준의 로비/게임 상태 스냅샷.
 * 참여 메뉴(Dialog)와 클라이언트 모드가 같은 데이터로 화면을 그리도록 한 번에 묶어서 제공한다.
 *
 * @param state            현재 게임 진행 상태
 * @param participation    이 플레이어의 참여 상태
 * @param hasReadyWorlds   로테이션에 준비된 게임 월드가 있는지
 * @param world            현재 로테이션 월드. 없으면 {@code null}
 * @param participantCount 현재 참여 대기 명단(게임 중에는 참가자) 인원
 */
public record LobbyStatus(GameState state, Participation participation, boolean hasReadyWorlds,
                          WorldSummary world, int participantCount) {

    /** 플레이어 한 명의 참여 상태 */
    public enum Participation {
        /** 참여하지 않음 */
        NONE,
        /** 게임 시작 전 참여 대기 명단에 있음 */
        WAITING,
        /** 게임 진행 중이라 다음 게임 대기열에 있음 */
        QUEUED,
        /** 진행 중인 게임의 참가자 */
        PLAYING
    }

    /** 화면 표시에 필요한 현재 월드 정보만 담는다. */
    public record WorldSummary(String displayName, int minPlayers, int maxPlayers) {
    }

    public boolean isRunning() {
        return state == GameState.RUNNING;
    }
}
