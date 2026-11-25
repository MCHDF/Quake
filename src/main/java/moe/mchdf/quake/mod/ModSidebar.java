package moe.mchdf.quake.mod;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import moe.mchdf.quake.scoreboard.SidebarContent;
import moe.mchdf.quake.scoreboard.SidebarRow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * 모드 사용자에게 사이드바 내용을 패킷({@link ModProtocol#SIDEBAR_CHANNEL})으로 보냅니다.
 *
 * <p>문구와 색은 텍스트 컴포넌트 JSON으로, 줄의 형태(값 줄, 막대, 구분선 등)는 종류 번호로 보냅니다.
 * 모드는 내용의 의미를 몰라도 형태에 맞춰 꾸며 그리기만 하면 되므로, 사이드바 문구나 항목을 바꿔도
 * 모드를 다시 배포할 필요가 없습니다. 내용이 바뀌었을 때만 보냅니다.
 */
public final class ModSidebar implements Listener {
    private final Plugin plugin;
    private final ModHandshake handshake;
    private final Map<UUID, byte[]> lastSent = new HashMap<>();

    public ModSidebar(Plugin plugin, ModHandshake handshake) {
        this.plugin = plugin;
        this.handshake = handshake;
    }

    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, ModProtocol.SIDEBAR_CHANNEL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /** 바닐라 사이드바 대신 모드 화면을 쓰는 플레이어인지 */
    public boolean usesModUi(Player player) {
        return handshake.hasMod(player);
    }

    public void show(Player player, Component title, SidebarContent content) {
        send(player, encode(true, title, content));
    }

    /** 플러그인이 꺼질 때 모드 화면에 남은 사이드바를 지운다. */
    public void hideAll() {
        byte[] hidden = encode(false, Component.empty(), new SidebarContent(null, List.of()));
        for (UUID id : List.copyOf(lastSent.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                send(player, hidden);
            }
        }
        lastSent.clear();
    }

    private void send(Player player, byte[] data) {
        UUID id = player.getUniqueId();
        if (Arrays.equals(lastSent.get(id), data)) {
            return;
        }
        // 클라가 채널을 등록하기 전에 보내면 서버가 버리므로, 등록 뒤 다음 갱신 때 보낸다.
        if (!player.getListeningPluginChannels().contains(ModProtocol.SIDEBAR_CHANNEL)) {
            return;
        }
        player.sendPluginMessage(plugin, ModProtocol.SIDEBAR_CHANNEL, data);
        lastSent.put(id, data);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSent.remove(event.getPlayer().getUniqueId());
    }

    // 줄 종류 번호. 모드의 SidebarS2CPayload.Row와 같아야 한다.
    static final int ROW_SPACER = 0;
    static final int ROW_TEXT = 1;
    static final int ROW_VALUE = 2;
    static final int ROW_BAR = 3;

    /**
     * 형식: boolean 표시 여부, 문자열 제목, int 강조색(ARGB, 0이면 기본색), VarInt 줄 수, 줄 목록.
     * 줄은 byte 종류 다음에 종류별 값이 온다. 문자열은 VarInt 길이 + UTF-8, int·float는 빅엔디언 4바이트다.
     */
    static byte[] encode(boolean visible, Component title, SidebarContent content) {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeBoolean(visible);
            writeString(out, json(title));
            out.writeInt(argb(content.accent()));
            writeVarInt(out, content.rows().size());
            for (SidebarRow row : content.rows()) {
                switch (row) {
                    case SidebarRow.Spacer spacer -> out.writeByte(ROW_SPACER);
                    case SidebarRow.Text text -> {
                        out.writeByte(ROW_TEXT);
                        writeString(out, json(text.text()));
                        out.writeBoolean(text.centered());
                    }
                    case SidebarRow.Value value -> {
                        out.writeByte(ROW_VALUE);
                        writeString(out, json(value.label()));
                        writeString(out, json(value.value()));
                    }
                    case SidebarRow.Bar bar -> {
                        out.writeByte(ROW_BAR);
                        writeString(out, json(bar.label()));
                        writeString(out, json(bar.value()));
                        out.writeFloat(bar.ratio());
                        out.writeInt(argb(bar.color()));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }

    private static int argb(TextColor color) {
        return color == null ? 0 : 0xFF000000 | color.value();
    }

    private static String json(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    static void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }
}
