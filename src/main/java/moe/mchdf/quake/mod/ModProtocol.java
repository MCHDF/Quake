package moe.mchdf.quake.mod;

/**
 * 클라이언트 모드와 함께 쓰는 프로토콜 정보입니다.
 * 모드의 {@code moe.mchdf.quake.network.QuakeProtocol}과 값이 같아야 합니다.
 */
public final class ModProtocol {
    /** 패킷 형식이 바뀌면 모드와 함께 올립니다. */
    public static final int VERSION = 1;

    /** 핸드셰이크 채널. 클라 → 서버: int 모드 버전 / 서버 → 클라: int 서버 버전, boolean 수락 여부. */
    public static final String HANDSHAKE_CHANNEL = "quake:handshake";

    private ModProtocol() {
    }
}
