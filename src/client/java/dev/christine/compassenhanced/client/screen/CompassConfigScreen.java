package dev.christine.compassenhanced.client.screen;

import dev.christine.compassenhanced.client.history.SearchHistoryStore;
import dev.christine.compassenhanced.component.CompassConfigComponent;
import dev.christine.compassenhanced.component.ModComponents;
import dev.christine.compassenhanced.network.SaveCompassConfigPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CompassConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 214;

    private Identifier targetItemId;
    private int radius;
    private boolean searchBlocks;
    private boolean searchDroppedItems;
    private boolean searchContainers;

    private ButtonWidget targetButton;
    private SearchHistoryWidget historyWidget;
    private ButtonWidget blocksButton;
    private ButtonWidget droppedItemsButton;
    private ButtonWidget containersButton;
    private ButtonWidget saveButton;

    public CompassConfigScreen(ItemStack compass) {
        super(Text.translatable("screen.compass_enhanced.config.title"));
        CompassConfigComponent config = compass.get(ModComponents.COMPASS_CONFIG);
        if (config == null) {
            radius = CompassConfigComponent.DEFAULT_RADIUS;
            searchBlocks = true;
            return;
        }

        targetItemId = config.targetItem();
        radius = config.radius();
        searchBlocks = config.searchBlocks();
        searchDroppedItems = config.searchDroppedItems();
        searchContainers = config.searchContainers();
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(8, (height - PANEL_HEIGHT) / 2);

        targetButton = addDrawableChild(ButtonWidget.builder(targetMessage(), button -> openItemSelector())
                .dimensions(left, top + 24, PANEL_WIDTH, 20)
                .build());

        historyWidget = addDrawableChild(new SearchHistoryWidget(
                left,
                top + 59,
                PANEL_WIDTH,
                24,
                SearchHistoryStore.getInstance().entries(),
                targetItemId,
                this::selectTarget
        ));

        addDrawableChild(new RadiusSlider(left, top + 98, PANEL_WIDTH, 20));

        blocksButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            searchBlocks = !searchBlocks;
            refreshSourceButtons();
        }).dimensions(left, top + 133, 146, 20).build());

        droppedItemsButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            searchDroppedItems = !searchDroppedItems;
            refreshSourceButtons();
        }).dimensions(left + 154, top + 133, 146, 20).build());

        containersButton = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            searchContainers = !searchContainers;
            refreshSourceButtons();
        }).dimensions(left, top + 157, PANEL_WIDTH, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(left, top + 193, 146, 20)
                .build());
        saveButton = addDrawableChild(ButtonWidget.builder(Text.translatable("gui.save"), button -> save())
                .dimensions(left + 154, top + 193, 146, 20)
                .build());

        refreshSourceButtons();
    }

    private void openItemSelector() {
        if (client == null) {
            return;
        }
        client.setScreen(new ItemSelectorScreen(this, targetItemId, this::selectTarget));
    }

    private void selectTarget(Identifier itemId) {
        targetItemId = itemId;
        if (historyWidget != null) {
            historyWidget.setSelectedItemId(itemId);
        }
        if (!isBlockItem()) {
            searchBlocks = false;
        }
        refreshSourceButtons();
    }

    private void save() {
        if (targetItemId == null || !hasAnySource()) {
            return;
        }

        ClientPlayNetworking.send(new SaveCompassConfigPayload(
                targetItemId,
                radius,
                searchBlocks,
                searchDroppedItems,
                searchContainers
        ));
        close();
    }

    private void refreshSourceButtons() {
        if (blocksButton == null) {
            return;
        }

        boolean blockTarget = targetItemId == null || isBlockItem();
        if (!blockTarget) {
            searchBlocks = false;
        }
        blocksButton.active = blockTarget;
        blocksButton.setMessage(sourceMessage("block", searchBlocks));
        droppedItemsButton.setMessage(sourceMessage("dropped_items", searchDroppedItems));
        containersButton.setMessage(sourceMessage("containers", searchContainers));
        saveButton.active = targetItemId != null && hasAnySource();
        targetButton.setMessage(targetMessage());
    }

    private Text targetMessage() {
        if (targetItemId == null) {
            return Text.translatable("screen.compass_enhanced.config.target.empty");
        }
        Item item = Registries.ITEM.get(targetItemId);
        return Text.translatable(
                "screen.compass_enhanced.config.target.value",
                item.getName(),
                targetItemId.toString()
        );
    }

    private Text sourceMessage(String source, boolean enabled) {
        return Text.translatable(
                "screen.compass_enhanced.config.source.value",
                enabled ? "✔" : "✘",
                Text.translatable("screen.compass_enhanced.config.source." + source)
        );
    }

    private boolean isBlockItem() {
        return targetItemId != null && Registries.ITEM.get(targetItemId) instanceof BlockItem;
    }

    private boolean hasAnySource() {
        return searchBlocks || searchDroppedItems || searchContainers;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(8, (height - PANEL_HEIGHT) / 2);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top, 0xFFFFFF);
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.compass_enhanced.config.target"),
                left,
                top + 14,
                0xA0A0A0
        );
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.compass_enhanced.config.history"),
                left,
                top + 48,
                0xA0A0A0
        );
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.compass_enhanced.config.radius"),
                left,
                top + 87,
                0xA0A0A0
        );
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.compass_enhanced.config.sources"),
                left,
                top + 122,
                0xA0A0A0
        );

        super.render(context, mouseX, mouseY, delta);

        if (targetItemId != null && !isBlockItem()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.translatable("screen.compass_enhanced.config.block_warning"),
                    width / 2,
                    top + 181,
                    0xFFAA00
            );
        } else if (targetItemId != null && !hasAnySource()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.translatable("screen.compass_enhanced.config.source_warning"),
                    width / 2,
                    top + 181,
                    0xFF5555
            );
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private final class RadiusSlider extends SliderWidget {
        private RadiusSlider(int x, int y, int width, int height) {
            super(
                    x,
                    y,
                    width,
                    height,
                    Text.empty(),
                    (double) (radius - CompassConfigComponent.MIN_RADIUS)
                            / (CompassConfigComponent.MAX_RADIUS - CompassConfigComponent.MIN_RADIUS)
            );
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.translatable("screen.compass_enhanced.config.radius_value", radius));
        }

        @Override
        protected void applyValue() {
            int span = CompassConfigComponent.MAX_RADIUS - CompassConfigComponent.MIN_RADIUS;
            radius = CompassConfigComponent.MIN_RADIUS + (int) Math.round(value * span);
            updateMessage();
        }
    }
}
