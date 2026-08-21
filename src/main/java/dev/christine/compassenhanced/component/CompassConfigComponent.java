package dev.christine.compassenhanced.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;

public record CompassConfigComponent(
        Identifier targetItem,
        int radius,
        boolean searchBlocks,
        boolean searchDroppedItems,
        boolean searchContainers
) {
    public static final int MIN_RADIUS = 8;
    public static final int DEFAULT_RADIUS = 32;
    public static final int MAX_RADIUS = 256;

    public static final Codec<CompassConfigComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("target_item").forGetter(CompassConfigComponent::targetItem),
            Codec.intRange(MIN_RADIUS, MAX_RADIUS).fieldOf("radius").forGetter(CompassConfigComponent::radius),
            Codec.BOOL.fieldOf("search_blocks").forGetter(CompassConfigComponent::searchBlocks),
            Codec.BOOL.fieldOf("search_dropped_items").forGetter(CompassConfigComponent::searchDroppedItems),
            Codec.BOOL.fieldOf("search_containers").forGetter(CompassConfigComponent::searchContainers)
    ).apply(instance, CompassConfigComponent::new));

    public static final PacketCodec<RegistryByteBuf, CompassConfigComponent> PACKET_CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC,
            CompassConfigComponent::targetItem,
            PacketCodecs.VAR_INT,
            CompassConfigComponent::radius,
            PacketCodecs.BOOLEAN,
            CompassConfigComponent::searchBlocks,
            PacketCodecs.BOOLEAN,
            CompassConfigComponent::searchDroppedItems,
            PacketCodecs.BOOLEAN,
            CompassConfigComponent::searchContainers,
            CompassConfigComponent::new
    );

    public boolean hasAnySource() {
        return searchBlocks || searchDroppedItems || searchContainers;
    }
}
