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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Registers one keybind to open the config UI and up to 9 preset-apply keybinds.
 * Default bindings are UNBOUND — the user chooses them in vanilla's Controls menu.
 */
public class KeybindManager {
    private static final String CATEGORY = "key.categories.macepvp";

    public static KeyBinding openConfig;
    public static KeyBinding stunSlam;
    public static final List<KeyBinding> presetBindings = new ArrayList<>();

    public static void register() {
        openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.macepvp.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET, // default: ]
                CATEGORY));

        // The main event — one keypress, three hits in one tick.
        stunSlam = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.macepvp.stun_slam",
                InputUtil.Type.KEYSYM,
                InputUtil.UNKNOWN_KEY.getCode(), // unbound by default — user picks
                CATEGORY));

        // 9 configurable "apply preset N" bindings, unbound by default.
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
            com.macepvp.macro.StunSlamCombo.execute();
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
