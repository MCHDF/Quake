package moe.mchdf.quake.weapon;

import moe.mchdf.quake.combat.HitZone;
import moe.mchdf.quake.combat.HitZones;
import moe.mchdf.quake.game.GameTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import moe.mchdf.quake.item.RailGunItem;
import moe.mchdf.quake.mod.ModHitMarker;
import moe.mchdf.quake.sound.QuakeSounds;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class RailGunWeapon {
    private static final double RANGE = 50.0D;
    private static final int COOLDOWN_TICKS = 36;
    /**
     * 플레이어 히트박스(가로 0.6, 세로 1.8블록)를 사방으로 늘리는 판정 여유 (블록).
     * 0.1이면 가로 0.8, 세로 2.0블록. 움직이는 상대를 맞힐 때 생기는 핑 지연을 조금 봐주는 정도로만 둔다.
     */
    static final double HIT_MARGIN = 0.1D;

    // 빔 외형: 밝은 심 + 반투명 바깥층
    private record BeamStyle(Material core, Material glow) {
    }

    private static final BeamStyle GAME_BEAM = new BeamStyle(Material.WHITE_CONCRETE, Material.RED_STAINED_GLASS);
    // 미니게임 중에는 쏜 사람의 팀 색으로 바깥층을 칠한다 (BLUE 팀은 하늘색)
    private static final BeamStyle RED_TEAM_BEAM = new BeamStyle(Material.WHITE_CONCRETE, Material.RED_STAINED_GLASS);
    private static final BeamStyle BLUE_TEAM_BEAM =
            new BeamStyle(Material.WHITE_CONCRETE, Material.LIGHT_BLUE_STAINED_GLASS);
    private static final BeamStyle TEST_BEAM = new BeamStyle(Material.LIGHT_GRAY_CONCRETE, Material.GRAY_STAINED_GLASS);
    private static final float CORE_WIDTH = 0.06F;
    private static final float GLOW_WIDTH = 0.18F;

    // 빔이 그대로 유지되는 시간과 가늘어지며 사라지는 시간 (틱)
    private static final int HOLD_TICKS = 2;
    private static final int FADE_TICKS = 6;

    // 1인칭에서 빔이 보이도록 시작점을 눈 위치에서 오른손 아래쪽으로 살짝 옮김
    private static final double MUZZLE_FORWARD = 0.4D;
    private static final double MUZZLE_RIGHT = 0.25D;
    private static final double MUZZLE_DOWN = 0.2D;

    private static final Vector3f BLOCK_FORWARD = new Vector3f(0.0F, 0.0F, 1.0F);

    private final JavaPlugin plugin;
    private final GameTeams gameTeams;
    private final ModHitMarker hitMarker;
    private final QuakeSounds sounds;

    public RailGunWeapon(JavaPlugin plugin, GameTeams gameTeams, ModHitMarker hitMarker, QuakeSounds sounds) {
        this.plugin = plugin;
        this.gameTeams = gameTeams;
        this.hitMarker = hitMarker;
        this.sounds = sounds;
    }

    /** 미니게임용 레일건: 적을 맞히면 득점한다. */
    public void fire(Player shooter) {
        fire(shooter, teamBeam(shooter), true);
    }

    private BeamStyle teamBeam(Player shooter) {
        String team = gameTeams.getTeamOf(shooter);
        if ("RED".equals(team)) {
            return RED_TEAM_BEAM;
        }
        if ("BLUE".equals(team)) {
            return BLUE_TEAM_BEAM;
        }
        return GAME_BEAM;
    }

    /**
     * 테스트용 레일건: 빔과 효과음만 나고 팀/점수는 건드리지 않는다.
     * 맞히면 맞은 부위를 액션바에 표시하고, 모드 사용자에게는 히트마커 패킷도 보낸다.
     */
    public void fireTest(Player shooter) {
        fire(shooter, TEST_BEAM, false);
    }

    private void fire(Player shooter, BeamStyle style, boolean scoring) {
        if (shooter == null || shooter.hasCooldown(RailGunItem.MATERIAL)) {
            return;
        }

        shooter.setCooldown(RailGunItem.MATERIAL, COOLDOWN_TICKS);

        Location eye = shooter.getEyeLocation();
        Vector direction = eye.getDirection();
        World world = shooter.getWorld();
        RayTraceResult result = world.rayTrace(
                eye,
                direction,
                RANGE,
                FluidCollisionMode.NEVER,
                false,
                HIT_MARGIN,
                entity -> entity != shooter && entity instanceof Player
        );

        // 실제 충돌 지점(블록/플레이어)까지, 아무것도 안 맞으면 최대 사거리까지 빔을 그림
        Vector end = result != null
                ? result.getHitPosition()
                : eye.toVector().add(direction.clone().multiply(RANGE));
        drawBeam(world, muzzle(eye, direction), end, style);
        sounds.launch(shooter);

        if (result == null || result.getHitBlock() != null) {
            return;
        }

        Entity hitEntity = result.getHitEntity();
        if (!(hitEntity instanceof Player hitPlayer)) {
            return;
        }

        Location impact = end.toLocation(world);
        HitZone zone = HitZones.of(hitPlayer, end);
        if (!scoring) {
            playHitSound(shooter, impact);
            showHitZone(shooter, zone);
            hitMarker.show(shooter, zone);
            return;
        }
        if (!gameTeams.areEnemies(shooter, hitPlayer)) {
            return;
        }

        playHitSound(shooter, impact);
        // 모드 히트마커는 득점 이벤트를 받아 보낸다(ModHitMarker).
        gameTeams.awardPoint(shooter, hitPlayer, zone);
    }

    /** 테스트 레일건으로 맞힌 부위를 액션바에 보여 준다. 헤드샷 판정을 게임 안에서 확인하는 용도 */
    private static void showHitZone(Player shooter, HitZone zone) {
        shooter.sendActionBar(zone == HitZone.HEAD
                ? Component.text("헤드샷", NamedTextColor.RED)
                : Component.text("몸통", NamedTextColor.WHITE));
    }

    private void playHitSound(Player shooter, Location impact) {
        sounds.hitConfirm(shooter);
        sounds.impact(impact);
    }

    private static Vector muzzle(Location eye, Vector direction) {
        Vector forward = direction.clone().normalize();
        Vector right = forward.clone().crossProduct(new Vector(0, 1, 0));
        if (right.lengthSquared() < 1.0E-6) {
            // 정확히 위/아래를 볼 때는 오른쪽 벡터를 정의할 수 없으므로 오프셋 없이 앞쪽만 이동
            return eye.toVector().add(forward.multiply(MUZZLE_FORWARD));
        }
        right.normalize();
        Vector up = right.clone().crossProduct(forward);
        return eye.toVector()
                .add(forward.multiply(MUZZLE_FORWARD))
                .add(right.multiply(MUZZLE_RIGHT))
                .subtract(up.multiply(MUZZLE_DOWN));
    }

    private void drawBeam(World world, Vector start, Vector end, BeamStyle style) {
        Vector delta = end.clone().subtract(start);
        float length = (float) delta.length();
        if (length < 0.01F) {
            return;
        }

        Quaternionf rotation = new Quaternionf().rotationTo(BLOCK_FORWARD, delta.toVector3f().normalize());
        // 엔티티 자체 회전이 변환에 더해지지 않도록 yaw/pitch는 0으로 고정
        Location origin = new Location(world, start.getX(), start.getY(), start.getZ(), 0.0F, 0.0F);

        spawnBeamPart(world, origin, style.glow(), rotation, GLOW_WIDTH, length);
        spawnBeamPart(world, origin, style.core(), rotation, CORE_WIDTH, length);
    }

    private void spawnBeamPart(World world, Location origin, Material material,
                               Quaternionf rotation, float width, float length) {
        BlockDisplay display = world.spawn(origin, BlockDisplay.class, entity -> {
            entity.setBlock(material.createBlockData());
            entity.setTransformation(beamTransformation(rotation, width, length));
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setPersistent(false);
            entity.setInterpolationDuration(0);
        });

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!display.isValid()) {
                return;
            }
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(FADE_TICKS);
            display.setTransformation(beamTransformation(rotation, 0.0F, length));
        }, HOLD_TICKS);
        plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, HOLD_TICKS + FADE_TICKS);
    }

    /**
     * 블록 모델(0~1 큐브)을 width x width x length 로 늘리고, +Z 축이 빔 방향을 향하도록 회전한다.
     * 단면 중심이 원점에 오도록 회전된 반폭만큼 평행이동한다.
     */
    private static Transformation beamTransformation(Quaternionf rotation, float width, float length) {
        Vector3f translation = new Vector3f(-width / 2.0F, -width / 2.0F, 0.0F).rotate(rotation);
        return new Transformation(
                translation,
                new Quaternionf(rotation),
                new Vector3f(width, width, length),
                new Quaternionf()
        );
    }
}
