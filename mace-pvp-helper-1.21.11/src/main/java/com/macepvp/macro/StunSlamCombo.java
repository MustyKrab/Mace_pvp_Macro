package com.macepvp.macro;

import com.macepvp.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

/**
 * Executes a 3-hit stun slam combo in a SINGLE game tick:
 *
 *   select slot1 → attack → select slot2 → attack → select slot3 → attack
 *
 * All six packets (three slot updates + three attacks) are queued into the same
 * tick. The server processes all incoming packets from a client before advancing
 * invulnerability frames for that tick, which is why two+ hits can land in one
 * 50ms window — the second and third hits arrive BEFORE i-frames apply.
 *
 * Mechanics that make this work:
 *   - Swapping weapons resets the attack-strength cooldown on the new weapon to
 *     full instantly, so every hit in the combo is a fully-charged swing.
 *   - {@link UpdateSelectedSlotC2SPacket} tells the server the new active slot
 *     BEFORE the corresponding attack packet, so each hit is attributed to the
 *     correct weapon server-side.
 *   - {@link MinecraftClient#doAttack()} is the same method vanilla calls on a
 *     left-click. It respects reach and line-of-sight — you still need to be
 *     aimed at the target. No aimbot, no reach extension.
 *
 * The hotbar is NOT rearranged. You arrange axe / spear / mace into the three
 * configured slots yourself; the combo just selects each in turn.
 */
public final class StunSlamCombo {

    private StunSlamCombo() {}

    public static void execute() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.getNetworkHandler() == null) return;
        if (!ModConfig.get().enabled) return;

        ClientPlayerEntity player = mc.player;
        ModConfig cfg = ModConfig.get();

        hitWithSlot(mc, player, cfg.stunSlamSlot1);
        hitWithSlot(mc, player, cfg.stunSlamSlot2);
        hitWithSlot(mc, player, cfg.stunSlamSlot3);
    }

    private static void hitWithSlot(MinecraftClient mc, ClientPlayerEntity player, int slot) {
        if (slot < 0 || slot >= 9) return;

        // 1. Change client-side selected slot (updates UI + local state).
        player.getInventory().selectedSlot = slot;

        // 2. Tell the server we changed slots, so it attributes the next attack
        //    to the correct item. Sent explicitly because we're driving this
        //    from code, not from the normal input-handler path that would
        //    queue this packet for us.
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        // 3. Fire the attack. Same code path as a manual left-click.
        mc.doAttack();
    }
}
