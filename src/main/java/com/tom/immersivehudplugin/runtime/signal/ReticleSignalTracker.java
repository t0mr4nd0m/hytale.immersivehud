package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

public final class ReticleSignalTracker {

    public void updateReticleSignalsIfNeeded(
            PlayerHudState state,
            World world,
            PlayerTickContext tickContext,
            GlobalConfig global,
            long now,
            int hideDelay
    ) {
        if (!shouldScanReticle(state, global, now)) {
            return;
        }

        float targetRange = reticleTargetRange(global);
        ReticleScanResult scan = scanReticle(world, tickContext, targetRange);

        if (scan.targetEntity()) {
            state.t.pulse(HudTrigger.TARGET_ENTITY, now, hideDelay);
        }

        if (scan.interactableBlock()) {
            state.t.pulse(HudTrigger.INTERACTABLE_BLOCK, now, hideDelay);
        }

        markReticleScanExecuted(state, now);
    }

    private boolean shouldScanReticle(
            PlayerHudState state,
            GlobalConfig global,
            long now
    ) {
        return (now - state.lastReticleScanMs) >= reticleScanIntervalMs(global);
    }

    private int reticleScanIntervalMs(GlobalConfig global) {
        return Math.max(100, intervalMs(global));
    }

    private int intervalMs(GlobalConfig cfg) {
        return cfg != null ? cfg.getIntervalMs() : GlobalConfig.INTERVAL_MS;
    }

    private float reticleTargetRange(GlobalConfig cfg) {
        return cfg != null ? cfg.getReticleTargetRange() : GlobalConfig.RETICLE_TARGET_RANGE;
    }

    private void markReticleScanExecuted(PlayerHudState state, long now) {
        state.lastReticleScanMs = now;
    }

    private ReticleScanResult scanReticle(
            World world,
            PlayerTickContext tickContext,
            float targetRange
    ) {
        Ref<EntityStore> target = TargetUtil.getTargetEntity(
                tickContext.ref(),
                targetRange,
                tickContext.store()
        );
        boolean hasEntityTarget = target != null && !target.equals(tickContext.ref());

        boolean lookingAtInteractable = false;
        Vector3i blockPos = TargetUtil.getTargetBlock(
                tickContext.ref(),
                targetRange,
                tickContext.store()
        );

        if (blockPos != null) {
            BlockType blockType = world.getBlockType(blockPos);
            var flags = blockType != null ? blockType.getFlags() : null;
            lookingAtInteractable = flags != null && flags.isUsable;
        }

        return new ReticleScanResult(hasEntityTarget, lookingAtInteractable);
    }

    private record ReticleScanResult(
            boolean targetEntity,
            boolean interactableBlock
    ) {}
}