package com.tom.immersivehudplugin.runtime.signal;

import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

public final class CombatSignalTracker {

    private int combatScanIntervalMs(GlobalConfig global) {
        return Math.max(100, intervalMs(global));
    }

    private int intervalMs(GlobalConfig cfg) {
        return cfg != null ? cfg.getIntervalMs() : GlobalConfig.INTERVAL_MS;
    }

    private float combatScanRange(GlobalConfig cfg) {
        return cfg != null ? cfg.getCombatScanRange() : GlobalConfig.COMBAT_SCAN_RANGE;
    }

    private boolean shouldScanCombat(
            PlayerHudState state,
            GlobalConfig global,
            long now
    ) {
        return (now - state.lastCombatScanMs) >= combatScanIntervalMs(global);
    }

    public void updateCombatSignalIfNeeded(
            PlayerHudState state,
            PlayerTickContext tickContext,
            GlobalConfig global,
            long now,
            int hideDelay
    ) {
        if (!shouldScanCombat(state, global, now)) {
            return;
        }

        state.lastCombatScanMs = now;

        if (hasNpcTargetingPlayer(tickContext, global)) {
            state.t.pulse(HudTrigger.IN_COMBAT, now, hideDelay);
        }
    }

    private final CombatSignalScanner combatSignalScanner = new CombatSignalScanner();

    private boolean hasNpcTargetingPlayer(
            PlayerTickContext tickContext,
            GlobalConfig global
    ) {
        float range = combatScanRange(global);

        return combatSignalScanner.hasNpcTargetingPlayer(
                tickContext.store(),
                tickContext.ref(),
                range
        );
    }
}
