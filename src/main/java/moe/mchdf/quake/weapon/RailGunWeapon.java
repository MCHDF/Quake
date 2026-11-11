package moe.mchdf.quake.weapon;

import moe.mchdf.quake.game.GameTeams;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class RailGunWeapon {
    private final GameTeams gameTeams;

    public RailGunWeapon(GameTeams gameTeams) {
        this.gameTeams = gameTeams;
    }

    public void fire(Player shooter) {
        if (shooter == null || shooter.hasCooldown(Material.IRON_SHOVEL)) {
            return;
        }

        shooter.setCooldown(Material.IRON_SHOVEL, 36);

        Location start = shooter.getEyeLocation();
        Vector direction = shooter.getEyeLocation().getDirection();
        RayTraceResult result = shooter.getWorld().rayTrace(
                start,
                direction,
                50,
                FluidCollisionMode.NEVER,
                false,
                0.4D,
                entity -> entity != shooter && entity instanceof Player
        );

        if (result == null || result.getHitEntity() == null || result.getHitBlock() != null) {
            drawTrail(start, direction, shooter.getWorld());
            playLaunchSound(shooter);
            return;
        }

        Entity hitEntity = result.getHitEntity();
        if (!(hitEntity instanceof Player hitPlayer)) {
            drawTrail(start, direction, shooter.getWorld());
            playLaunchSound(shooter);
            return;
        }

        if (!gameTeams.areEnemies(shooter, hitPlayer)) {
            drawTrail(start, hitPlayer.getLocation().toVector().subtract(start.toVector()), shooter.getWorld());
            playLaunchSound(shooter);
            return;
        }

        gameTeams.awardPoint(shooter);
        shooter.getWorld().playSound(hitPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);
        drawTrail(start, hitPlayer.getLocation().toVector().subtract(start.toVector()), shooter.getWorld());
        playLaunchSound(shooter);
    }

    private void drawTrail(Location start, Vector direction, World world) {
        Vector step = direction.clone().normalize().multiply(0.8);
        Location point = start.clone();
        for (int i = 0; i < 30; i++) {
            point.add(step);
            if (point.getBlock().isEmpty()) {
                world.spawnParticle(Particle.DUST, point, 2, 0.1, 0.1, 0.1, 0.01,
                        new DustOptions(Color.fromRGB(255, 0, 0), 1.0F));

            } else {
                break;
            }
        }
    }

    private void playLaunchSound(Player shooter) {
        World world = shooter.getWorld();
        Location loc = shooter.getLocation();
        world.playSound(loc, Sound.ENTITY_TNT_PRIMED, 1.0F, 1.0F);
        world.playSound(loc, Sound.ENTITY_SKELETON_AMBIENT, 1.0F, 1.0F);
        world.playSound(loc, Sound.BLOCK_PISTON_EXTEND, 1.0F, 1.0F);
        world.playSound(loc, Sound.ENTITY_SKELETON_STEP, 1.0F, 1.0F);
    }
}
