package moe.mchdf.quake.settings;

import java.util.Optional;

/** 효과음 한 종류의 음량 단계. 끄기와 3단계이며, 크게가 원래 음량이다. */
public enum SoundLevel {
    OFF("off", "끄기", 0.0F),
    LOW("low", "작게", 0.4F),
    MEDIUM("medium", "보통", 0.7F),
    HIGH("high", "크게", 1.0F);

    private final String id;
    private final String label;
    private final float multiplier;

    SoundLevel(String id, String label, float multiplier) {
        this.id = id;
        this.label = label;
        this.multiplier = multiplier;
    }

    /** DB와 모드 패킷에 쓰는 값 */
    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    /** 소리마다 정해 둔 음량에 곱하는 값 */
    public float multiplier() {
        return multiplier;
    }

    public SoundLevel next() {
        SoundLevel[] levels = values();
        return levels[(ordinal() + 1) % levels.length];
    }

    public SoundLevel previous() {
        SoundLevel[] levels = values();
        return levels[(ordinal() + levels.length - 1) % levels.length];
    }

    static Optional<SoundLevel> fromId(String id) {
        for (SoundLevel level : values()) {
            if (level.id.equals(id)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }
}
