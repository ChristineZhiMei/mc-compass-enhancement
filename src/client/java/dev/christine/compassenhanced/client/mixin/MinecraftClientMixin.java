package dev.christine.compassenhanced.client.mixin;

import dev.christine.compassenhanced.client.history.SearchHistoryStore;
import dev.christine.compassenhanced.component.CompassConfigComponent;
import dev.christine.compassenhanced.component.ModComponents;
import dev.christine.compassenhanced.network.ScanCompassPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void compassEnhanced$scanConfiguredCompass(CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (client.player == null) {
            return;
        }

        ItemStack mainHandStack = client.player.getMainHandStack();
        if (!mainHandStack.isOf(Items.COMPASS)) {
            return;
        }

        CompassConfigComponent config = mainHandStack.get(ModComponents.COMPASS_CONFIG);
        if (config == null) {
            return;
        }

        ClientPlayNetworking.send(ScanCompassPayload.INSTANCE);
        SearchHistoryStore.getInstance().recordAttempt(config.targetItem());
        cir.setReturnValue(false);
    }
}
