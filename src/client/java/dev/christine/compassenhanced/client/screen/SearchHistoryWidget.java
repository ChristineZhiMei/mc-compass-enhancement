package dev.christine.compassenhanced.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

final class SearchHistoryWidget extends ClickableWidget {
    private static final int CELL_SIZE = 24;
    private static final int ICON_OFFSET = 4;

    private final List<Identifier> itemIds;
    private final Consumer<Identifier> onSelected;
    private Identifier selectedItemId;

    SearchHistoryWidget(
            int x,
            int y,
            int width,
            int height,
            List<Identifier> itemIds,
            Identifier selectedItemId,
            Consumer<Identifier> onSelected
    ) {
        super(x, y, width, height, Text.translatable("screen.compass_enhanced.config.history"));
        this.itemIds = List.copyOf(itemIds);
        this.selectedItemId = selectedItemId;
        this.onSelected = onSelected;
    }

    void setSelectedItemId(Identifier selectedItemId) {
        this.selectedItemId = selectedItemId;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY(), getRight(), getBottom(), 0x88000000);
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), 0xFF808080);

        if (itemIds.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    MinecraftClient.getInstance().textRenderer,
                    Text.translatable("screen.compass_enhanced.config.history.empty"),
                    getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2,
                    0xA0A0A0
            );
            return;
        }

        Identifier hoveredItemId = null;
        for (int index = 0; index < itemIds.size(); index++) {
            int cellX = getX() + index * CELL_SIZE;
            Identifier itemId = itemIds.get(index);
            boolean selected = itemId.equals(selectedItemId);
            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= getY() && mouseY < getBottom();
            if (selected || hovered) {
                context.fill(
                        cellX + 1,
                        getY() + 1,
                        cellX + CELL_SIZE - 1,
                        getBottom() - 1,
                        selected ? 0xAA55AAFF : 0x66FFFFFF
                );
            }

            ItemStack stack = Registries.ITEM.get(itemId).getDefaultStack();
            context.drawItem(stack, cellX + ICON_OFFSET, getY() + ICON_OFFSET);
            if (hovered) {
                hoveredItemId = itemId;
            }
        }

        if (hoveredItemId != null) {
            ItemStack stack = Registries.ITEM.get(hoveredItemId).getDefaultStack();
            context.drawTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    List.of(stack.getName(), Text.literal(hoveredItemId.toString())),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !active || !visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int index = (int) (mouseX - getX()) / CELL_SIZE;
        if (index < 0 || index >= itemIds.size()) {
            return false;
        }

        playClickSound(MinecraftClient.getInstance().getSoundManager());
        onSelected.accept(itemIds.get(index));
        return true;
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(
                NarrationPart.TITLE,
                Text.translatable("screen.compass_enhanced.config.history_narration", itemIds.size())
        );
    }
}
