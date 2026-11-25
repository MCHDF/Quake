package moe.mchdf.quake.mod;

/**
 * 클라이언트 모드와 함께 쓰는 프로토콜 정보입니다.
 * 모드의 {@code moe.mchdf.quake.network.QuakeProtocol}과 값이 같아야 합니다.
 */
public final class ModProtocol {
    /** 패킷 형식이 바뀌면 모드와 함께 올립니다. */
    public static final int VERSION = 2;

    /** 핸드셰이크 채널. 클라 → 서버: int 모드 버전 / 서버 → 클라: int 서버 버전, boolean 수락 여부. */
    public static final String HANDSHAKE_CHANNEL = "quake:handshake";

    /**
     * 사이드바 채널. 서버 → 클라: boolean 표시 여부, 문자열 제목, 문자열 목록 줄.
     * 문자열은 VarInt 바이트 길이 + UTF-8, 목록은 VarInt 개수 + 원소이며 제목과 줄은 텍스트 컴포넌트 JSON입니다.
     * (마인크래프트 {@code ByteBufCodecs.BOOL}, {@code STRING_UTF8}, {@code list()}와 같은 형식)
     */
    public static final String SIDEBAR_CHANNEL = "quake:sidebar";

    private ModProtocol() {
    }
}
