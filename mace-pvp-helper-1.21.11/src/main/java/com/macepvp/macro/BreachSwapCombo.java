package com.macepvp.macro;

import com.macepvp.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Breach swap — MC-28289 attribute-swap exploit.
 *
 * Mechanic: if the player attacks with weapon A and swaps to weapon B within
 * the same tick, the server uses weapon A's base damage and attack cooldown
 * but applies weapon B's enchantments (and ability to disable a shield).
 * Swapping to a Breach-enchanted mace after an axe/sword attack gives that
 * attack the mace's Breach armor-penetration while keeping the fast weapon's
 * cooldown and the original weapon's damage scaling.
 *
 * Sequence on one keypress, all in one tick:
 *   1. attackEntity(target) with currently-held weapon
 *   2. swing hand (animation + swing packet)
 *   3. find the Breach-enchanted mace (hotbar first, then backpack)
 *   4. update selected slot + send slot-update packet so the "swap" lands
 *      server-side in the same tick as the attack
 *
 * If the Breach mace is in the backpack, we first relocate it to a fixed
 * hotbar slot via SlotActionType.SWAP before selecting it.
 */
public final class BreachSwapCombo {

    /** Breach enchantment registry id. */
    private static final Identifier BREACH_ID = Identifier.of("minecraft", "breach");

    private BreachSwapCombo() {}

    public static void execute() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null
                || mc.getNetworkHandler() == null) return;
        if (!ModConfig.get().enabled) return;

        ClientPlayerEntity player = mc.player;
        PlayerInventory inv = player.getInventory();
        ModConfig cfg = ModConfig.get();

        // --- Step 1: attack with the currently-held weapon ---
        Entity target = resolveTarget(mc);
        if (target != null) {
            mc.interactionManager.attackEntity(player, target);
        }
        player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        // --- Step 2: locate the Breach mace ---
        int breachSlot = findBreachInHotbar(inv);

        if (breachSlot < 0) {
            // Not in hotbar — check backpack and relocate to the configured fixed slot.
            int backpackIndex = findBreachInBackpack(inv);
            if (backpackIndex < 0) return; // no Breach mace anywhere, nothing to do

            int targetSlot = Math.max(0, Math.min(8, cfg.breachSwapFixedSlot));
            int syncId = player.playerScreenHandler.syncId;
            // SWAP: moves the backpack stack into the target hotbar slot, and
            // whatever was in that slot gets bounced to the backpack slot.
            mc.interactionManager.clickSlot(syncId, backpackIndex, targetSlot,
                    SlotActionType.SWAP, player);
            breachSlot = targetSlot;
        }

        // --- Step 3: select the Breach mace (client + server) ---
        inv.setSelectedSlot(breachSlot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(breachSlot));
    }

    private static Entity resolveTarget(MinecraftClient mc) {
        HitResult hr = mc.crosshairTarget;
        if (hr instanceof EntityHitResult ehr) return ehr.getEntity();
        return null;
    }

    /** @return hotbar index 0..8 holding a Breach-enchanted mace, or -1. */
    private static int findBreachInHotbar(PlayerInventory inv) {
        for (int i = 0; i < 9; i++) {
            if (isBreachMace(inv.getStack(i))) return i;
        }
        return -1;
    }

    /**
     * @return PlayerInventory index 9..35 (backpack) holding a Breach mace,
     *         or -1 if none. This is the raw inventory index, which happens
     *         to match the screen-handler slot id for backpack slots.
     */
    private static int findBreachInBackpack(PlayerInventory inv) {
        for (int i = 9; i < 36; i++) {
            if (isBreachMace(inv.getStack(i))) return i;
        }
        return -1;
    }

    private static boolean isBreachMace(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() != Items.MACE) return false;

        ItemEnchantmentsComponent enchants =
                stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchants == null) return false;

        for (RegistryEntry<Enchantment> entry : enchants.getEnchantments()) {
            // entry.getKey() returns Optional<RegistryKey<Enchantment>>;
            // compare its value (the Identifier) to Breach's id.
            if (entry.getKey().isPresent()
                    && entry.getKey().get().getValue().equals(BREACH_ID)) {
                return true;
            }
        }
        return false;
    }
}
