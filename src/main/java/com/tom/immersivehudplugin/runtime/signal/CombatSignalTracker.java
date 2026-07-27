package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

public final class CombatSignalTracker {

    private final CombatSignalScanner combatSignalScanner;

    public CombatSignalTracker(JavaPlugin plugin) {
        this.combatSignalScanner = new CombatSignalScanner(plugin);
    }

    private int combatScanIntervalMs(GlobalConfig global) {
        return Math.max(100, intervalMs(global));
    }

    private int intervalMs(GlobalConfig cfg) {
        return cfg != null
                ? cfg.getIntervalMs()
                : GlobalConfig.INTERVAL_MS;
    }

    private float combatScanRange(GlobalConfig cfg) {
        return cfg != null
                ? cfg.getCombatScanRange()
                : GlobalConfig.COMBAT_SCAN_RANGE;
    }

    private boolean shouldScanCombat(
            PlayerHudState state,
            GlobalConfig global,
            long now
    ) {
        return (now - state.lastCombatScanMs)
                >= combatScanIntervalMs(global);
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

        int scanInterval = combatScanIntervalMs(global);
        int combatHideDelay =
                Math.max(hideDelay, scanInterval * 2);

        boolean inCombat =
                combatSignalScanner.scanNpcCombat(
                        tickContext.store(),
                        tickContext.ref(),
                        combatScanRange(global),
                        global
                );

        if (inCombat) {
            state.t.pulse(
                    HudTrigger.IN_COMBAT,
                    now,
                    combatHideDelay
            );
        }
    }
}