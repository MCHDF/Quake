package moe.mchdf.quake.game;

/** {@link GameEvent} 구독자. 보스바, 채팅 알림, (예정) 모드 패킷 전송이 각각 하나의 구독자가 된다. */
@FunctionalInterface
public interface GameListener {
    void onGameEvent(GameEvent event);
}
