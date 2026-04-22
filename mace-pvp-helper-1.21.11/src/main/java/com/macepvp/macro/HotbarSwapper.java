package com.macepvp.macro;

import com.macepvp.config.HotbarPreset;
import com.macepvp.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Rearranges the player's inventory so the hotbar matches a target preset.
 *
 * Minecraft inventory layout (indices in PlayerInventory.main):
 *   0-8   = hotbar (slot 0 is leftmost)
 *   9-35  = main inventory
 *
 * Screen-handler slot IDs for the survival inventory:
 *   9-35  = main inventory (rows 1-3 of the backpack)
 *   36-44 = hotbar
 *
 * We use SlotActionType.SWAP with the hotbar button index to relocate items
 * without opening the inventory GUI. The server treats this like the player
 * pressing a hotbar number while an item is hovered in their inventory screen,
 * which vanilla allows from the player's own screen handler at any time.
 */
public class HotbarSwapper {

    private HotbarSwapper() {}

    /** Apply the preset to the current player. Returns true if any swap happened. */
    public static boolean apply(HotbarPreset preset) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null) return false;
        if (!ModConfig.get().enabled) return false;

        ClientPlayerEntity player = mc.player;
        PlayerInventory inv = player.getInventory();
        int syncId = player.playerScreenHandler.syncId;
        boolean anyChange = false;

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            if (!preset.isSlotDefined(hotbarSlot)) continue;

            Item wanted = preset.getItem(hotbarSlot);
            ItemStack current = inv.getStack(hotbarSlot);

            // Already correct — skip.
            if (current.getItem() == wanted) continue;

            // Find the wanted item somewhere in the inventory (main inventory only,
            // since we're shuffling within what the player already carries).
            int sourceIndex = findItem(inv, wanted, hotbarSlot);
            if (sourceIndex < 0) continue; // not carried — nothing to do

            // Convert PlayerInventory index → screen-handler slot ID.
            int screenSlotId = toScreenSlot(sourceIndex);

            // SWAP action with button = hotbar slot index moves the source stack
            // into that hotbar slot (and whatever was there into the source slot).
            mc.interactionManager.clickSlot(syncId, screenSlotId, hotbarSlot,
                    SlotActionType.SWAP, player);
            anyChange = true;
        }

        if (preset.selectSlotOnApply >= 0 && preset.selectSlotOnApply < 9) {
            // Direct field write avoids yarn method renames between 1.21.x releases.
            // (In some versions this is setSelectedSlot(int); the field itself is stable.)
            player.getInventory().selectedSlot = preset.selectSlotOnApply;
        }
        return anyChange;
    }

    /**
     * Search the whole inventory for the requested item, preferring stacks
     * that aren't already sitting in another "defined" hotbar slot of the
     * active preset (so we don't cannibalize other configured slots).
     */
    private static int findItem(PlayerInventory inv, Item wanted, int ignoringHotbar) {
        // First pass: main inventory (9-35) — safest, not a configured hotbar slot.
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).getItem() == wanted) return i;
        }
        // Second pass: other hotbar slots, skipping the one we're filling.
        for (int i = 0; i < 9; i++) {
            if (i == ignoringHotbar) continue;
            if (inv.getStack(i).getItem() == wanted) return i;
        }
        return -1;
    }

    /**
     * PlayerInventory indices map to screen-handler slot IDs like this in the
     * survival inventory screen:
     *   PlayerInventory  0..8   (hotbar)   →  screen slot 36..44
     *   PlayerInventory  9..35  (backpack) →  screen slot 9..35
     */
    private static int toScreenSlot(int playerInvIndex) {
        if (playerInvIndex < 9) return playerInvIndex + 36;
        return playerInvIndex; // backpack indices are 1:1
    }
}
