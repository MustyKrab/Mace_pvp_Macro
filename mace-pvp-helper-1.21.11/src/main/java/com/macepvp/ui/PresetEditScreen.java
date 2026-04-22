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
 *   Click a slot      → captures the item currently in that hotbar slot of your live inventory.
 *   Right-click a slot → clears the slot.
 *   Click "Select: N" → cycles which slot auto-selects after the preset is applied.
 */
public class PresetEditScreen extends Screen {
    private final Screen parent;
    private final int presetIndex;
    private final HotbarPreset preset;

    private TextFieldWidget nameField;

    private static final int SLOT_SIZE = 22;
    private static final int SLOT_GAP = 4;

    public PresetEditScreen(Screen parent, int presetIndex) {
        super(Text.literal("Edit preset"));
        this.parent = parent;
        this.presetIndex = presetIndex;
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

        // "Select on apply" cycler.
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(selectLabel()),
                b -> {
                    preset.selectSlotOnApply = (preset.selectSlotOnApply + 2) % 10 - 1; // cycle -1..8
                    b.setMessage(Text.literal(selectLabel()));
                }
        ).dimensions(this.width / 2 - 100, 70, 200, 20).build());

        // Capture-from-hotbar button — grabs the whole live hotbar at once.
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Capture from current hotbar"),
                b -> {
                    PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
                    for (int i = 0; i < 9; i++) {
                        ItemStack s = inv.getStack(i);
                        preset.setItem(i, s.isEmpty() ? Items.AIR : s.getItem());
                    }
                }
        ).dimensions(this.width / 2 - 110, this.height - 60, 220, 20).build());

        // Done / Cancel.
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

    private int slotsStartX() {
        int totalWidth = 9 * SLOT_SIZE + 8 * SLOT_GAP;
        return this.width / 2 - totalWidth / 2;
    }

    private int slotsStartY() {
        return this.height / 2 - 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let buttons/text-field claim clicks first.
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        int sx = slotsStartX();
        int sy = slotsStartY();
        for (int i = 0; i < 9; i++) {
            int x = sx + i * (SLOT_SIZE + SLOT_GAP);
            int y = sy;
            if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                if (button == 0) {
                    // Left-click: capture the item currently in the player's matching hotbar slot.
                    PlayerInventory inv = MinecraftClient.getInstance().player.getInventory();
                    ItemStack s = inv.getStack(i);
                    preset.setItem(i, s.isEmpty() ? Items.AIR : s.getItem());
                } else if (button == 1) {
                    // Right-click: clear the slot (becomes "don't care").
                    preset.slots[i] = "";
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        ctx.drawTextWithShadow(this.textRenderer, Text.literal("Name:"), this.width / 2 - 140, 46, 0xAAAAAA);

        // Instructions.
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Left-click a slot to capture from your live hotbar. Right-click to clear."),
                this.width / 2, slotsStartY() - 20, 0xAAAAAA);

        // Draw the 9 slots + the item each preset slot expects.
        int sx = slotsStartX();
        int sy = slotsStartY();
        for (int i = 0; i < 9; i++) {
            int x = sx + i * (SLOT_SIZE + SLOT_GAP);
            // Slot background
            int bg = preset.isSlotDefined(i) ? 0xFF3A3A3A : 0xFF1F1F1F;
            int border = (preset.selectSlotOnApply == i) ? 0xFFFFAA00 : 0xFF555555;
            ctx.fill(x - 1, sy - 1, x + SLOT_SIZE + 1, sy + SLOT_SIZE + 1, border);
            ctx.fill(x, sy, x + SLOT_SIZE, sy + SLOT_SIZE, bg);

            if (preset.isSlotDefined(i)) {
                ItemStack display = new ItemStack(preset.getItem(i));
                if (!display.isEmpty()) {
                    ctx.drawItem(display, x + 3, sy + 3);
                }
            } else {
                ctx.drawTextWithShadow(this.textRenderer, Text.literal("·"), x + SLOT_SIZE / 2 - 2, sy + SLOT_SIZE / 2 - 4, 0xFF666666);
            }

            // Slot number label.
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal(String.valueOf(i + 1)),
                    x + SLOT_SIZE - 6, sy + SLOT_SIZE + 2, 0xFF888888);
        }
    }

    @Override
    public boolean shouldPause() { return false; }
}
