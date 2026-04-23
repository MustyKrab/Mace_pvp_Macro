package com.macepvp.macro;

import com.macepvp.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import java.util.concurrent.ThreadLocalRandom;

public final class StunSlamCombo {
    private static int step = 0;
    private static int delayTicks = 0;
    private static Entity lockedTarget = null;

    private StunSlamCombo() {}

    // Bind this to your macro key press
    public static void start() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !ModConfig.get().enabled) return;
        if (step != 0) return; // Don't restart if already running

        lockedTarget = resolveTarget(mc);
        step = 1;
        delayTicks = 0; // Fire first hit immediately
    }

    // YOU MUST CALL THIS IN: ClientTickEvents.END_CLIENT_TICK.register(client -> StunSlamCombo.onTick());
    public static void onTick() {
        if (step == 0) return;

        if (delayTicks > 0) {
            delayTicks--;
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        ModConfig cfg = ModConfig.get();

        if (player == null || lockedTarget == null) {
            step = 0; // Abort if target lost or player null
            return;
        }

        // Humanized delay: 1 to 3 ticks (50ms to 150ms) between hits.
        // Adjust these numbers based on how strict the AC is.
        int nextDelay = ThreadLocalRandom.current().nextInt(1, 4);

        switch (step) {
            case 1:
                hitWithSlot(mc, player, cfg.stunSlamSlot1, lockedTarget);
                delayTicks = nextDelay;
                step = 2;
                break;
            case 2:
                hitWithSlot(mc, player, cfg.stunSlamSlot2, lockedTarget);
                delayTicks = nextDelay;
                step = 3;
                break;
            case 3:
                hitWithSlot(mc, player, cfg.stunSlamSlot3, lockedTarget);
                step = 0; // Combo finished
                break;
        }
    }

    private static Entity resolveTarget(MinecraftClient mc) {
        HitResult hr = mc.crosshairTarget;
        if (hr instanceof EntityHitResult ehr) {
            return ehr.getEntity();
        }
        return null;
    }

    private static void hitWithSlot(MinecraftClient mc, ClientPlayerEntity player, int slot, Entity target) {
        if (slot < 0 || slot >= 9) return;
        
        player.getInventory().setSelectedSlot(slot);
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        
        if (target != null) {
            mc.interactionManager.attackEntity(player, target);
        }
        
        // swingHand handles the animation AND sends the HandSwingC2SPacket automatically.
        player.swingHand(Hand.MAIN_HAND);
    }
}
