package com.macepvp.ui;

import com.macepvp.config.HotbarPreset;
import com.macepvp.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Editor for a single preset.
 *   "Capture slot N"   → captures whatever is in that hotbar slot of your live inventory.
 *   "Clear slot N"     → marks the slot as "don't care" in the preset.
 *   "Capture all"      → snapshots the whole live hotbar at once.
 *   "Select: N"        → cycles which slot auto-highlights after the preset is applied.
 *
 * This version uses only ButtonWidgets (no custom mouse handling), which keeps
 * it portable across Minecraft versions even as the internal Screen input API
 * changes shape.
 */
public class PresetEditScreen extends Screen {
    private final Screen parent;
    private final HotbarPreset preset;
    private TextFieldWidget nameField;

    private static final int SLOT_SIZE = 22;
    private static final int SLOT_GAP = 4;

    public PresetEditScreen(Screen parent, int presetIndex) {
        super(Text.literal("Edit preset"));
        this.parent = parent;
        this.preset = ModConfig.get().presets.get(presetIndex);
    }

    @Override
    protected void init() {
        super.init();

        nameField = new TextFieldWidget(this.textRenderer,
                this.width / 2 - 100, 40, 200, 20, Text.literal("Name"));
        nameField.setText(preset.name);
        nameField.setChangedListener(s -> preset.name = s);
        this.addDrawableChild(nameField);

        // Cycle which slot is auto-selected on apply (-1 = none, 0..8 = slots 1..9).
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(selectLabel()),
                b -> {
                    preset.selectSlotOnApply = (preset.selectSlotOnApply + 2) % 10 - 1;
                    b.setMessage(Text.literal(selectLabel()));
                }
        ).dimensions(this.width / 2 - 100, 70, 200, 20).build());

        // "Capture" and "Clear" buttons for each of the 9 slots.
        int slotsY = 110;
        int startX = this.width / 2 - (9 * 50 + 8 * 4) / 2;
        for (int i = 0; i < 9; i++) {
            final int slot = i;
            int x = startX + i * (50 + 4);

            // Capture-single-slot button (label shows current configured item).
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(slotButtonLabel(slot)),
                    b -> {
                        PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
                        ItemStack s = inv.getStack(slot);
                        preset.setItem(slot, s.isEmpty() ? Items.AIR : s.getItem());
                        b.setMessage(Text.literal(slotButtonLabel(slot)));
                    }
            ).dimensions(x, slotsY, 50, 20).build());

            // Clear button directly below.
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Clear"),
                    b -> {
                        preset.slots[slot] = "";
                        // Rebuild UI so the capture button label refreshes.
                        this.clearAndInit();
                    }
            ).dimensions(x, slotsY + 22, 50, 20).build());
        }

        // Capture whole hotbar at once.
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Capture all from current hotbar"),
                b -> {
                    PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
                    for (int i = 0; i < 9; i++) {
                        ItemStack s = inv.getStack(i);
                        preset.setItem(i, s.isEmpty() ? Items.AIR : s.getItem());
                    }
                    this.clearAndInit();
                }
        ).dimensions(this.width / 2 - 110, slotsY + 60, 220, 20).build());

        // Save / Cancel.
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save"),
                b -> {
                    ModConfig.get().save();
                    client.setScreen(parent);
                }
        ).dimensions(this.width / 2 - 100, this.height - 30, 95, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                b -> client.setScreen(parent)
        ).dimensions(this.width / 2 + 5, this.height - 30, 95, 20).build());
    }

    private String selectLabel() {
        if (preset.selectSlotOnApply < 0) return "Select on apply: none";
        return "Select on apply: slot " + (preset.selectSlotOnApply + 1);
    }

    private String slotButtonLabel(int slot) {
        if (!preset.isSlotDefined(slot)) return "Slot " + (slot + 1);
        // Short item name fallback — just show the registry path segment.
        String id = preset.slots[slot];
        int colon = id.indexOf(':');
        String shortId = colon >= 0 ? id.substring(colon + 1) : id;
        // Truncate so it fits the 50-px button.
        if (shortId.length() > 7) shortId = shortId.substring(0, 7);
        return (slot + 1) + ": " + shortId;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Name:"),
                this.width / 2 - 140, 46, 0xAAAAAA);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Click a numbered button to capture that slot from your live hotbar. Clear = \"don't care\"."),
                this.width / 2, 95, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() { return false; }
}
