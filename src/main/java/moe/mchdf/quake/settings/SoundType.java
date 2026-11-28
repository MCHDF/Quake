package moe.mchdf.quake.settings;

/** 플레이어가 음량을 따로 정하는 효과음 종류. 서버가 듣는 사람마다 따로 재생하므로 모드 없이도 조절된다. */
public enum SoundType {
    HIT_CONFIRM("sound.hit", "적중음", "내가 상대를 맞혔을 때 나는 확인음"),
    KILL("sound.kill", "처치음", "빔이 사람에게 꽂히는 충돌음 (맞은 사람과 주변도 들음)"),
    OWN_SHOT("sound.own_shot", "내 발사음", "내가 레일건을 쏠 때 나는 소리"),
    OTHER_SHOT("sound.other_shot", "다른 사람 발사음", "다른 플레이어가 레일건을 쏠 때 나는 소리"),
    COUNTDOWN("sound.countdown", "카운트다운", "게임 시작 5초 전부터 울리는 소리");

    private final String key;
    private final String label;
    private final String description;

    SoundType(String key, String label, String description) {
        this.key = key;
        this.label = label;
        this.description = description;
    }

    /** DB와 모드 패킷에 쓰는 항목 이름 */
    public String key() {
        return key;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
