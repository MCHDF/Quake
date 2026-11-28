package moe.mchdf.quake.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PlayerSettingsTest {
    @Test
    void everySoundStartsAtOriginalVolume() {
        for (SoundType type : SoundType.values()) {
            assertEquals(SoundLevel.HIGH, PlayerSettings.DEFAULTS.sound(type));
        }
    }

    @Test
    void valuesRoundTripIncludingSounds() {
        PlayerSettings settings = new PlayerSettings(false, true)
                .withSound(SoundType.KILL, SoundLevel.OFF)
                .withSound(SoundType.OTHER_SHOT, SoundLevel.LOW);

        assertEquals(settings, PlayerSettings.fromValues(settings.values()));
    }

    @Test
    void unreadableSoundFallsBackToDefault() {
        PlayerSettings settings = PlayerSettings.fromValues(Map.of("sound.hit", "loud", "sound.kill", "medium"));

        assertEquals(SoundLevel.HIGH, settings.sound(SoundType.HIT_CONFIRM));
        assertEquals(SoundLevel.MEDIUM, settings.sound(SoundType.KILL));
    }

    @Test
    void withAcceptsOnlyKnownLevels() {
        assertEquals(SoundLevel.OFF,
                PlayerSettings.DEFAULTS.with("sound.own_shot", "off").orElseThrow().sound(SoundType.OWN_SHOT));
        assertTrue(PlayerSettings.DEFAULTS.with("sound.own_shot", "HIGH").isEmpty());
    }

    @Test
    void levelsCycleInBothDirections() {
        assertEquals(SoundLevel.OFF, SoundLevel.HIGH.next());
        assertEquals(SoundLevel.HIGH, SoundLevel.OFF.previous());
        assertEquals(SoundLevel.MEDIUM, SoundLevel.LOW.next());
    }
}
