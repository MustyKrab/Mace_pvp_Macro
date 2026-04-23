package com.macepvp.keybind;

import com.macepvp.config.HotbarPreset;
import com.macepvp.config.ModConfig;
import com.macepvp.macro.HotbarSwapper;
import com.macepvp.ui.ConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers keybinds for opening the config UI, running the stun slam combo,
 * and applying any of 9 user-defined hotbar presets.
 *
 * Notes for 1.21.11: KeyBinding now takes a {@link KeyBinding.Category} object
 * rather than a category string. Each category must be registered exactly once.
 */
public class KeybindManager {
    // Category must be a singleton — registering twice throws.
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("macepvp", "main"));

    public static KeyBinding openConfig;
    public static KeyBinding stunSlam;
    public static KeyBinding breachSwap;
    public static final List<KeyBinding> presetBindings = new ArrayList<>();

    public static void register() {
        openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.macepvp.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET, // default: ]
                CATEGORY));

        stunSlam = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.macepvp.stun_slam",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(), // unbound by default
                CATEGORY));

        breachSwap = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.macepvp.breach_swap",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(), // unbound by default
                CATEGORY));

        for (int i = 0; i < 9; i++) {
            KeyBinding kb = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.macepvp.apply_preset_" + (i + 1),
                    InputUtil.Type.KEYSYM,
                    InputUtil.UNKNOWN_KEY.getCode(),
                    CATEGORY));
            presetBindings.add(kb);
        }

        ClientTickEvents.END_CLIENT_TICK.register(KeybindManager::onTick);
    }

    private static void onTick(MinecraftClient client) {
        if (client.player == null) return;

        while (openConfig.wasPressed()) {
            client.setScreen(new ConfigScreen(client.currentScreen));
        }

        while (stunSlam.wasPressed()) {
            com.macepvp.macro.StunSlamCombo.start();
        }

        while (breachSwap.wasPressed()) {
            com.macepvp.macro.BreachSwapCombo.execute();
        }

        List<HotbarPreset> presets = ModConfig.get().presets;
        for (int i = 0; i < presetBindings.size(); i++) {
            KeyBinding kb = presetBindings.get(i);
            while (kb.wasPressed()) {
                if (i < presets.size()) {
                    HotbarSwapper.apply(presets.get(i));
                }
            }
        }
    }
}
