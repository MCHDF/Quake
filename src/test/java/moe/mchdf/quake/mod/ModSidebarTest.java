package moe.mchdf.quake.mod;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import moe.mchdf.quake.scoreboard.SidebarContent;
import moe.mchdf.quake.scoreboard.SidebarRow;
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
    void encodesRowsInModPayloadFormat() throws IOException {
        var content = new SidebarContent(NamedTextColor.RED, List.of(
                new SidebarRow.Spacer(),
                new SidebarRow.Text(Component.text("t"), true),
                new SidebarRow.Value(Component.text("k"), Component.text("v")),
                new SidebarRow.Bar(Component.text("b"), Component.text("1"), 0.5f, NamedTextColor.GREEN)));

        byte[] data = ModSidebar.encode(true, Component.text("T"), content);

        var bytes = new ByteArrayOutputStream();
        var expected = new DataOutputStream(bytes);
        expected.writeBoolean(true);
        writeMinecraftString(expected, json(Component.text("T")));
        expected.writeInt(0xFF000000 | NamedTextColor.RED.value());
        expected.writeByte(4); // 줄 수 VarInt
        expected.writeByte(ModSidebar.ROW_SPACER);
        expected.writeByte(ModSidebar.ROW_TEXT);
        writeMinecraftString(expected, json(Component.text("t")));
        expected.writeBoolean(true);
        expected.writeByte(ModSidebar.ROW_VALUE);
        writeMinecraftString(expected, json(Component.text("k")));
        writeMinecraftString(expected, json(Component.text("v")));
        expected.writeByte(ModSidebar.ROW_BAR);
        writeMinecraftString(expected, json(Component.text("b")));
        writeMinecraftString(expected, json(Component.text("1")));
        expected.writeFloat(0.5f);
        expected.writeInt(0xFF000000 | NamedTextColor.GREEN.value());
        assertArrayEquals(bytes.toByteArray(), data);
    }

    @Test
    void defaultAccentIsZero() throws IOException {
        byte[] data = ModSidebar.encode(true, Component.text("T"), new SidebarContent(null, List.of()));

        var in = new java.io.DataInputStream(new java.io.ByteArrayInputStream(data));
        in.readBoolean();
        in.skipBytes(in.readUnsignedByte());
        assertEquals(0, in.readInt());
    }

    @Test
    void keepsColorsInJson() {
        byte[] data = ModSidebar.encode(true, Component.empty(), new SidebarContent(null,
                List.of(new SidebarRow.Text(Component.text("5명", NamedTextColor.YELLOW), false))));

        assertTrue(new String(data, StandardCharsets.UTF_8).contains("\"color\":\"yellow\""));
    }

    @Test
    void varIntUsesSevenBitGroups() throws IOException {
        var bytes = new ByteArrayOutputStream();
        ModSidebar.writeVarInt(new DataOutputStream(bytes), 300);
        assertArrayEquals(new byte[] {(byte) 0xAC, 0x02}, bytes.toByteArray());
    }

    @Test
    void sendsOnlyWhenContentChanges() {
        sidebar.show(player, Component.text("T"), content("a"));
        sidebar.show(player, Component.text("T"), content("a"));
        sidebar.show(player, Component.text("T"), content("b"));

        verify(player, times(2)).sendPluginMessage(eq(plugin), eq(ModProtocol.SIDEBAR_CHANNEL), any());
    }

    @Test
    void waitsUntilClientRegistersChannel() {
        when(player.getListeningPluginChannels()).thenReturn(Set.of());
        sidebar.show(player, Component.text("T"), content("a"));
        verify(player, never()).sendPluginMessage(any(), anyString(), any());

        when(player.getListeningPluginChannels()).thenReturn(Set.of(ModProtocol.SIDEBAR_CHANNEL));
        sidebar.show(player, Component.text("T"), content("a"));
        verify(player).sendPluginMessage(eq(plugin), eq(ModProtocol.SIDEBAR_CHANNEL), any());
    }

    private static String json(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    private static SidebarContent content(String line) {
        return new SidebarContent(null, List.of(new SidebarRow.Text(Component.text(line), false)));
    }

    private static void writeMinecraftString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeByte(bytes.length); // 128바이트 미만이면 VarInt는 1바이트
        out.write(bytes);
    }
}
