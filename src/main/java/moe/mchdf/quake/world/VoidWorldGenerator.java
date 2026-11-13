package moe.mchdf.quake.world;

import java.util.Random;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.ChunkGenerator;

final class VoidWorldGenerator extends ChunkGenerator {
    @Override
    public ChunkData generateChunkData(
            World world, Random random, int chunkX, int chunkZ, BiomeGrid biome
    ) {
        ChunkData data = createChunkData(world);
        for (int blockX = -2; blockX <= 2; blockX++) {
            for (int blockZ = -2; blockZ <= 2; blockZ++) {
                if (Math.floorDiv(blockX, 16) == chunkX && Math.floorDiv(blockZ, 16) == chunkZ) {
                    data.setBlock(Math.floorMod(blockX, 16), 64, Math.floorMod(blockZ, 16), Material.STONE);
                }
            }
        }
        return data;
    }
}
