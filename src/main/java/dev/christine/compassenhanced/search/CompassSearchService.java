package dev.christine.compassenhanced.search;

import dev.christine.compassenhanced.component.CompassConfigComponent;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public final class CompassSearchService {
    private CompassSearchService() {
    }

    public static Optional<SearchResult> search(
            ServerWorld world,
            ServerPlayerEntity player,
            CompassConfigComponent config
    ) {
        Item targetItem = Registries.ITEM.get(config.targetItem());

        Optional<SearchResult> blockResult = config.searchBlocks()
                ? BlockScanner.findNearest(world, player, targetItem, config.radius())
                : Optional.empty();
        Optional<SearchResult> droppedItemResult = config.searchDroppedItems()
                ? DroppedItemScanner.findNearest(world, player, targetItem, config.radius())
                : Optional.empty();
        Optional<SearchResult> containerResult = config.searchContainers()
                ? ContainerScanner.findNearest(world, player, targetItem, config.radius())
                : Optional.empty();

        return Stream.of(blockResult, droppedItemResult, containerResult)
                .flatMap(Optional::stream)
                .min(Comparator.comparingDouble(SearchResult::distanceSquared));
    }
}
