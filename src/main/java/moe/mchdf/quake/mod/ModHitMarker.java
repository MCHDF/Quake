package moe.mchdf.quake.mod;

import moe.mchdf.quake.combat.HitZone;
import moe.mchdf.quake.game.GameEvent;
import moe.mchdf.quake.game.GameListener;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 모드 사용자가 상대를 맞히면 맞은 부위를 패킷({@link ModProtocol#HIT_CHANNEL})으로 보냅니다.
 * 판정은 서버가 하고, 모드는 받은 결과로 히트마커를 그리기만 합니다.
 *
 * <p>게임에서는 득점 이벤트({@link GameEvent.PointScored})를 구독해 쏜 사람에게만 보내고,
 * 점수와 상관없는 테스트 레일건은 {@link #show}를 직접 호출합니다.
 */
public final class ModHitMarker implements GameListener {
    // 맞은 부위 번호. 모드의 HitS2CPayload와 같아야 한다.
    static final byte ZONE_BODY = 0;
    static final byte ZONE_HEAD = 1;

    private final Plugin plugin;
    private final ModHandshake handshake;

    public ModHitMarker(Plugin plugin, ModHandshake handshake) {
        this.plugin = plugin;
        this.handshake = handshake;
    }

    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, ModProtocol.HIT_CHANNEL);
    }

    @Override
    public void onGameEvent(GameEvent event) {
        if (event instanceof GameEvent.PointScored scored) {
            show(scored.shooter(), scored.zone());
        }
    }

    /** 모드를 쓰는 {@code shooter}에게 맞힌 부위를 알린다. 모드가 없으면 아무것도 하지 않는다. */
    public void show(Player shooter, HitZone zone) {
        if (shooter == null || !handshake.hasMod(shooter)
                || !shooter.getListeningPluginChannels().contains(ModProtocol.HIT_CHANNEL)) {
            return;
        }
        shooter.sendPluginMessage(plugin, ModProtocol.HIT_CHANNEL, encode(zone));
    }

    /** 형식: byte 맞은 부위 ({@link #ZONE_BODY}, {@link #ZONE_HEAD}) */
    static byte[] encode(HitZone zone) {
        return new byte[] {zone == HitZone.HEAD ? ZONE_HEAD : ZONE_BODY};
    }
}
