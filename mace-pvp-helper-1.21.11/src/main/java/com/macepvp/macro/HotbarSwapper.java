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
 * without opening the inventory GUI.
 */
public class HotbarSwapper {

    private HotbarSwapper() {}

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

            if (current.getItem() == wanted) continue;

            int sourceIndex = findItem(inv, wanted, hotbarSlot);
            if (sourceIndex < 0) continue;

            int screenSlotId = toScreenSlot(sourceIndex);

            mc.interactionManager.clickSlot(syncId, screenSlotId, hotbarSlot,
                    SlotActionType.SWAP, player);
            anyChange = true;
        }

        if (preset.selectSlotOnApply >= 0 && preset.selectSlotOnApply < 9) {
            // 1.21.11: selectedSlot field is private; use the setter.
            player.getInventory().setSelectedSlot(preset.selectSlotOnApply);
        }
        return anyChange;
    }

    private static int findItem(PlayerInventory inv, Item wanted, int ignoringHotbar) {
        for (int i = 9; i < 36; i++) {
            if (inv.getStack(i).getItem() == wanted) return i;
        }
        for (int i = 0; i < 9; i++) {
            if (i == ignoringHotbar) continue;
            if (inv.getStack(i).getItem() == wanted) return i;
        }
        return -1;
    }

    private static int toScreenSlot(int playerInvIndex) {
        if (playerInvIndex < 9) return playerInvIndex + 36;
        return playerInvIndex;
    }
}
