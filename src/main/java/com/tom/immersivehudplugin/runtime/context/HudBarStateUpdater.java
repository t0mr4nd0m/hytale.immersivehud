package com.tom.immersivehudplugin.runtime.context;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

public final class HudBarStateUpdater {

    private final int healthState;
    private final int staminaState;
    private final int manaState;
    private final int oxygenState;

    public HudBarStateUpdater(
            int healthState,
            int staminaState,
            int manaState,
            int oxygenState
    ) {
        this.healthState = healthState;
        this.staminaState = staminaState;
        this.manaState = manaState;
        this.oxygenState = oxygenState;
    }

    public void update(PlayerHudState state, PlayerTickContext tickContext) {
        EntityStatMap stats = tickContext.stats();

        state.healthBar.update(getCurrentBar(stats, healthState), getMaxBar(stats, healthState));
        state.staminaBar.update(getCurrentBar(stats, staminaState), getMaxBar(stats, staminaState));
        state.manaBar.update(getCurrentBar(stats, manaState), getMaxBar(stats, manaState));
        state.oxygenBar.update(getCurrentBar(stats, oxygenState), getMaxBar(stats, oxygenState));
    }

    private float getCurrentBar(EntityStatMap stats, int statIndex) {
        if (stats == null) {
            return 0f;
        }

        var component = stats.get(statIndex);
        return component != null ? component.get() : 0f;
    }

    private float getMaxBar(EntityStatMap stats, int statIndex) {
        if (stats == null) {
            return 0f;
        }

        var component = stats.get(statIndex);
        return component != null ? component.getMax() : 0f;
    }
}