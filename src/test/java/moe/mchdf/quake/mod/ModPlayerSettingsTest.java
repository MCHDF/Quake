package moe.mchdf.quake.mod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import moe.mchdf.quake.settings.PlayerSettings;
import moe.mchdf.quake.settings.PlayerSettingsRepository;
import moe.mchdf.quake.settings.PlayerSettingsService;
import moe.mchdf.quake.settings.SoundLevel;
import moe.mchdf.quake.settings.SoundType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ModPlayerSettingsTest {
    private Plugin plugin;
    private ModHandshake handshake;
    private PlayerSettingsRepository repository;
    private PlayerSettingsService settings;
    private ModPlayerSettings channel;
    private Player modUser;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        handshake = mock(ModHandshake.class);
        repository = mock(PlayerSettingsRepository.class);
        settings = new PlayerSettingsService(repository, Logger.getAnonymousLogger());
        channel = new ModPlayerSettings(plugin, handshake, settings);
        settings.addChangeListener((player, before, after) -> channel.send(player));
        modUser = mock(Player.class);
        when(modUser.getUniqueId()).thenReturn(UUID.randomUUID());
        when(handshake.hasMod(modUser)).thenReturn(true);
        when(modUser.getListeningPluginChannels()).thenReturn(Set.of(ModProtocol.PLAYER_SETTINGS_CHANNEL));
    }

    private static byte[] change(String key, String value) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            ModSidebar.writeString(out, key);
            ModSidebar.writeString(out, value);
        }
        return bytes.toByteArray();
    }

    @Test
    void encodesEveryItemAsNameValuePairs() throws Exception {
        var expected = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(expected)) {
            out.writeByte(7);
            for (String text : new String[] {"sidebar", "false", "auto_rejoin", "true",
                    "sound.hit", "off", "sound.kill", "high", "sound.own_shot", "high",
                    "sound.other_shot", "high", "sound.countdown", "high"}) {
                ModSidebar.writeString(out, text);
            }
        }
        assertArrayEquals(expected.toByteArray(), ModPlayerSettings.encode(
                new PlayerSettings(false, true).withSound(SoundType.HIT_CONFIRM, SoundLevel.OFF)));
    }

    @Test
    void modChangeIsSavedAndSentBack() throws Exception {
        channel.onPluginMessageReceived(ModProtocol.PLAYER_SETTINGS_CHANNEL, modUser, change("sidebar", "false"));

        PlayerSettings expected = PlayerSettings.DEFAULTS.withSidebar(false);
        assertEquals(expected, settings.get(modUser));
        verify(repository).save(modUser.getUniqueId(), expected);
        verify(modUser).sendPluginMessage(plugin, ModProtocol.PLAYER_SETTINGS_CHANNEL,
                ModPlayerSettings.encode(expected));
    }

    @Test
    void unknownValueIsNotSavedAndCurrentSettingsAreResent() throws Exception {
        channel.onPluginMessageReceived(ModProtocol.PLAYER_SETTINGS_CHANNEL, modUser, change("sidebar", "maybe"));
        channel.onPluginMessageReceived(ModProtocol.PLAYER_SETTINGS_CHANNEL, modUser, change("fly", "true"));

        assertEquals(PlayerSettings.DEFAULTS, settings.get(modUser));
        verify(repository, never()).save(any(), any());
        verify(modUser, org.mockito.Mockito.times(2)).sendPluginMessage(plugin, ModProtocol.PLAYER_SETTINGS_CHANNEL,
                ModPlayerSettings.encode(PlayerSettings.DEFAULTS));
    }

    @Test
    void modCanChangeSoundLevel() throws Exception {
        channel.onPluginMessageReceived(ModProtocol.PLAYER_SETTINGS_CHANNEL, modUser, change("sound.countdown", "low"));
        channel.onPluginMessageReceived(ModProtocol.PLAYER_SETTINGS_CHANNEL, modUser, change("sound.kill", "loud"));

        assertEquals(PlayerSettings.DEFAULTS.withSound(SoundType.COUNTDOWN, SoundLevel.LOW), settings.get(modUser));
    }

    @Test
    void playerWithoutModIsIgnored() throws Exception {
        Player vanilla = mock(Player.class);
        when(vanilla.getUniqueId()).thenReturn(UUID.randomUUID());

        channel.onPluginMessageReceived(ModProtocol.PLAYER_SETTINGS_CHANNEL, vanilla, change("sidebar", "false"));
        channel.send(vanilla);

        assertEquals(PlayerSettings.DEFAULTS, settings.get(vanilla));
        verify(vanilla, never()).sendPluginMessage(any(), anyString(), any());
    }
}
