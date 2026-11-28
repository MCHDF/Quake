package moe.mchdf.quake.sound;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import moe.mchdf.quake.settings.PlayerSettings;
import moe.mchdf.quake.settings.SoundLevel;
import moe.mchdf.quake.settings.SoundType;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Quake 효과음. 월드 전체에 한 번 재생하지 않고 듣는 사람마다 따로 재생해, 각자 정한 음량
 * ({@link PlayerSettings#sound})을 곱하고 끈 사람에게는 보내지 않는다.
 */
public final class QuakeSounds {
    /** 위치가 있는 소리는 음량이 1 이하일 때 이 거리까지 들린다(마인크래프트 기준 16블록). */
    private static final double HEARING_DISTANCE = 16.0D;
    // 매 발사마다 피치를 살짝 흔들어 같은 소리가 반복되는 느낌을 줄임
    private static final float PITCH_JITTER = 0.05F;

    /**
     * 효과음 한 겹. delayTicks 만큼 늦게 재생해 "충전 → 발사 → 잔향" 순서를 만든다.
     * 소리는 {@code org.bukkit.Sound} 상수 대신 이름으로 둔다. 상수는 서버 레지스트리가 있어야 만들어져
     * 단위 테스트에서 쓸 수 없다.
     */
    record SoundLayer(String sound, float volume, float pitch, long delayTicks) {
    }

    /*
     * 적중음 — 쏜 사람에게만, 거리와 상관없이 항상 들림
     *   - 화살 명중음 : 바닐라 활 명중 "띵"
     *   - 종 노트블록 : 높은 피치로 얹어 맑고 또렷한 확인음
     */
    private static final List<SoundLayer> HIT_CONFIRM = List.of(
            new SoundLayer("minecraft:entity.arrow.hit_player", 1.0F, 0.9F, 0L),
            new SoundLayer("minecraft:block.note_block.bell", 0.5F, 2.0F, 0L)
    );

    /*
     * 처치음 — 명중 지점에서 전기 충돌. 주변 플레이어와 맞은 사람이 들음
     *   - 번개 충돌음   : 빔이 꽂히는 전기 "파직"
     *   - 유리 깨짐     : 높은 피치로 날카로운 파편감
     *   - 불 꺼지는 소리 : 맞은 자리가 "치익" 하고 타는 잔향
     */
    private static final List<SoundLayer> IMPACT = List.of(
            new SoundLayer("minecraft:entity.lightning_bolt.impact", 0.6F, 1.8F, 0L),
            new SoundLayer("minecraft:block.glass.break", 0.7F, 1.8F, 0L),
            new SoundLayer("minecraft:block.fire.extinguish", 0.4F, 1.6F, 0L)
    );

    /*
     * 레일건 발사음 — 쏜 사람은 내 발사음, 주변은 다른 사람 발사음 음량으로 들음
     *  1. 전자 충전음  : 비콘 효과 선택음을 최고 피치로 → 짧고 높은 "삐잉" 하는 전기음
     *  2. 초음속 파열음 : 워든 음파 공격을 높은 피치로 → 묵직한 저음이 빠진 날카로운 빔 발사음
     *  3. 방전 스냅    : 번개 충돌음을 최고 피치로, 작게 → 전기가 튀는 "탁" 소리
     *  4. 금속 잔향    : 자수정 공명음 → 레일이 울리는 듯한 맑은 금속성 꼬리
     */
    private static final List<SoundLayer> LAUNCH = List.of(
            new SoundLayer("minecraft:block.beacon.power_select", 0.6F, 2.0F, 0L),
            new SoundLayer("minecraft:entity.warden.sonic_boom", 0.7F, 1.8F, 0L),
            new SoundLayer("minecraft:entity.lightning_bolt.impact", 0.35F, 2.0F, 0L),
            new SoundLayer("minecraft:block.amethyst_block.resonate", 0.8F, 1.6F, 2L)
    );

    /** 카운트다운 — 시작 5초 전부터 1초마다, 시작 순간에는 높은 피치 */
    private static final List<SoundLayer> COUNTDOWN = List.of(
            new SoundLayer("minecraft:block.note_block.pling", 1.0F, 1.0F, 0L)
    );
    private static final float COUNTDOWN_START_PITCH = 2.0F;

    private static final Map<SoundType, List<SoundLayer>> PREVIEWS = new EnumMap<>(Map.of(
            SoundType.HIT_CONFIRM, HIT_CONFIRM,
            SoundType.KILL, IMPACT,
            SoundType.OWN_SHOT, LAUNCH,
            SoundType.OTHER_SHOT, LAUNCH,
            SoundType.COUNTDOWN, COUNTDOWN
    ));

    private final Plugin plugin;
    private final Function<Player, PlayerSettings> settings;

    public QuakeSounds(Plugin plugin, Function<Player, PlayerSettings> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    /** 쏜 사람에게만 들리는 적중음 */
    public void hitConfirm(Player shooter) {
        play(shooter, shooter.getLocation(), HIT_CONFIRM, SoundType.HIT_CONFIRM, randomJitter());
    }

    /** 명중 지점의 처치음. 맞은 사람을 포함해 주변 플레이어가 듣는다. */
    public void impact(Location at) {
        float jitter = randomJitter();
        for (Player listener : at.getWorld().getPlayers()) {
            if (canHear(listener, at)) {
                play(listener, at, IMPACT, SoundType.KILL, jitter);
            }
        }
    }

    /** 레일건 발사음. 쏜 사람은 내 발사음, 다른 사람은 다른 사람 발사음 음량으로 듣는다. */
    public void launch(Player shooter) {
        Location at = shooter.getEyeLocation();
        float jitter = randomJitter();
        for (Player listener : at.getWorld().getPlayers()) {
            if (listener.equals(shooter)) {
                play(listener, at, LAUNCH, SoundType.OWN_SHOT, jitter);
            } else if (canHear(listener, at)) {
                play(listener, at, LAUNCH, SoundType.OTHER_SHOT, jitter);
            }
        }
    }

    /** 카운트다운 한 번. {@code start}이면 시작 순간의 높은 소리 */
    public void countdown(Player player, boolean start) {
        float shift = start ? COUNTDOWN_START_PITCH - COUNTDOWN.getFirst().pitch() : 0.0F;
        play(player, player.getLocation(), COUNTDOWN, SoundType.COUNTDOWN, shift);
    }

    /** 설정 화면에서 음량을 바꿨을 때, 그 종류의 소리를 고른 음량으로 들려준다. 끄기면 아무것도 들리지 않는다. */
    public void preview(Player player, SoundType type, SoundLevel level) {
        playAt(player, player.getLocation(), PREVIEWS.get(type), level, 0.0F);
    }

    private void play(Player listener, Location at, List<SoundLayer> layers, SoundType type, float pitchShift) {
        playAt(listener, at, layers, settings.apply(listener).sound(type), pitchShift);
    }

    private void playAt(Player listener, Location at, List<SoundLayer> layers, SoundLevel level, float pitchShift) {
        if (level == SoundLevel.OFF) {
            return;
        }
        for (SoundLayer layer : layers) {
            float volume = layer.volume() * level.multiplier();
            float pitch = clampPitch(layer.pitch() + pitchShift);
            if (layer.delayTicks() <= 0L) {
                listener.playSound(at, layer.sound(), SoundCategory.PLAYERS, volume, pitch);
            } else {
                Location delayed = at.clone();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (listener.isOnline()) {
                        listener.playSound(delayed, layer.sound(), SoundCategory.PLAYERS, volume, pitch);
                    }
                }, layer.delayTicks());
            }
        }
    }

    private static boolean canHear(Player listener, Location at) {
        return listener.getLocation().distanceSquared(at) <= HEARING_DISTANCE * HEARING_DISTANCE;
    }

    private static float randomJitter() {
        return (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F) * PITCH_JITTER;
    }

    // 마인크래프트 사운드 피치는 0.5~2.0 범위만 유효
    private static float clampPitch(float pitch) {
        return Math.clamp(pitch, 0.5F, 2.0F);
    }
}
