package moe.mchdf.quake.mod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModSidebarTest {
    private Plugin plugin;
    private Player player;
    private ModSidebar sidebar;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getListeningPluginChannels()).thenReturn(Set.of(ModProtocol.SIDEBAR_CHANNEL));
        sidebar = new ModSidebar(plugin, mock(ModHandshake.class));
    }

    @Test
    void encodesInMinecraftStreamCodecFormat() {
        byte[] data = ModSidebar.encode(true, Component.text("T"), List.of(Component.text("a")));

        var expected = new ByteArrayOutputStream();
        expected.write(1); // BOOL
        writeMinecraftString(expected, json(Component.text("T")));
        expected.write(1); // 목록 개수 VarInt
        writeMinecraftString(expected, json(Component.text("a")));
        assertArrayEquals(expected.toByteArray(), data);
    }

    @Test
    void keepsColorsInJson() {
        byte[] data = ModSidebar.encode(true, Component.empty(),
                List.of(Component.text("5명", NamedTextColor.YELLOW)));

        assertTrue(new String(data, StandardCharsets.UTF_8).contains("\"color\":\"yellow\""));
    }

    @Test
    void varIntUsesSevenBitGroups() {
        var out = new ByteArrayOutputStream();
        ModSidebar.writeVarInt(out, 300);
        assertArrayEquals(new byte[] {(byte) 0xAC, 0x02}, out.toByteArray());
    }

    @Test
    void sendsOnlyWhenContentChanges() {
        sidebar.show(player, Component.text("T"), List.of(Component.text("a")));
        sidebar.show(player, Component.text("T"), List.of(Component.text("a")));
        sidebar.show(player, Component.text("T"), List.of(Component.text("b")));

        verify(player, times(2)).sendPluginMessage(eq(plugin), eq(ModProtocol.SIDEBAR_CHANNEL), any());
    }

    @Test
    void waitsUntilClientRegistersChannel() {
        when(player.getListeningPluginChannels()).thenReturn(Set.of());
        sidebar.show(player, Component.text("T"), List.of());
        verify(player, never()).sendPluginMessage(any(), anyString(), any());

        when(player.getListeningPluginChannels()).thenReturn(Set.of(ModProtocol.SIDEBAR_CHANNEL));
        sidebar.show(player, Component.text("T"), List.of());
        verify(player).sendPluginMessage(eq(plugin), eq(ModProtocol.SIDEBAR_CHANNEL), any());
    }

    private static String json(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    private static void writeMinecraftString(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.write(bytes.length); // 128바이트 미만이면 VarInt는 1바이트
        out.writeBytes(bytes);
    }
}
