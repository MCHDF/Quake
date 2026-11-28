package moe.mchdf.quake.mod;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import moe.mchdf.quake.menu.SettingsMenu;
import moe.mchdf.quake.settings.PlayerSettingsService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModSettingsScreenTest {
    private Plugin plugin;
    private ModHandshake handshake;
    private ModSettingsScreen screen;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        handshake = mock(ModHandshake.class);
        screen = new ModSettingsScreen(plugin, handshake);
    }

    private Player player(boolean hasMod, boolean listening) {
        Player player = mock(Player.class);
        when(handshake.hasMod(player)).thenReturn(hasMod);
        when(player.getListeningPluginChannels())
                .thenReturn(listening ? Set.of(ModProtocol.OPEN_SETTINGS_CHANNEL) : Set.of());
        return player;
    }

    @Test
    void sendsEmptyOpenPacketToModUser() {
        Player modUser = player(true, true);

        assertTrue(screen.open(modUser));
        verify(modUser).sendPluginMessage(plugin, ModProtocol.OPEN_SETTINGS_CHANNEL, new byte[0]);
    }

    @Test
    void vanillaUserGetsNoPacket() {
        Player vanilla = player(false, false);

        assertFalse(screen.open(vanilla));
        verify(vanilla, never()).sendPluginMessage(any(), anyString(), any());
    }

    @Test
    void modUserBeforeChannelRegistrationFallsBackToVanilla() {
        Player notReady = player(true, false);

        assertFalse(screen.open(notReady));
        verify(notReady, never()).sendPluginMessage(any(), anyString(), any());
    }

    @Test
    void settingsMenuDoesNotOpenChestForModUser() {
        Player modUser = player(true, true);

        new SettingsMenu(screen, mock(PlayerSettingsService.class)).open(modUser);

        verify(modUser, never()).openInventory(any(org.bukkit.inventory.Inventory.class));
    }
}
