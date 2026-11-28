package moe.mchdf.quake.settings;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 플레이어 한 명의 개인 설정. 모드 없이도 서버가 적용할 수 있는 항목만 둔다.
 *
 * <p>DB에는 항목마다 키-값 한 줄로 저장해, 항목이 늘어나도 테이블을 바꾸지 않는다.
 * 저장된 값이 없거나 읽을 수 없으면 기본값을 쓴다.
 *
 * @param sidebar 사이드바 표시. 끄면 바닐라 사이드바와 모드 패널을 모두 숨긴다.
 * @param autoRejoin 게임이 끝나면 다음 게임에 자동으로 다시 참여
 */
public record PlayerSettings(boolean sidebar, boolean autoRejoin) {
    public static final PlayerSettings DEFAULTS = new PlayerSettings(true, true);

    static final String SIDEBAR = "sidebar";
    static final String AUTO_REJOIN = "auto_rejoin";

    public PlayerSettings withSidebar(boolean value) {
        return new PlayerSettings(value, autoRejoin);
    }

    public PlayerSettings withAutoRejoin(boolean value) {
        return new PlayerSettings(sidebar, value);
    }

    /**
     * 항목 하나를 문자열 값으로 바꾼다. 모드가 보낸 값처럼 밖에서 온 값을 받을 때 쓴다.
     * 모르는 항목이거나 그 항목에 맞지 않는 값이면 비어 있다.
     */
    public Optional<PlayerSettings> with(String key, String value) {
        Map<String, String> values = values();
        if (!values.containsKey(key)) {
            return Optional.empty();
        }
        values.put(key, value);
        PlayerSettings changed = fromValues(values);
        return value.equals(changed.values().get(key)) ? Optional.of(changed) : Optional.empty();
    }

    /** 저장하거나 모드에 보낼 키-값. 항목 이름은 모드와 같아야 한다. */
    public Map<String, String> values() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(SIDEBAR, Boolean.toString(sidebar));
        values.put(AUTO_REJOIN, Boolean.toString(autoRejoin));
        return values;
    }

    static PlayerSettings fromValues(Map<String, String> values) {
        return new PlayerSettings(
                bool(values.get(SIDEBAR), DEFAULTS.sidebar),
                bool(values.get(AUTO_REJOIN), DEFAULTS.autoRejoin));
    }

    private static boolean bool(String value, boolean fallback) {
        if ("true".equals(value)) {
            return true;
        }
        if ("false".equals(value)) {
            return false;
        }
        return fallback;
    }
}
