package com.macepvp.ui;

import com.macepvp.config.HotbarPreset;
import com.macepvp.config.ModConfig;
import com.macepvp.keybind.KeybindManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 28;
    private static final int LIST_TOP = 50;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Mace PvP Helper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        rebuildWidgets();
    }

    private void rebuildWidgets() {
        this.clearChildren();

        ModConfig cfg = ModConfig.get();

        // Enable/disable toggle, top-left.
        ButtonWidget enabledBtn = ButtonWidget.builder(
                Text.literal("Mod: " + (cfg.enabled ? "ON" : "OFF")),
                b -> {
                    cfg.enabled = !cfg.enabled;
                    cfg.save();
                    rebuildWidgets();
                }).dimensions(10, 10, 90, 20).build();
        this.addDrawableChild(enabledBtn);

        // Add-preset button, top-right.
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("+ New preset"),
                b -> {
                    cfg.presets.add(new HotbarPreset("New preset"));
                    cfg.save();
                    client.setScreen(new PresetEditScreen(this, cfg.presets.size() - 1));
                }).dimensions(this.width - 110, 10, 100, 20).build());

        // Preset rows.
        int listHeight = this.height - LIST_TOP - 40;
        int visibleRows = Math.max(1, listHeight / ROW_HEIGHT);
        int totalPresets = cfg.presets.size();
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalPresets - visibleRows)));

        for (int row = 0; row < visibleRows && row + scrollOffset < totalPresets; row++) {
            int idx = row + scrollOffset;
            int y = LIST_TOP + row * ROW_HEIGHT;
            HotbarPreset p = cfg.presets.get(idx);

            // Edit button (shows name).
            String label = (idx + 1) + ". " + p.name;
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(label),
                    b -> client.setScreen(new PresetEditScreen(this, idx))
            ).dimensions(20, y, 220, 20).build());

            // Apply-now button.
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Apply"),
                    b -> {
                        com.macepvp.macro.HotbarSwapper.apply(p);
                        client.setScreen(null); // close to see effect
                    }
            ).dimensions(250, y, 50, 20).build());

            // Keybind hint — shows the currently bound key for the matching preset slot.
            String keyLabel = "Key: unbound";
            if (idx < KeybindManager.presetBindings.size()) {
                KeyBinding kb = KeybindManager.presetBindings.get(idx);
                keyLabel = "Key: " + kb.getBoundKeyLocalizedText().getString();
            }
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(keyLabel),
                    b -> {
                        // Jump to vanilla controls menu, category filter is manual.
                        client.setScreen(new net.minecraft.client.gui.screen.option.KeybindsScreen(this, client.options));
                    }
            ).dimensions(310, y, 120, 20).build());

            // Delete button.
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("✕"),
                    b -> {
                        cfg.presets.remove(idx);
                        cfg.save();
                        rebuildWidgets();
                    }
            ).dimensions(440, y, 20, 20).build());
        }

        // Bottom: done + open vanilla keybinds.
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                b -> client.setScreen(parent)
        ).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());

        // ===== Stun-slam slot pickers (bottom bar) =====
        int ssY = this.height - 60;
        int sx = this.width / 2 - 180;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Axe slot: " + (cfg.stunSlamSlot1 + 1)),
                b -> {
                    cfg.stunSlamSlot1 = (cfg.stunSlamSlot1 + 1) % 9;
                    cfg.save();
                    b.setMessage(Text.literal("Axe slot: " + (cfg.stunSlamSlot1 + 1)));
                }
        ).dimensions(sx, ssY, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Spear slot: " + (cfg.stunSlamSlot2 + 1)),
                b -> {
                    cfg.stunSlamSlot2 = (cfg.stunSlamSlot2 + 1) % 9;
                    cfg.save();
                    b.setMessage(Text.literal("Spear slot: " + (cfg.stunSlamSlot2 + 1)));
                }
        ).dimensions(sx + 110, ssY, 110, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Mace slot: " + (cfg.stunSlamSlot3 + 1)),
                b -> {
                    cfg.stunSlamSlot3 = (cfg.stunSlamSlot3 + 1) % 9;
                    cfg.save();
                    b.setMessage(Text.literal("Mace slot: " + (cfg.stunSlamSlot3 + 1)));
                }
        ).dimensions(sx + 230, ssY, 110, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        super.render(ctx, mouseX, mouseY, delta);

        // Title
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        // Helper text under title
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Presets run in the order listed. Bind keys under Options → Controls → \"Mace PvP\"."),
                this.width / 2, 32, 0xAAAAAA);
    }

    // Note: mouseScrolled override removed for 1.21.11 compatibility. The
    // signature changed between versions and our preset list is short enough
    // that buttons suffice without scroll support.

    @Override
    public boolean shouldPause() { return false; }
}
