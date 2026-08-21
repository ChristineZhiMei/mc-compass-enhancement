package dev.christine.compassenhanced.search;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class LoadedChunkAccess {
    private LoadedChunkAccess() {
    }

    static List<LoadedChunk> collect(
            ServerWorld world,
            Vec3d origin,
            int radius,
            double bestDistanceSquared
    ) {
        int minChunkX = Math.floorDiv(MathHelper.floor(origin.x - radius), 16);
        int maxChunkX = Math.floorDiv(MathHelper.floor(origin.x + radius), 16);
        int minChunkZ = Math.floorDiv(MathHelper.floor(origin.z - radius), 16);
        int maxChunkZ = Math.floorDiv(MathHelper.floor(origin.z + radius), 16);
        double radiusSquared = (double) radius * radius;
        List<LoadedChunk> chunks = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                double minX = chunkX * 16.0 + 0.5;
                double maxX = minX + 15.0;
                double minZ = chunkZ * 16.0 + 0.5;
                double maxZ = minZ + 15.0;
                double deltaX = distanceToInterval(origin.x, minX, maxX);
                double deltaZ = distanceToInterval(origin.z, minZ, maxZ);
                double minHorizontalDistanceSquared = deltaX * deltaX + deltaZ * deltaZ;
                if (cannotBeat(minHorizontalDistanceSquared, radiusSquared, bestDistanceSquared)) {
                    continue;
                }

                Chunk chunk = world.getChunkManager().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (!(chunk instanceof WorldChunk worldChunk)) {
                    continue;
                }

                chunks.add(new LoadedChunk(worldChunk, minHorizontalDistanceSquared));
            }
        }

        chunks.sort(Comparator.comparingDouble(LoadedChunk::minHorizontalDistanceSquared));
        return List.copyOf(chunks);
    }

    private static boolean cannotBeat(
            double lowerBoundSquared,
            double radiusSquared,
            double bestDistanceSquared
    ) {
        if (Double.isFinite(bestDistanceSquared)) {
            return lowerBoundSquared >= Math.min(radiusSquared, bestDistanceSquared);
        }
        return lowerBoundSquared > radiusSquared;
    }

    private static double distanceToInterval(double value, double min, double max) {
        if (value < min) {
            return min - value;
        }
        if (value > max) {
            return value - max;
        }
        return 0.0;
    }

    record LoadedChunk(WorldChunk chunk, double minHorizontalDistanceSquared) {
    }
}
