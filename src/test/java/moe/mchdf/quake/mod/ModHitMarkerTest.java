package moe.mchdf.quake.mod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import moe.mchdf.quake.combat.HitZone;
import moe.mchdf.quake.game.GameEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModHitMarkerTest {
    private Plugin plugin;
    private ModHandshake handshake;
    private ModHitMarker hitMarker;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        handshake = mock(ModHandshake.class);
        hitMarker = new ModHitMarker(plugin, handshake);
    }

    private Player player(boolean hasMod, boolean listening) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(handshake.hasMod(player)).thenReturn(hasMod);
        when(player.getListeningPluginChannels())
                .thenReturn(listening ? Set.of(ModProtocol.HIT_CHANNEL) : Set.of());
        return player;
    }

    @Test
    void encodesZoneAsSingleByte() {
        assertArrayEquals(new byte[] {ModHitMarker.ZONE_BODY}, ModHitMarker.encode(HitZone.BODY));
        assertArrayEquals(new byte[] {ModHitMarker.ZONE_HEAD}, ModHitMarker.encode(HitZone.HEAD));
    }

    @Test
    void sendsHitOnlyToShooterWhenPointIsScored() {
        Player shooter = player(true, true);
        Player target = player(true, true);

        hitMarker.onGameEvent(new GameEvent.PointScored(shooter, "RED", target, "BLUE", HitZone.HEAD,
                List.of(shooter.getUniqueId(), target.getUniqueId())));

        verify(shooter).sendPluginMessage(plugin, ModProtocol.HIT_CHANNEL, new byte[] {ModHitMarker.ZONE_HEAD});
        verify(target, never()).sendPluginMessage(any(), anyString(), any());
    }

    @Test
    void doesNotSendToVanillaPlayers() {
        Player vanilla = player(false, false);

        hitMarker.show(vanilla, HitZone.BODY);

        verify(vanilla, never()).sendPluginMessage(any(), anyString(), any());
    }

    @Test
    void waitsUntilClientListensOnHitChannel() {
        Player notReady = player(true, false);

        hitMarker.show(notReady, HitZone.BODY);

        verify(notReady, never()).sendPluginMessage(any(), eq(ModProtocol.HIT_CHANNEL), any());
    }
}
