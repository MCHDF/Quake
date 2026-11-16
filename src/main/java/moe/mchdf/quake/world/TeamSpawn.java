package moe.mchdf.quake.world;

import org.bukkit.Location;
import org.bukkit.World;

/** Coordinates and facing within the owning game world. */
public record TeamSpawn(double x, double y, double z, float yaw, float pitch) {
    public TeamSpawn {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("스폰 좌표는 유한한 숫자여야 합니다.");
        }
    }

    public Location location(World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }
}
