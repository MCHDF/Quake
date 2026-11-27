package moe.mchdf.quake.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class HitZonesTest {
    // 발이 y=100인 플레이어 기준
    private static final double FEET = 100.0D;
    private static final double STANDING_EYE = FEET + 1.62D;
    private static final double SNEAKING_EYE = FEET + 1.27D;

    @Test
    void standingPlayerHitAtHeadHeightIsHeadshot() {
        assertEquals(HitZone.HEAD, HitZones.of(Pose.STANDING, STANDING_EYE, FEET + 1.75D));
        assertEquals(HitZone.HEAD, HitZones.of(Pose.STANDING, STANDING_EYE, FEET + 1.51D));
    }

    @Test
    void standingPlayerHitBelowHeadIsBody() {
        assertEquals(HitZone.BODY, HitZones.of(Pose.STANDING, STANDING_EYE, FEET + 1.49D));
        assertEquals(HitZone.BODY, HitZones.of(Pose.STANDING, STANDING_EYE, FEET + 0.2D));
    }

    @Test
    void hitAboveHeadWithinRayMarginIsHeadshot() {
        // raySize 여유 때문에 머리 위(키 1.8 초과)에 찍힌 판정
        assertEquals(HitZone.HEAD, HitZones.of(Pose.STANDING, STANDING_EYE, FEET + 2.1D));
    }

    @Test
    void sneakingPlayerUsesLowerHead() {
        assertEquals(HitZone.HEAD, HitZones.of(Pose.SNEAKING, SNEAKING_EYE, FEET + 1.3D));
        assertEquals(HitZone.BODY, HitZones.of(Pose.SNEAKING, SNEAKING_EYE, FEET + 1.1D));
    }

    @Test
    void lyingPosesAreAlwaysBody() {
        double eye = FEET + 0.4D;
        assertEquals(HitZone.BODY, HitZones.of(Pose.SWIMMING, eye, FEET + 0.6D));
        assertEquals(HitZone.BODY, HitZones.of(Pose.FALL_FLYING, eye, FEET + 0.6D));
        assertEquals(HitZone.BODY, HitZones.of(Pose.SLEEPING, FEET + 0.2D, FEET + 0.3D));
    }

    @Test
    void readsPoseAndEyeHeightFromPlayer() {
        Player target = mock(Player.class);
        when(target.getPose()).thenReturn(Pose.STANDING);
        when(target.getEyeLocation()).thenReturn(new Location(null, 5.0D, STANDING_EYE, 5.0D));

        assertEquals(HitZone.HEAD, HitZones.of(target, new Vector(5.3D, FEET + 1.7D, 5.0D)));
        assertEquals(HitZone.BODY, HitZones.of(target, new Vector(5.3D, FEET + 1.0D, 5.0D)));
    }
}
