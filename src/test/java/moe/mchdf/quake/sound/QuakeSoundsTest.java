package moe.mchdf.quake.sound;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import moe.mchdf.quake.settings.PlayerSettings;
import moe.mchdf.quake.settings.SoundLevel;
import moe.mchdf.quake.settings.SoundType;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class QuakeSoundsTest {
    private final Map<Player, PlayerSettings> settings = new HashMap<>();
    private World world;
    private QuakeSounds sounds;

    @BeforeEach
    void setUp() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(mock(BukkitScheduler.class));
        world = mock(World.class);
        sounds = new QuakeSounds(plugin, player -> settings.getOrDefault(player, PlayerSettings.DEFAULTS));
    }

    private Player player(double x) {
        Player player = mock(Player.class);
        Location location = new Location(world, x, 64, 0);
        when(player.getLocation()).thenReturn(location);
        when(player.getEyeLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        return player;
    }

    private static void verifyBoom(Player listener, float volume) {
        verify(listener).playSound(any(Location.class), eq("minecraft:entity.warden.sonic_boom"), eq(SoundCategory.PLAYERS),
                ArgumentMatchers.floatThat(actual -> Math.abs(actual - volume) < 1.0E-4F), anyFloat());
    }

    @Test
    void shooterAndOthersHearLaunchAtTheirOwnVolume() {
        Player shooter = player(0);
        Player near = player(5);
        Player muted = player(6);
        Player far = player(100);
        when(world.getPlayers()).thenReturn(List.of(shooter, near, muted, far));
        settings.put(shooter, PlayerSettings.DEFAULTS.withSound(SoundType.OWN_SHOT, SoundLevel.LOW));
        settings.put(muted, PlayerSettings.DEFAULTS.withSound(SoundType.OTHER_SHOT, SoundLevel.OFF));

        sounds.launch(shooter);

        verifyBoom(shooter, 0.7F * 0.4F);
        verifyBoom(near, 0.7F);
        verify(muted, never()).playSound(any(Location.class), anyString(), any(SoundCategory.class),
                anyFloat(), anyFloat());
        verify(far, never()).playSound(any(Location.class), anyString(), any(SoundCategory.class),
                anyFloat(), anyFloat());
    }

    @Test
    void ownShotSettingDoesNotChangeWhatOthersHear() {
        Player shooter = player(0);
        Player other = player(3);
        when(world.getPlayers()).thenReturn(List.of(shooter, other));
        settings.put(other, PlayerSettings.DEFAULTS.withSound(SoundType.OWN_SHOT, SoundLevel.OFF));

        sounds.launch(shooter);

        verifyBoom(other, 0.7F);
    }

    @Test
    void previewPlaysChosenLevelAndOffIsSilent() {
        Player player = player(0);

        sounds.preview(player, SoundType.COUNTDOWN, SoundLevel.MEDIUM);
        sounds.preview(player, SoundType.KILL, SoundLevel.OFF);

        verify(player).playSound(any(Location.class), eq("minecraft:block.note_block.pling"), eq(SoundCategory.PLAYERS),
                eq(0.7F), eq(1.0F));
        verify(player, never()).playSound(any(Location.class), eq("minecraft:block.glass.break"), any(SoundCategory.class),
                anyFloat(), anyFloat());
    }
}
