package com.macepvp.config;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Arrays;

/**
 * A HotbarPreset defines which item should appear in each of the 9 hotbar slots.
 * Slots are stored as item registry IDs (e.g. "minecraft:mace") so they survive saves.
 * A null/empty slot means "don't care — leave whatever is there alone."
 */
public class HotbarPreset {
    public String name = "Untitled";
    // 9 hotbar slots, 0 = leftmost
    public String[] slots = new String[9];
    // Which slot should be auto-selected after applying this preset.
    // -1 means "don't change selected slot."
    public int selectSlotOnApply = -1;

    public HotbarPreset() {
        Arrays.fill(slots, "");
    }

    public HotbarPreset(String name) {
        this();
        this.name = name;
    }

    public Item getItem(int slot) {
        if (slot < 0 || slot >= 9) return Items.AIR;
        String id = slots[slot];
        if (id == null || id.isEmpty()) return Items.AIR;
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) return Items.AIR;
        return Registries.ITEM.get(identifier);
    }

    public void setItem(int slot, Item item) {
        if (slot < 0 || slot >= 9) return;
        if (item == null || item == Items.AIR) {
            slots[slot] = "";
        } else {
            slots[slot] = Registries.ITEM.getId(item).toString();
        }
    }

    public boolean isSlotDefined(int slot) {
        if (slot < 0 || slot >= 9) return false;
        return slots[slot] != null && !slots[slot].isEmpty();
    }

    public HotbarPreset copy() {
        HotbarPreset p = new HotbarPreset(this.name);
        System.arraycopy(this.slots, 0, p.slots, 0, 9);
        p.selectSlotOnApply = this.selectSlotOnApply;
        return p;
    }

    // ---- Sensible starter presets for mace PvP ----

    public static HotbarPreset defaultSmashLoadout() {
        HotbarPreset p = new HotbarPreset("Smash");
        p.setItem(0, Items.MACE);
        p.setItem(1, Items.WIND_CHARGE);
        p.setItem(2, Items.ENDER_PEARL);
        p.selectSlotOnApply = 0;
        return p;
    }

    public static HotbarPreset defaultBreachLoadout() {
        HotbarPreset p = new HotbarPreset("Breach");
        // Put a second mace (intended to be Breach-enchanted) on slot 0 for quick swap vs armored targets.
        p.setItem(0, Items.MACE);
        p.setItem(1, Items.TOTEM_OF_UNDYING);
        p.selectSlotOnApply = 0;
        return p;
    }

    public static HotbarPreset defaultRecoveryLoadout() {
        HotbarPreset p = new HotbarPreset("Recovery");
        p.setItem(0, Items.GOLDEN_APPLE);
        p.setItem(1, Items.TOTEM_OF_UNDYING);
        p.selectSlotOnApply = 1;
        return p;
    }
}
