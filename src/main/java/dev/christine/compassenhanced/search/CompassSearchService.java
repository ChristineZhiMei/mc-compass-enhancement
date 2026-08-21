package dev.christine.compassenhanced.search;

import dev.christine.compassenhanced.component.CompassConfigComponent;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Optional;

public final class CompassSearchService {
    private CompassSearchService() {
    }

    public static Optional<SearchResult> search(
            ServerWorld world,
            ServerPlayerEntity player,
            CompassConfigComponent config
    ) {
        Item targetItem = Registries.ITEM.get(config.targetItem());
        SearchResult best = null;

        if (config.searchDroppedItems()) {
            best = DroppedItemScanner.findNearest(world, player, targetItem, config.radius()).orElse(null);
        }

        boolean needsChunks = config.searchContainers() || config.searchBlocks();
        List<LoadedChunkAccess.LoadedChunk> chunks = needsChunks
                ? LoadedChunkAccess.collect(
                        world,
                        player.getPos(),
                        config.radius(),
                        best == null ? Double.POSITIVE_INFINITY : best.distanceSquared()
                )
                : List.of();

        if (config.searchContainers()) {
            Optional<SearchResult> containerResult = ContainerScanner.findNearest(
                    player,
                    targetItem,
                    config.radius(),
                    chunks,
                    best == null ? Double.POSITIVE_INFINITY : best.distanceSquared()
            );
            if (containerResult.isPresent()) {
                best = containerResult.get();
            }
        }

        if (config.searchBlocks()) {
            Optional<SearchResult> blockResult = BlockScanner.findNearest(
                    player,
                    targetItem,
                    config.radius(),
                    chunks,
                    best == null ? Double.POSITIVE_INFINITY : best.distanceSquared()
            );
            if (blockResult.isPresent()) {
                best = blockResult.get();
            }
        }

        return Optional.ofNullable(best);
    }
}
