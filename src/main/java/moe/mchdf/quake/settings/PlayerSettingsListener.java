package moe.mchdf.quake.settings;

import org.bukkit.entity.Player;

/** 플레이어 설정이 바뀌었을 때 받는다. 바닐라 화면과 모드 화면 어느 쪽에서 바꿨든 같다. */
@FunctionalInterface
public interface PlayerSettingsListener {
    void changed(Player player, PlayerSettings before, PlayerSettings after);
}
