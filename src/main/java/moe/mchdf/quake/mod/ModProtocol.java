package moe.mchdf.quake.mod;

/**
 * 클라이언트 모드와 함께 쓰는 프로토콜 정보입니다.
 * 모드의 {@code moe.mchdf.quake.network.QuakeProtocol}과 값이 같아야 합니다.
 */
public final class ModProtocol {
    /** 패킷 형식이 바뀌면 모드와 함께 올립니다. */
    public static final int VERSION = 3;

    /** 핸드셰이크 채널. 클라 → 서버: int 모드 버전 / 서버 → 클라: int 서버 버전, boolean 수락 여부. */
    public static final String HANDSHAKE_CHANNEL = "quake:handshake";

    /**
     * 사이드바 채널. 서버 → 클라: 제목, 강조색, 줄 목록(구분선·글자·값·막대). 형식은 {@link ModSidebar#encode}를 봅니다.
     * 문자열은 VarInt 바이트 길이 + UTF-8(마인크래프트 {@code STRING_UTF8}), 글자는 텍스트 컴포넌트 JSON입니다.
     */
    public static final String SIDEBAR_CHANNEL = "quake:sidebar";

    /** 명중 채널. 서버 → 클라(쏜 사람): byte 맞은 부위(0 몸통, 1 머리). 형식은 {@link ModHitMarker#encode}를 봅니다. */
    public static final String HIT_CHANNEL = "quake:hit";

    private ModProtocol() {
    }
}
