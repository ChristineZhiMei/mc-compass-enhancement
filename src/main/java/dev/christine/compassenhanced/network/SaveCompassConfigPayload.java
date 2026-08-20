package dev.christine.compassenhanced.network;

import dev.christine.compassenhanced.CompassEnhanced;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SaveCompassConfigPayload(
        Identifier targetItem,
        int radius,
        boolean blocks,
        boolean droppedItems,
        boolean containers
) implements CustomPayload {
    public static final Id<SaveCompassConfigPayload> ID =
            new Id<>(CompassEnhanced.id("save_config"));

    public static final PacketCodec<RegistryByteBuf, SaveCompassConfigPayload> CODEC = PacketCodec.tuple(
            Identifier.PACKET_CODEC,
            SaveCompassConfigPayload::targetItem,
            PacketCodecs.VAR_INT,
            SaveCompassConfigPayload::radius,
            PacketCodecs.BOOLEAN,
            SaveCompassConfigPayload::blocks,
            PacketCodecs.BOOLEAN,
            SaveCompassConfigPayload::droppedItems,
            PacketCodecs.BOOLEAN,
            SaveCompassConfigPayload::containers,
            SaveCompassConfigPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
