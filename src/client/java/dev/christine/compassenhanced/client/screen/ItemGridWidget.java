package dev.christine.compassenhanced.client.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.function.Consumer;

final class ItemGridWidget extends ScrollableWidget {
    private static final int CELL_SIZE = 26;
    private static final int PADDING = 4;

    private final Identifier selectedItemId;
    private final Consumer<Identifier> onSelected;
    private List<ItemSelectorScreen.ItemEntry> items;

    ItemGridWidget(
            int x,
            int y,
            int width,
            int height,
            List<ItemSelectorScreen.ItemEntry> items,
            Identifier selectedItemId,
            Consumer<Identifier> onSelected
    ) {
        super(x, y, width, height, Text.translatable("screen.compass_enhanced.selector.grid"));
        this.items = items;
        this.selectedItemId = selectedItemId;
        this.onSelected = onSelected;
    }

    void setItems(List<ItemSelectorScreen.ItemEntry> items) {
        this.items = items;
        setScrollY(0.0);
        refreshScroll();
    }

    int getItemCount() {
        return items.size();
    }

    @Override
    protected int getContentsHeightWithPadding() {
        int columns = getColumns();
        int rows = (items.size() + columns - 1) / columns;
        return PADDING * 2 + rows * CELL_SIZE;
    }

    @Override
    protected double getDeltaYPerScroll() {
        return CELL_SIZE;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(getX(), getY(), getRight(), getBottom(), 0x88000000);
        context.drawBorder(getX(), getY(), getWidth(), getHeight(), 0xFF808080);

        int columns = getColumns();
        int contentTop = getY() + PADDING - (int) getScrollY();
        ItemSelectorScreen.ItemEntry hoveredEntry = null;

        context.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
        for (int index = 0; index < items.size(); index++) {
            int cellX = getX() + PADDING + (index % columns) * CELL_SIZE;
            int cellY = contentTop + (index / columns) * CELL_SIZE;
            if (cellY + CELL_SIZE < getY() || cellY > getBottom()) {
                continue;
            }

            ItemSelectorScreen.ItemEntry entry = items.get(index);
            boolean selected = entry.id().equals(selectedItemId);
            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL_SIZE
                    && mouseY >= cellY && mouseY < cellY + CELL_SIZE
                    && mouseY >= getY() && mouseY < getBottom();
            if (selected || hovered) {
                context.fill(
                        cellX,
                        cellY,
                        cellX + CELL_SIZE - 2,
                        cellY + CELL_SIZE - 2,
                        selected ? 0xAA55AAFF : 0x66FFFFFF
                );
            }
            context.drawItem(entry.stack(), cellX + 4, cellY + 4);
            if (hovered) {
                hoveredEntry = entry;
            }
        }
        context.disableScissor();
        drawScrollbar(context);

        if (hoveredEntry != null) {
            context.drawTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    List.of(hoveredEntry.stack().getName(), Text.literal(hoveredEntry.id().toString())),
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

        if (checkScrollbarDragged(mouseX, mouseY, button)) {
            return true;
        }

        int relativeX = (int) mouseX - getX() - PADDING;
        int relativeY = (int) (mouseY - getY() - PADDING + getScrollY());
        if (relativeX < 0 || relativeY < 0) {
            return false;
        }

        int columns = getColumns();
        int column = relativeX / CELL_SIZE;
        int row = relativeY / CELL_SIZE;
        if (column >= columns
                || relativeX % CELL_SIZE >= CELL_SIZE - 2
                || relativeY % CELL_SIZE >= CELL_SIZE - 2) {
            return false;
        }

        int index = row * columns + column;
        if (index >= items.size()) {
            return false;
        }

        playClickSound(MinecraftClient.getInstance().getSoundManager());
        onSelected.accept(items.get(index).id());
        return true;
    }

    private int getColumns() {
        return Math.max(1, (getWidth() - PADDING * 2 - SCROLLBAR_WIDTH) / CELL_SIZE);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        builder.put(
                NarrationPart.TITLE,
                Text.translatable("screen.compass_enhanced.selector.grid_narration", items.size())
        );
    }
}
