package dev.christine.compassenhanced.client.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ItemSelectorScreen extends Screen {
    private static final int PANEL_WIDTH = 320;

    private final Screen parent;
    private final Identifier selectedItemId;
    private final Consumer<Identifier> onSelected;
    private final List<ItemEntry> allItems;

    private TextFieldWidget searchField;
    private ItemGridWidget itemGrid;

    public ItemSelectorScreen(Screen parent, Identifier selectedItemId, Consumer<Identifier> onSelected) {
        super(Text.translatable("screen.compass_enhanced.selector.title"));
        this.parent = parent;
        this.selectedItemId = selectedItemId;
        this.onSelected = onSelected;
        this.allItems = collectItems();
    }

    private static List<ItemEntry> collectItems() {
        List<ItemEntry> entries = new ArrayList<>();
        for (Item item : Registries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            Identifier id = Registries.ITEM.getId(item);
            ItemStack stack = new ItemStack(item);
            entries.add(new ItemEntry(
                    id,
                    stack,
                    id.toString().toLowerCase(Locale.ROOT),
                    stack.getName().getString().toLowerCase(Locale.ROOT)
            ));
        }
        entries.sort(Comparator.comparing(entry -> entry.id().toString()));
        return List.copyOf(entries);
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = Math.max(8, (height - 224) / 2);
        int backButtonY = Math.min(height - 28, top + 202);

        searchField = new TextFieldWidget(
                textRenderer,
                left,
                top + 20,
                PANEL_WIDTH,
                20,
                Text.translatable("screen.compass_enhanced.selector.search")
        );
        searchField.setPlaceholder(Text.translatable("screen.compass_enhanced.selector.search_hint"));
        searchField.setMaxLength(128);
        searchField.setChangedListener(this::filterItems);
        addDrawableChild(searchField);

        itemGrid = new ItemGridWidget(
                left,
                top + 47,
                PANEL_WIDTH,
                Math.max(72, backButtonY - top - 55),
                allItems,
                selectedItemId,
                this::chooseItem
        );
        addDrawableChild(itemGrid);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), button -> close())
                .dimensions(left, backButtonY, PANEL_WIDTH, 20)
                .build());
        setInitialFocus(searchField);
    }

    private void filterItems(String query) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            itemGrid.setItems(allItems);
            return;
        }

        itemGrid.setItems(allItems.stream()
                .filter(entry -> entry.idSearchText().contains(normalized)
                        || entry.nameSearchText().contains(normalized))
                .toList());
    }

    private void chooseItem(Identifier itemId) {
        onSelected.accept(itemId);
        close();
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int top = Math.max(8, (height - 224) / 2);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, top, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(
                textRenderer,
                Text.translatable("screen.compass_enhanced.selector.results", itemGrid.getItemCount()),
                (width + PANEL_WIDTH) / 2 - 90,
                top + 6,
                0xA0A0A0
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    record ItemEntry(
            Identifier id,
            ItemStack stack,
            String idSearchText,
            String nameSearchText
    ) {
    }
}
