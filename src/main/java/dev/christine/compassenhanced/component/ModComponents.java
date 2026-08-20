package dev.christine.compassenhanced.component;

import dev.christine.compassenhanced.CompassEnhanced;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModComponents {
    public static final ComponentType<CompassConfigComponent> COMPASS_CONFIG = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            CompassEnhanced.id("config"),
            ComponentType.<CompassConfigComponent>builder()
                    .codec(CompassConfigComponent.CODEC)
                    .packetCodec(CompassConfigComponent.PACKET_CODEC)
                    .build()
    );

    private ModComponents() {
    }

    public static void initialize() {
        // Loading this class registers all component types.
    }
}
