package dev.christine.compassenhanced.search;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Optional;

public final class ContainerScanner {
    private ContainerScanner() {
    }

    public static Optional<SearchResult> findNearest(
            ServerWorld world,
            ServerPlayerEntity player,
            Item targetItem,
            int radius
    ) {
        Vec3d origin = player.getPos();
        double radiusSquared = (double) radius * radius;
        int minChunkX = Math.floorDiv(MathHelper.floor(origin.x - radius), 16);
        int maxChunkX = Math.floorDiv(MathHelper.floor(origin.x + radius), 16);
        int minChunkZ = Math.floorDiv(MathHelper.floor(origin.z - radius), 16);
        int maxChunkZ = Math.floorDiv(MathHelper.floor(origin.z + radius), 16);

        SearchResult nearest = null;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                WorldChunk chunk = LoadedChunkAccess.get(world, chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof Inventory inventory)
                            || !containsTarget(inventory, targetItem)) {
                        continue;
                    }

                    BlockPos blockPos = blockEntity.getPos();
                    Vec3d target = Vec3d.ofCenter(blockPos);
                    double distanceSquared = origin.squaredDistanceTo(target);
                    if (distanceSquared <= radiusSquared
                            && (nearest == null || distanceSquared < nearest.distanceSquared())) {
                        nearest = new SearchResult(target, SearchSource.CONTAINER, distanceSquared);
                    }
                }
            }
        }
        return Optional.ofNullable(nearest);
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
