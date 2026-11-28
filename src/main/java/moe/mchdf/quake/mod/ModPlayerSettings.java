package moe.mchdf.quake.mod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import moe.mchdf.quake.settings.PlayerSettings;
import moe.mchdf.quake.settings.PlayerSettingsService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * 서버 DB에 저장된 플레이어 설정(사이드바, 자동 다시 참여 등)을 모드 설정 화면과 주고받습니다
 * ({@link ModProtocol#PLAYER_SETTINGS_CHANNEL}).
 *
 * <p>서버 → 클라: VarInt 항목 수, 항목마다 문자열 이름과 문자열 값. 핸드셰이크가 수락되거나 설정이 바뀔 때마다
 * 전체를 보냅니다(바닐라 화면에서 바꿨을 때도). 클라 → 서버: 문자열 이름, 문자열 값으로 바꾼 항목 하나.
 * 서버가 값을 검사해 저장한 뒤 전체를 다시 보내므로, 받아들이지 않은 값은 모드 화면에서도 되돌아갑니다.
 * 문자열은 VarInt 바이트 길이 + UTF-8입니다.
 */
public final class ModPlayerSettings implements PluginMessageListener, Listener {
    /** 클라가 보낸 문자열의 최대 바이트 수. 항목 이름과 값은 짧다. */
    private static final int MAX_STRING_BYTES = 64;

    private final Plugin plugin;
    private final ModHandshake handshake;
    private final PlayerSettingsService settings;

    public ModPlayerSettings(Plugin plugin, ModHandshake handshake, PlayerSettingsService settings) {
        this.plugin = plugin;
        this.handshake = handshake;
        this.settings = settings;
    }

    public void register() {
        var messenger = plugin.getServer().getMessenger();
        messenger.registerIncomingPluginChannel(plugin, ModProtocol.PLAYER_SETTINGS_CHANNEL, this);
        messenger.registerOutgoingPluginChannel(plugin, ModProtocol.PLAYER_SETTINGS_CHANNEL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        handshake.addAcceptListener(this::send);
        settings.addChangeListener((player, before, after) -> send(player));
    }

    /** 모드 사용자에게 현재 설정을 보낸다. 모드가 없거나 채널 등록 전이면 보내지 않는다(등록되면 그때 보냄). */
    public void send(Player player) {
        if (!handshake.hasMod(player)
                || !player.getListeningPluginChannels().contains(ModProtocol.PLAYER_SETTINGS_CHANNEL)) {
            return;
        }
        player.sendPluginMessage(plugin, ModProtocol.PLAYER_SETTINGS_CHANNEL, encode(settings.get(player)));
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (ModProtocol.PLAYER_SETTINGS_CHANNEL.equals(event.getChannel())) {
            send(event.getPlayer());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!ModProtocol.PLAYER_SETTINGS_CHANNEL.equals(channel) || !handshake.hasMod(player)) {
            return;
        }
        String key;
        String value;
        try (var in = new DataInputStream(new ByteArrayInputStream(message))) {
            key = readString(in);
            value = readString(in);
        } catch (IOException exception) {
            plugin.getLogger().warning(player.getName() + "의 모드 설정 변경을 읽지 못했습니다.");
            return;
        }
        var changed = settings.get(player).with(key, value);
        if (changed.isEmpty()) {
            plugin.getLogger().warning(player.getName() + "의 모드가 알 수 없는 설정 값을 보냈습니다: " + key + "=" + value);
            send(player);
            return;
        }
        settings.update(player, ignored -> changed.get());
    }

    static byte[] encode(PlayerSettings current) {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            Map<String, String> values = current.values();
            ModSidebar.writeVarInt(out, values.size());
            for (var entry : values.entrySet()) {
                ModSidebar.writeString(out, entry.getKey());
                ModSidebar.writeString(out, entry.getValue());
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        return bytes.toByteArray();
    }

    private static String readString(DataInputStream in) throws IOException {
        int length = readVarInt(in);
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new IOException("문자열 길이가 올바르지 않습니다: " + length);
        }
        return new String(in.readNBytes(length), StandardCharsets.UTF_8);
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int value = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            int b = in.readUnsignedByte();
            value |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) {
                return value;
            }
        }
        throw new IOException("VarInt가 너무 깁니다.");
    }
}
