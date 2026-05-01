package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.server.core.universe.world.World;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

public final class HudSignalPipeline {

    private final HeldItemSignalTracker heldItemSignalTracker;
    private final MovementSignalTracker movementSignalTracker;
    private final ReticleSignalTracker reticleSignalTracker;
    private final CombatSignalTracker combatSignalTracker;

    public HudSignalPipeline(
            HeldItemSignalTracker heldItemSignalTracker,
            MovementSignalTracker movementSignalTracker,
            ReticleSignalTracker reticleSignalTracker,
            CombatSignalTracker combatSignalTracker
    ) {
        this.heldItemSignalTracker = heldItemSignalTracker;
        this.movementSignalTracker = movementSignalTracker;
        this.reticleSignalTracker = reticleSignalTracker;
        this.combatSignalTracker = combatSignalTracker;
    }

    public void update(
            PlayerHudState state,
            World world,
            PlayerTickContext tickContext,
            GlobalConfig global,
            long now
    ) {
        int hideDelay = global != null
                ? global.getHideDelayMs()
                : GlobalConfig.HIDE_DELAY_MS;

        state.hideDelayMs = hideDelay;

        movementSignalTracker.updateMovementSignals(state, tickContext, now, hideDelay);
        reticleSignalTracker.updateReticleSignalsIfNeeded(state, world, tickContext, global, now, hideDelay);
        heldItemSignalTracker.cleanupWeaponSignals(state);
        combatSignalTracker.updateCombatSignalIfNeeded(state, tickContext, global, now, hideDelay);
    }
}