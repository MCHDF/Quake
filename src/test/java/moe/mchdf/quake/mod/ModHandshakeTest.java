package moe.mchdf.quake.mod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModHandshakeTest {
    private Plugin plugin;
    private Player player;
    private ModHandshake handshake;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ModHandshakeTest"));
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("tester");
        handshake = new ModHandshake(plugin);
    }

    @Test
    void acceptsMatchingVersion() {
        handshake.onPluginMessageReceived(ModProtocol.HANDSHAKE_CHANNEL, player, intBytes(ModProtocol.VERSION));

        assertTrue(handshake.hasMod(player));
        verify(player).sendPluginMessage(plugin, ModProtocol.HANDSHAKE_CHANNEL, ModHandshake.encodeReply(true));
    }

    @Test
    void rejectsDifferentVersion() {
        handshake.onPluginMessageReceived(ModProtocol.HANDSHAKE_CHANNEL, player, intBytes(ModProtocol.VERSION + 1));

        assertFalse(handshake.hasMod(player));
        verify(player).sendPluginMessage(plugin, ModProtocol.HANDSHAKE_CHANNEL, ModHandshake.encodeReply(false));
    }

    @Test
    void ignoresMalformedMessage() {
        handshake.onPluginMessageReceived(ModProtocol.HANDSHAKE_CHANNEL, player, new byte[] {1});

        assertFalse(handshake.hasMod(player));
        verify(player, never()).sendPluginMessage(any(), eq(ModProtocol.HANDSHAKE_CHANNEL), any());
    }

    @Test
    void forgetsPlayerOnQuit() {
        handshake.onPluginMessageReceived(ModProtocol.HANDSHAKE_CHANNEL, player, intBytes(ModProtocol.VERSION));
        handshake.onQuit(new PlayerQuitEvent(player, (net.kyori.adventure.text.Component) null,
                PlayerQuitEvent.QuitReason.DISCONNECTED));

        assertFalse(handshake.hasMod(player));
    }

    @Test
    void replyMatchesModCodec() {
        // 모드 HandshakeS2CPayload: int(빅엔디언) 버전 + boolean 1바이트
        assertArrayEquals(new byte[] {0, 0, 0, (byte) ModProtocol.VERSION, 1}, ModHandshake.encodeReply(true));
    }

    private static byte[] intBytes(int value) {
        return new byte[] {(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }
}
