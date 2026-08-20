package dev.christine.compassenhanced;

import dev.christine.compassenhanced.component.ModComponents;
import dev.christine.compassenhanced.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public final class CompassEnhanced implements ModInitializer {
    public static final String MOD_ID = "compass_enhanced";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.initialize();
        ModNetworking.initialize();
    }
}
