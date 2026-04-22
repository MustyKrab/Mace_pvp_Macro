package com.macepvp;

import com.macepvp.config.ModConfig;
import com.macepvp.keybind.KeybindManager;
import net.fabricmc.api.ClientModInitializer;

public class MacePvpClient implements ClientModInitializer {
    public static final String MOD_ID = "macepvp";

    @Override
    public void onInitializeClient() {
        // Force-load config so we fail fast on bad JSON and so defaults get written.
        ModConfig.get();
        KeybindManager.register();
        System.out.println("[MacePvP] Initialized.");
    }
}
