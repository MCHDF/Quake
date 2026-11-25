package moe.mchdf.quake.mod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 * 클라이언트 모드의 핸드셰이크를 받아 응답하고, 모드를 쓰는 플레이어를 기록합니다.
 *
 * <p>플러그인이 채널을 등록하면 서버가 접속한 클라이언트에 채널 목록을 알리고,
 * 모드는 그 목록에서 {@link ModProtocol#HANDSHAKE_CHANNEL}을 찾았을 때만 핸드셰이크를 보냅니다.
 * 버전이 같으면 수락하고, 다르면 거절 응답을 보내 모드가 스스로 비활성화하게 합니다.
 *
 * <p>서버는 클라이언트가 {@code minecraft:register}로 등록한 채널로만 메시지를 보냅니다(등록 전이면 조용히 버려짐).
 * 핸드셰이크가 채널 등록보다 먼저 도착하면 응답을 보관했다가 등록되는 즉시 보냅니다.
 */
public final class ModHandshake implements PluginMessageListener, Listener {
    private final Plugin plugin;
    private final Set<UUID> modPlayers = new HashSet<>();
    private final Map<UUID, byte[]> pendingReplies = new HashMap<>();
    private final List<Consumer<Player>> acceptListeners = new ArrayList<>();

    public ModHandshake(Plugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        var messenger = plugin.getServer().getMessenger();
        messenger.registerIncomingPluginChannel(plugin, ModProtocol.HANDSHAKE_CHANNEL, this);
        messenger.registerOutgoingPluginChannel(plugin, ModProtocol.HANDSHAKE_CHANNEL);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 핸드셰이크가 수락될 때 호출할 작업을 등록합니다. 바닐라 화면(사이드바 등)을 모드 화면으로 넘길 때 씁니다.
     * 같은 접속에서 핸드셰이크가 다시 와도 처음 수락될 때만 호출합니다.
     */
    public void addAcceptListener(Consumer<Player> listener) {
        acceptListeners.add(listener);
    }

    /** 핸드셰이크를 마친(버전이 맞는) 모드를 쓰는 플레이어인지 확인합니다. */
    public boolean hasMod(Player player) {
        return modPlayers.contains(player.getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!ModProtocol.HANDSHAKE_CHANNEL.equals(channel)) {
            return;
        }
        int clientVersion;
        try {
            clientVersion = new DataInputStream(new ByteArrayInputStream(message)).readInt();
        } catch (IOException e) {
            plugin.getLogger().warning(player.getName() + "의 모드 핸드셰이크를 읽지 못했습니다.");
            return;
        }
        boolean accepted = clientVersion == ModProtocol.VERSION;
        boolean firstAccept = false;
        if (accepted) {
            firstAccept = modPlayers.add(player.getUniqueId());
            plugin.getLogger().info(player.getName() + "이(가) Quake 모드로 접속했습니다. (프로토콜 " + clientVersion + ")");
        } else {
            modPlayers.remove(player.getUniqueId());
            plugin.getLogger().info(player.getName() + "의 Quake 모드 버전이 맞지 않습니다. (모드 "
                    + clientVersion + ", 서버 " + ModProtocol.VERSION + ")");
        }
        sendReply(player, encodeReply(accepted));
        // 수락 응답을 먼저 보내고, 사이드바 등 모드 화면 패킷은 그 뒤에 보낸다.
        if (firstAccept) {
            acceptListeners.forEach(listener -> listener.accept(player));
        }
    }

    private void sendReply(Player player, byte[] reply) {
        if (player.getListeningPluginChannels().contains(ModProtocol.HANDSHAKE_CHANNEL)) {
            pendingReplies.remove(player.getUniqueId());
            player.sendPluginMessage(plugin, ModProtocol.HANDSHAKE_CHANNEL, reply);
        } else {
            pendingReplies.put(player.getUniqueId(), reply);
        }
    }

    @EventHandler
    public void onRegisterChannel(PlayerRegisterChannelEvent event) {
        if (!ModProtocol.HANDSHAKE_CHANNEL.equals(event.getChannel())) {
            return;
        }
        byte[] reply = pendingReplies.remove(event.getPlayer().getUniqueId());
        if (reply != null) {
            event.getPlayer().sendPluginMessage(plugin, ModProtocol.HANDSHAKE_CHANNEL, reply);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        modPlayers.remove(event.getPlayer().getUniqueId());
        pendingReplies.remove(event.getPlayer().getUniqueId());
    }

    static byte[] encodeReply(boolean accepted) {
        var bytes = new ByteArrayOutputStream(5);
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(ModProtocol.VERSION);
            out.writeBoolean(accepted);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return bytes.toByteArray();
    }
}
