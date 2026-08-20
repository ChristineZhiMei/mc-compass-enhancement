package dev.christine.compassenhanced.search;

import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class DroppedItemScanner {
    private DroppedItemScanner() {
    }

    public static Optional<SearchResult> findNearest(
            ServerWorld world,
            ServerPlayerEntity player,
            Item targetItem,
            int radius
    ) {
        Vec3d origin = player.getPos();
        double radiusSquared = (double) radius * radius;
        Box searchBox = new Box(
                origin.x - radius,
                origin.y - radius,
                origin.z - radius,
                origin.x + radius,
                origin.y + radius,
                origin.z + radius
        );

        SearchResult nearest = null;
        for (ItemEntity entity : world.getEntitiesByClass(
                ItemEntity.class,
                searchBox,
                itemEntity -> itemEntity.isAlive() && itemEntity.getStack().isOf(targetItem)
        )) {
            Vec3d target = entity.getPos();
            double distanceSquared = origin.squaredDistanceTo(target);
            if (distanceSquared <= radiusSquared
                    && (nearest == null || distanceSquared < nearest.distanceSquared())) {
                nearest = new SearchResult(target, SearchSource.DROPPED_ITEM, distanceSquared);
            }
        }
        return Optional.ofNullable(nearest);
    }
}
