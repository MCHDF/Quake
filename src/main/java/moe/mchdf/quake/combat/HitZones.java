package moe.mchdf.quake.combat;

import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.util.Vector;

/**
 * 맞은 지점의 높이로 맞은 부위를 판정한다. 무기와 상관없이 쓸 수 있도록 레일건 코드와 분리해 둔다.
 *
 * <p>눈 높이에서 {@link #HEAD_BELOW_EYE}만큼 아래부터 위쪽을 머리로 본다. 서 있으면 발에서 1.5블록(눈 1.62),
 * 웅크리면 1.15블록(눈 1.27)부터가 머리다.
 *
 * <p>머리가 몸 위쪽에 있지 않은 자세(수영, 겉날개 비행, 잠자기 등)는 높이로 머리를 가를 수 없어 항상 몸통으로 본다.
 * 레이 트레이스를 판정 여유({@code raySize})와 함께 쓰면 맞은 지점이 실제 몸보다 바깥에 찍히는데, 머리 위를 스친
 * 판정(레일건은 0.1블록 이내)도 머리로 친다.
 */
public final class HitZones {
    /** 머리 판정이 시작되는 높이: 눈 높이에서 이만큼 아래 (블록) */
    static final double HEAD_BELOW_EYE = 0.12D;

    private HitZones() {
    }

    /** {@code hitPosition}은 맞은 지점의 월드 좌표다. */
    public static HitZone of(Player target, Vector hitPosition) {
        return of(target.getPose(), target.getEyeLocation().getY(), hitPosition.getY());
    }

    static HitZone of(Pose pose, double eyeY, double hitY) {
        if (pose != Pose.STANDING && pose != Pose.SNEAKING) {
            return HitZone.BODY;
        }
        return hitY >= eyeY - HEAD_BELOW_EYE ? HitZone.HEAD : HitZone.BODY;
    }
}
