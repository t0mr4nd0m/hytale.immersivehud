package com.tom.immersivehudplugin.runtime.signal;

import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

public final class CombatSignalTracker {

    private final CombatSignalScanner combatSignalScanner = new CombatSignalScanner();

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

        var result = combatSignalScanner.scanNpcCombat(
                tickContext.store(),
                tickContext.ref(),
                state.combatTargetRef,
                combatScanRange(global)
        );

        int combatHideDelay = Math.max(hideDelay, combatScanIntervalMs(global) * 2);

        if (result.active()) {
            state.combatTargetRef = result.npcRef();
            state.lastCombatTargetSeenMs = now;
            state.t.pulse(HudTrigger.IN_COMBAT, now, combatHideDelay);
            return;
        }

        if (state.combatTargetRef != null && now - state.lastCombatTargetSeenMs > combatHideDelay) {
            state.combatTargetRef = null;
            state.lastCombatTargetSeenMs = 0L;
        }
    }
}
