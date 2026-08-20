package dev.christine.compassenhanced.network;

import dev.christine.compassenhanced.CompassEnhanced;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record ScanCompassPayload() implements CustomPayload {
    public static final ScanCompassPayload INSTANCE = new ScanCompassPayload();
    public static final Id<ScanCompassPayload> ID =
            new Id<>(CompassEnhanced.id("scan"));
    public static final PacketCodec<RegistryByteBuf, ScanCompassPayload> CODEC =
            PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
