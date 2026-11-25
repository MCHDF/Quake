package moe.mchdf.quake.mod;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
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
 * <p>바닐라 사이드바와 같은 제목·줄을 텍스트 컴포넌트 JSON으로 보내므로, 모드는 내용의 의미를 몰라도
 * 그대로 그리기만 하면 됩니다. 사이드바 문구를 바꿔도 모드를 다시 배포할 필요가 없습니다.
 * 내용이 바뀌었을 때만 보냅니다.
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

    public void show(Player player, Component title, List<Component> lines) {
        send(player, encode(true, title, lines));
    }

    /** 플러그인이 꺼질 때 모드 화면에 남은 사이드바를 지운다. */
    public void hideAll() {
        byte[] hidden = encode(false, Component.empty(), List.of());
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

    static byte[] encode(boolean visible, Component title, List<Component> lines) {
        var out = new ByteArrayOutputStream();
        out.write(visible ? 1 : 0);
        writeString(out, json(title));
        writeVarInt(out, lines.size());
        for (Component line : lines) {
            writeString(out, json(line));
        }
        return out.toByteArray();
    }

    private static String json(Component component) {
        return GsonComponentSerializer.gson().serialize(component);
    }

    static void writeString(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.writeBytes(bytes);
    }

    static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }
}
