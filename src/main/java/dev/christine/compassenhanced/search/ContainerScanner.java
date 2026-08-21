package dev.christine.compassenhanced.search;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;

public final class ContainerScanner {
    private ContainerScanner() {
    }

    public static Optional<SearchResult> findNearest(
            ServerPlayerEntity player,
            Item targetItem,
            int radius,
            List<LoadedChunkAccess.LoadedChunk> chunks,
            double bestDistanceSquared
    ) {
        Vec3d origin = player.getPos();
        double radiusSquared = (double) radius * radius;
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

            for (BlockEntity blockEntity : loadedChunk.chunk().getBlockEntities().values()) {
                if (!(blockEntity instanceof Inventory inventory)) {
                    continue;
                }

                BlockPos blockPos = blockEntity.getPos();
                Vec3d target = Vec3d.ofCenter(blockPos);
                double distanceSquared = origin.squaredDistanceTo(target);
                if (distanceSquared > radiusSquared || distanceSquared >= currentBestDistanceSquared) {
                    continue;
                }
                if (containsTarget(inventory, targetItem)) {
                    nearest = new SearchResult(target, SearchSource.CONTAINER, distanceSquared);
                    currentBestDistanceSquared = distanceSquared;
                }
            }
        }
        return Optional.ofNullable(nearest);
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

    private static boolean containsTarget(Inventory inventory, Item targetItem) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStack(slot).isOf(targetItem)) {
                return true;
            }
        }
        return false;
    }
}
