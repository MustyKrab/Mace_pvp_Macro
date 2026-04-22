package com.macepvp.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Single-instance config holder. Persists presets to
 *   <minecraft>/config/macepvp.json
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;

    public List<HotbarPreset> presets = new ArrayList<>();

    // Delay (in ticks) between individual swap actions. 1 tick ~= 50ms.
    // Zero works on singleplayer; servers with anti-cheat may need 1–2.
    public int swapDelayTicks = 0;

    // Safety toggle — disables all macro execution without wiping your presets.
    public boolean enabled = true;

    // ===== Stun-slam combo =====
    // Hotbar slots (0-indexed) for the 3-hit combo. Defaults: slot 1/2/3 = axe/spear/mace.
    // Arrange these items in your hotbar yourself; the combo does NOT rearrange the hotbar,
    // it just selects + attacks three times in a single tick.
    public int stunSlamSlot1 = 0;
    public int stunSlamSlot2 = 1;
    public int stunSlamSlot3 = 2;

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("macepvp.json");
    }

    private static ModConfig load() {
        Path p = path();
        if (Files.exists(p)) {
            try {
                String json = Files.readString(p);
                ModConfig cfg = GSON.fromJson(json, ModConfig.class);
                if (cfg == null) cfg = defaults();
                if (cfg.presets == null) cfg.presets = new ArrayList<>();
                return cfg;
            } catch (IOException e) {
                System.err.println("[MacePvP] Failed to read config: " + e.getMessage());
            }
        }
        ModConfig cfg = defaults();
        cfg.save();
        return cfg;
    }

    private static ModConfig defaults() {
        ModConfig cfg = new ModConfig();
        cfg.presets.add(HotbarPreset.defaultSmashLoadout());
        cfg.presets.add(HotbarPreset.defaultBreachLoadout());
        cfg.presets.add(HotbarPreset.defaultRecoveryLoadout());
        return cfg;
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("[MacePvP] Failed to write config: " + e.getMessage());
        }
    }
}
