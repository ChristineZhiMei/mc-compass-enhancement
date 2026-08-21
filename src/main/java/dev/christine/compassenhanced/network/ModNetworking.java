package dev.christine.compassenhanced.network;

import dev.christine.compassenhanced.component.CompassConfigComponent;
import dev.christine.compassenhanced.component.ModComponents;
import dev.christine.compassenhanced.search.CompassSearchService;
import dev.christine.compassenhanced.search.SearchResult;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LodestoneTrackerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ModNetworking {
    private static final long SCAN_COOLDOWN_TICKS = 10L;
    private static final Map<UUID, Long> NEXT_SCAN_TICK = new HashMap<>();

    private ModNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.playC2S().register(SaveCompassConfigPayload.ID, SaveCompassConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ScanCompassPayload.ID, ScanCompassPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SaveCompassConfigPayload.ID, ModNetworking::handleSave);
        ServerPlayNetworking.registerGlobalReceiver(ScanCompassPayload.ID, ModNetworking::handleScan);
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> NEXT_SCAN_TICK.remove(handler.player.getUuid())
        );
    }

    private static void handleSave(
            SaveCompassConfigPayload payload,
            ServerPlayNetworking.Context context
    ) {
        ServerPlayerEntity player = context.player();
        ItemStack compass = player.getMainHandStack();
        if (!compass.isOf(Items.COMPASS)) {
            sendInvalidConfig(player);
            return;
        }

        Optional<Item> targetItem = getTargetItem(payload.targetItem());
        if (targetItem.isEmpty()
                || payload.radius() < CompassConfigComponent.MIN_RADIUS
                || payload.radius() > CompassConfigComponent.MAX_RADIUS
                || !(payload.blocks() || payload.droppedItems() || payload.containers())
                || (payload.blocks() && !(targetItem.get() instanceof BlockItem))) {
            sendInvalidConfig(player);
            return;
        }

        CompassConfigComponent config = new CompassConfigComponent(
                payload.targetItem(),
                payload.radius(),
                payload.blocks(),
                payload.droppedItems(),
                payload.containers()
        );
        compass.set(ModComponents.COMPASS_CONFIG, config);
        compass.remove(DataComponentTypes.LODESTONE_TRACKER);
        player.sendMessage(Text.translatable("message.compass_enhanced.config_saved"), true);
    }

    private static void handleScan(
            ScanCompassPayload payload,
            ServerPlayNetworking.Context context
    ) {
        ServerPlayerEntity player = context.player();
        ItemStack compass = player.getMainHandStack();
        if (!compass.isOf(Items.COMPASS)) {
            return;
        }

        CompassConfigComponent config = compass.get(ModComponents.COMPASS_CONFIG);
        if (config == null) {
            player.sendMessage(Text.translatable("message.compass_enhanced.not_configured"), true);
            return;
        }

        Optional<Item> targetItem = getTargetItem(config.targetItem());
        if (targetItem.isEmpty() || !isValid(config, targetItem.get())) {
            compass.remove(DataComponentTypes.LODESTONE_TRACKER);
            sendInvalidConfig(player);
            return;
        }

        long currentTick = player.getServerWorld().getTime();
        long nextAllowedTick = NEXT_SCAN_TICK.getOrDefault(player.getUuid(), Long.MIN_VALUE);
        if (currentTick < nextAllowedTick) {
            return;
        }
        NEXT_SCAN_TICK.put(player.getUuid(), currentTick + SCAN_COOLDOWN_TICKS);

        Optional<SearchResult> result = CompassSearchService.search(player.getServerWorld(), player, config);
        if (result.isEmpty()) {
            compass.remove(DataComponentTypes.LODESTONE_TRACKER);
            player.sendMessage(
                    Text.translatable(
                            "message.compass_enhanced.not_found",
                            config.radius(),
                            Text.translatable(targetItem.get().getTranslationKey())
                    ),
                    true
            );
            return;
        }

        SearchResult nearest = result.get();
        double deltaY = nearest.target().y - player.getY();
        String direction = deltaY > 2.0
                ? "up"
                : deltaY < -2.0 ? "down" : "middle";
        BlockPos targetPos = BlockPos.ofFloored(nearest.target());
        compass.set(
                DataComponentTypes.LODESTONE_TRACKER,
                new LodestoneTrackerComponent(
                        Optional.of(GlobalPos.create(player.getServerWorld().getRegistryKey(), targetPos)),
                        false
                )
        );
        player.sendMessage(
                Text.translatable(
                        "message.compass_enhanced.found",
                        Text.translatable(targetItem.get().getTranslationKey()),
                        Math.round(Math.sqrt(nearest.distanceSquared())),
                        Text.translatable(nearest.source().translationKey()),
                        Text.translatable("message.compass_enhanced.direction." + direction)
                ),
                true
        );
    }

    private static Optional<Item> getTargetItem(net.minecraft.util.Identifier targetId) {
        if (!Registries.ITEM.containsId(targetId)) {
            return Optional.empty();
        }
        Item targetItem = Registries.ITEM.get(targetId);
        return targetItem == Items.AIR ? Optional.empty() : Optional.of(targetItem);
    }

    private static boolean isValid(CompassConfigComponent config, Item targetItem) {
        return config.radius() >= CompassConfigComponent.MIN_RADIUS
                && config.radius() <= CompassConfigComponent.MAX_RADIUS
                && config.hasAnySource()
                && (!config.searchBlocks() || targetItem instanceof BlockItem);
    }

    private static void sendInvalidConfig(ServerPlayerEntity player) {
        player.sendMessage(Text.translatable("message.compass_enhanced.invalid_config"), true);
    }
}
