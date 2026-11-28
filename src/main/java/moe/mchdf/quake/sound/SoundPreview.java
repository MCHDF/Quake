package moe.mchdf.quake.sound;

import moe.mchdf.quake.settings.PlayerSettings;
import moe.mchdf.quake.settings.PlayerSettingsListener;
import moe.mchdf.quake.settings.SoundType;
import org.bukkit.entity.Player;

/**
 * 효과음 음량을 바꾸면 그 소리를 새 음량으로 한 번 들려준다. 바닐라 설정 화면과 모드 화면 모두
 * 설정 변경 알림을 거치므로 어느 쪽에서 바꿔도 같다.
 */
public final class SoundPreview implements PlayerSettingsListener {
    private final QuakeSounds sounds;

    public SoundPreview(QuakeSounds sounds) {
        this.sounds = sounds;
    }

    @Override
    public void changed(Player player, PlayerSettings before, PlayerSettings after) {
        for (SoundType type : SoundType.values()) {
            if (before.sound(type) != after.sound(type)) {
                sounds.preview(player, type, after.sound(type));
            }
        }
    }
}
