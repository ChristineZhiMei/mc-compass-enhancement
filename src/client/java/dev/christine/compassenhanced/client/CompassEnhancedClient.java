package dev.christine.compassenhanced.client;

import dev.christine.compassenhanced.client.screen.CompassConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class CompassEnhancedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient()
                    || hand != Hand.MAIN_HAND
                    || player.isSneaking()) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(Items.COMPASS)) {
                return ActionResult.PASS;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new CompassConfigScreen(stack));
            return ActionResult.FAIL;
        });
    }
}
