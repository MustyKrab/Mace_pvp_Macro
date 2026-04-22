package com.macepvp.macro;

import com.macepvp.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Executes a 3-hit stun slam combo in a SINGLE game tick:
 *   select slot1 → attack → select slot2 → attack → select slot3 → attack
 *
 * All packets leave the client in the same tick. The server processes incoming
 * packets from a client before advancing invulnerability frames for the tick,
 * which is why the 2nd and 3rd hits land within one 50 ms window.
 *
 * Mechanics that make this work:
 *   - Swapping weapons resets the attack-strength cooldown on the new weapon,
 *     so every hit is fully charged.
 *   - {@link UpdateSelectedSlotC2SPacket} updates the server's view of which
 *     slot is active BEFORE the attack packet for that hit.
 *   - We attack whatever entity is currently under the crosshair, using the
 *     interaction manager's public attackEntity method. You still need to be
 *     aimed at the target — there's no aim correction.
 *
 * This does NOT rearrange your hotbar. Arrange axe / spear / mace in the three
 * configured slots yourself (defaults: 1/2/3).
 */
public final class StunSlamCombo {

    private StunSlamCombo() {}

    public static void execute() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.interactionManager == null
                || mc.getNetworkHandler() == null) return;
        if (!ModConfig.get().enabled) return;

        ClientPlayerEntity player = mc.player;
        ModConfig cfg = ModConfig.get();

        // Resolve the target once at combo start. If crosshair target is not
        // an entity, we still swap + swing (damage just won't land).
        Entity target = resolveTarget(mc);

        hitWithSlot(mc, player, cfg.stunSlamSlot1, target);
        hitWithSlot(mc, player, cfg.stunSlamSlot2, target);
        hitWithSlot(mc, player, cfg.stunSlamSlot3, target);
    }

    private static Entity resolveTarget(MinecraftClient mc) {
        HitResult hr = mc.crosshairTarget;
        if (hr instanceof EntityHitResult ehr) {
            return ehr.getEntity();
        }
        return null;
    }

    private static void hitWithSlot(MinecraftClient mc, ClientPlayerEntity player,
                                    int slot, Entity target) {
        if (slot < 0 || slot >= 9) return;

        // 1. Update client-side selected slot via the public setter.
        player.getInventory().setSelectedSlot(slot);

        // 2. Inform the server of the slot change so the next attack is
        //    attributed to the correct item.
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));

        // 3. Attack. If we have an entity target, attack it (this is the same
        //    call path MinecraftClient.doAttack() uses internally, but attackEntity
        //    is public while doAttack is private in 1.21.11). Swing the hand
        //    explicitly so the animation plays and the swing packet is sent.
        if (target != null) {
            mc.interactionManager.attackEntity(player, target);
        }
        player.swingHand(Hand.MAIN_HAND);
        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
    }
}
