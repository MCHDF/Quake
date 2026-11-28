package moe.mchdf.quake.sound;

import java.util.UUID;
import moe.mchdf.quake.game.GameEvent;
import moe.mchdf.quake.game.GameListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** 게임 시작 5초 전부터 참가자에게 1초마다 카운트다운 소리를 들려준다. 시작 순간에는 높은 소리 */
public final class CountdownSound implements GameListener {
    private static final int FROM_SECONDS = 5;

    private final QuakeSounds sounds;

    public CountdownSound(QuakeSounds sounds) {
        this.sounds = sounds;
    }

    @Override
    public void onGameEvent(GameEvent event) {
        if (!(event instanceof GameEvent.Countdown countdown) || countdown.remainingSeconds() > FROM_SECONDS) {
            return;
        }
        for (UUID id : countdown.participants()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                sounds.countdown(player, countdown.remainingSeconds() == 0);
            }
        }
    }
}
