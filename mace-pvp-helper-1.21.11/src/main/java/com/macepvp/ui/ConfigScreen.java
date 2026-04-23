package com.macepvp.ui;

import com.macepvp.config.HotbarPreset;
import com.macepvp.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Main config screen.
 *
 * Layout is deliberately static — every button is placed once in init() and
 * never rebuilt. Buttons that change state (ON/OFF toggles, slot cyclers)
 * update their own label in-place via {@code b.setMessage(...)} instead of
 * tearing down and re-adding widgets.
 */
public class ConfigScreen extends Screen {
    private final Screen parent;
    private static final int MAX_VISIBLE_PRESETS = 5;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Mace PvP Helper"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        ModConfig cfg = ModConfig.get();

        // ===== Top row: enable toggle + new-preset button =====
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Mod: " + (cfg.enabled ? "ON" : "OFF")),
                b -> {
                    cfg.enabled = !cfg.enabled;
                    cfg.save();
                    b.setMessage(Text.literal("Mod: " + (cfg.enabled ? "ON" : "OFF")));
                }
        ).dimensions(10, 10, 90, 20).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("+ New preset"),
                b -> {
                    cfg.presets.add(new HotbarPreset("New preset"));
                    cfg.save();
                    client.setScreen(new PresetEditScreen(this, cfg.presets.size() - 1));
                }
        ).dimensions(this.width - 110, 10, 100, 20).build());

        // ===== Preset rows (up to 5 shown) =====
        int listTop = 60;
        int rowHeight = 26;
        int shown = Math.min(MAX_VISIBLE_PRESETS, cfg.presets.size());

        for (int i = 0; i < shown; i++) {
            final int idx = i;
            final HotbarPreset p = cfg.presets.get(idx);
            int y = listTop + i * rowHeight;

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Edit: " + (idx + 1) + ". " + p.name),
                    b -> client.setScreen(new PresetEditScreen(this, idx))
            ).dimensions(this.width / 2 - 200, y, 220, 20).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Apply"),
                    b -> {
                        com.macepvp.macro.HotbarSwapper.apply(p);
                        client.setScreen(null);
                    }
            ).dimensions(this.width / 2 + 30, y, 60, 20).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Delete"),
                    b -> {
                        cfg.presets.remove(idx);
                        cfg.save();
                        client.setScreen(new ConfigScreen(parent)); // reopen fresh
                    }
            ).dimensions(this.width / 2 + 100, y, 60, 20).build());
        }

        // ===== Stun slam slot pickers =====
        int ssY = this.height - 60;
        int sx = this.width / 2 - 165;

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

        // Breach-swap fixed slot (where the Breach mace lands if it's in the backpack).
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Breach slot: " + (cfg.breachSwapFixedSlot + 1)),
                b -> {
                    cfg.breachSwapFixedSlot = (cfg.breachSwapFixedSlot + 1) % 9;
                    cfg.save();
                    b.setMessage(Text.literal("Breach slot: " + (cfg.breachSwapFixedSlot + 1)));
                }
        ).dimensions(this.width / 2 - 60, ssY - 25, 120, 20).build());

        // ===== Done button =====
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                b -> client.setScreen(parent)
        ).dimensions(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // NOTE: We intentionally do NOT call this.renderBackground() here.
        // Lunar Client draws its own blur effect when a screen opens and throws
        // "Can only blur once per frame" if we try to blur again. Skipping the
        // explicit background call lets Lunar's existing blur show through and
        // our widgets render on top normally. On vanilla Fabric the screen still
        // gets its dim overlay from the Screen superclass defaults.
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title,
                this.width / 2, 15, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Bind \"Stun slam combo\" under Options → Controls → Mace PvP"),
                this.width / 2, 38, 0xAAAAAA);
    }

    @Override
    public boolean shouldPause() { return false; }
}
