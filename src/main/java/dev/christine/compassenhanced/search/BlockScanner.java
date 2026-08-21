package dev.christine.compassenhanced.search;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BlockScanner {
    private BlockScanner() {
    }

    public static Optional<SearchResult> findNearest(
            ServerPlayerEntity player,
            Item targetItem,
            int radius,
            List<LoadedChunkAccess.LoadedChunk> chunks,
            double bestDistanceSquared
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
        double currentBestDistanceSquared = bestDistanceSquared;
        for (LoadedChunkAccess.LoadedChunk loadedChunk : chunks) {
            if (cannotBeat(
                    loadedChunk.minHorizontalDistanceSquared(),
                    radiusSquared,
                    currentBestDistanceSquared
            )) {
                break;
            }

            List<SectionCandidate> candidates = collectSectionCandidates(
                    loadedChunk,
                    targetBlock,
                    origin,
                    minX,
                    maxX,
                    minY,
                    maxY,
                    minZ,
                    maxZ
            );
            for (SectionCandidate candidate : candidates) {
                if (cannotBeat(
                        candidate.minDistanceSquared(),
                        radiusSquared,
                        currentBestDistanceSquared
                )) {
                    break;
                }

                SearchResult result = scanSection(
                        candidate,
                        targetBlock,
                        origin,
                        radiusSquared,
                        currentBestDistanceSquared
                );
                if (result != null) {
                    nearest = result;
                    currentBestDistanceSquared = result.distanceSquared();
                }
            }
        }
        return Optional.ofNullable(nearest);
    }

    private static List<SectionCandidate> collectSectionCandidates(
            LoadedChunkAccess.LoadedChunk loadedChunk,
            Block targetBlock,
            Vec3d origin,
            int minX,
            int maxX,
            int minY,
            int maxY,
            int minZ,
            int maxZ
    ) {
        ChunkSection[] sections = loadedChunk.chunk().getSectionArray();
        int chunkStartX = loadedChunk.chunk().getPos().getStartX();
        int chunkStartZ = loadedChunk.chunk().getPos().getStartZ();
        int startLocalX = Math.max(0, minX - chunkStartX);
        int endLocalX = Math.min(15, maxX - chunkStartX);
        int startLocalZ = Math.max(0, minZ - chunkStartZ);
        int endLocalZ = Math.min(15, maxZ - chunkStartZ);
        List<SectionCandidate> candidates = new ArrayList<>();

        if (startLocalX > endLocalX || startLocalZ > endLocalZ) {
            return List.of();
        }

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            ChunkSection section = sections[sectionIndex];
            int sectionY = loadedChunk.chunk().getBottomSectionCoord() + sectionIndex;
            int sectionStartY = ChunkSectionPos.getBlockCoord(sectionY);
            if (section.isEmpty()
                    || sectionStartY > maxY
                    || sectionStartY + 15 < minY
                    || !section.hasAny(state -> state.isOf(targetBlock))) {
                continue;
            }

            int startLocalY = Math.max(0, minY - sectionStartY);
            int endLocalY = Math.min(15, maxY - sectionStartY);
            double minDistanceSquared = minDistanceSquared(
                    origin,
                    chunkStartX + startLocalX + 0.5,
                    chunkStartX + endLocalX + 0.5,
                    sectionStartY + startLocalY + 0.5,
                    sectionStartY + endLocalY + 0.5,
                    chunkStartZ + startLocalZ + 0.5,
                    chunkStartZ + endLocalZ + 0.5
            );
            candidates.add(new SectionCandidate(
                    section,
                    chunkStartX,
                    sectionStartY,
                    chunkStartZ,
                    startLocalX,
                    endLocalX,
                    startLocalY,
                    endLocalY,
                    startLocalZ,
                    endLocalZ,
                    minDistanceSquared
            ));
        }

        candidates.sort(Comparator.comparingDouble(SectionCandidate::minDistanceSquared));
        return candidates;
    }

    private static SearchResult scanSection(
            SectionCandidate candidate,
            Block targetBlock,
            Vec3d origin,
            double radiusSquared,
            double bestDistanceSquared
    ) {
        SearchResult nearest = null;
        double currentBestDistanceSquared = bestDistanceSquared;

        for (int localY = candidate.startLocalY(); localY <= candidate.endLocalY(); localY++) {
            for (int localZ = candidate.startLocalZ(); localZ <= candidate.endLocalZ(); localZ++) {
                for (int localX = candidate.startLocalX(); localX <= candidate.endLocalX(); localX++) {
                    BlockState state = candidate.section().getBlockState(localX, localY, localZ);
                    if (!state.isOf(targetBlock)) {
                        continue;
                    }

                    Vec3d target = new Vec3d(
                            candidate.chunkStartX() + localX + 0.5,
                            candidate.sectionStartY() + localY + 0.5,
                            candidate.chunkStartZ() + localZ + 0.5
                    );
                    double distanceSquared = origin.squaredDistanceTo(target);
                    if (distanceSquared <= radiusSquared && distanceSquared < currentBestDistanceSquared) {
                        nearest = new SearchResult(target, SearchSource.BLOCK, distanceSquared);
                        currentBestDistanceSquared = distanceSquared;
                    }
                }
            }
        }
        return nearest;
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

    private static double minDistanceSquared(
            Vec3d origin,
            double minX,
            double maxX,
            double minY,
            double maxY,
            double minZ,
            double maxZ
    ) {
        double deltaX = distanceToInterval(origin.x, minX, maxX);
        double deltaY = distanceToInterval(origin.y, minY, maxY);
        double deltaZ = distanceToInterval(origin.z, minZ, maxZ);
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
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

    private record SectionCandidate(
            ChunkSection section,
            int chunkStartX,
            int sectionStartY,
            int chunkStartZ,
            int startLocalX,
            int endLocalX,
            int startLocalY,
            int endLocalY,
            int startLocalZ,
            int endLocalZ,
            double minDistanceSquared
    ) {
    }
}
