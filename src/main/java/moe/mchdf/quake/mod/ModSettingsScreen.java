package moe.mchdf.quake.mod;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * 모드 사용자에게 모드 전용 설정 화면을 열라고 알립니다({@link ModProtocol#OPEN_SETTINGS_CHANNEL}).
 * 모드 사용자는 바닐라 사용자와 설정 항목이 달라 화면을 모드가 직접 그립니다.
 */
public final class ModSettingsScreen {
    private static final byte[] EMPTY = new byte[0];

    private final Plugin plugin;
    private final ModHandshake handshake;

    public ModSettingsScreen(Plugin plugin, ModHandshake handshake) {
        this.plugin = plugin;
        this.handshake = handshake;
    }

    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, ModProtocol.OPEN_SETTINGS_CHANNEL);
    }

    /**
     * 모드 화면을 열었으면 {@code true}. 모드가 없거나 아직 채널을 등록하지 않았으면 아무것도 보내지 않고
     * {@code false}를 돌려주므로, 호출한 쪽이 바닐라 화면을 연다.
     */
    public boolean open(Player player) {
        if (!handshake.hasMod(player)
                || !player.getListeningPluginChannels().contains(ModProtocol.OPEN_SETTINGS_CHANNEL)) {
            return false;
        }
        player.sendPluginMessage(plugin, ModProtocol.OPEN_SETTINGS_CHANNEL, EMPTY);
        return true;
    }
}
