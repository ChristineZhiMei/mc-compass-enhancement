package dev.christine.compassenhanced.search;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;

public final class BlockScanner {
    private BlockScanner() {
    }

    public static Optional<SearchResult> findNearest(
            ServerWorld world,
            ServerPlayerEntity player,
            Item targetItem,
            int radius
    ) {
        if (!(targetItem instanceof BlockItem blockItem)) {
            return Optional.empty();
        }

        Block targetBlock = blockItem.getBlock();
        Vec3d origin = player.getPos();
        double radiusSquared = (double) radius * radius;
        int minX = MathHelper.floor(origin.x - radius);
        int maxX = MathHelper.floor(origin.x + radius);
        int minY = MathHelper.floor(origin.y - radius);
        int maxY = MathHelper.floor(origin.y + radius);
        int minZ = MathHelper.floor(origin.z - radius);
        int maxZ = MathHelper.floor(origin.z + radius);

        SearchResult nearest = null;
        for (int chunkX = Math.floorDiv(minX, 16); chunkX <= Math.floorDiv(maxX, 16); chunkX++) {
            for (int chunkZ = Math.floorDiv(minZ, 16); chunkZ <= Math.floorDiv(maxZ, 16); chunkZ++) {
                WorldChunk chunk = LoadedChunkAccess.get(world, chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                nearest = scanChunk(
                        chunk,
                        targetBlock,
                        origin,
                        radiusSquared,
                        minX,
                        maxX,
                        minY,
                        maxY,
                        minZ,
                        maxZ,
                        nearest
                );
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static SearchResult scanChunk(
            WorldChunk chunk,
            Block targetBlock,
            Vec3d origin,
            double radiusSquared,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ,
            SearchResult nearest
    ) {
        ChunkSection[] sections = chunk.getSectionArray();
        int chunkStartX = chunk.getPos().getStartX();
        int chunkStartZ = chunk.getPos().getStartZ();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            int sectionY = chunk.getBottomSectionCoord() + sectionIndex;
            int sectionStartY = ChunkSectionPos.getBlockCoord(sectionY);
            if (section.isEmpty()
                    || sectionStartY > maxY
                    || sectionStartY + 15 < minY
                    || !section.hasAny(state -> state.isOf(targetBlock))) {
                continue;
            }

            int startLocalX = Math.max(0, minX - chunkStartX);
            int endLocalX = Math.min(15, maxX - chunkStartX);
            int startLocalY = Math.max(0, minY - sectionStartY);
            int endLocalY = Math.min(15, maxY - sectionStartY);
            int startLocalZ = Math.max(0, minZ - chunkStartZ);
            int endLocalZ = Math.min(15, maxZ - chunkStartZ);

            for (int localY = startLocalY; localY <= endLocalY; localY++) {
                for (int localZ = startLocalZ; localZ <= endLocalZ; localZ++) {
                    for (int localX = startLocalX; localX <= endLocalX; localX++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        if (!state.isOf(targetBlock)) {
                            continue;
                        }

                        Vec3d target = new Vec3d(
                                chunkStartX + localX + 0.5,
                                sectionStartY + localY + 0.5,
                                chunkStartZ + localZ + 0.5
                        );
                        double distanceSquared = origin.squaredDistanceTo(target);
                        if (distanceSquared <= radiusSquared
                                && (nearest == null || distanceSquared < nearest.distanceSquared())) {
                            nearest = new SearchResult(target, SearchSource.BLOCK, distanceSquared);
                        }
                    }
                }
            }
        }
        return nearest;
    }
}
