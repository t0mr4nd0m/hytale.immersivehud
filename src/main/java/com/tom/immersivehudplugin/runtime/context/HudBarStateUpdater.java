package com.tom.immersivehudplugin.runtime.context;

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
        state.healthBar.update(getCurrentBar(tickContext, healthState), getMaxBar(tickContext, healthState));
        state.staminaBar.update(getCurrentBar(tickContext, staminaState), getMaxBar(tickContext, staminaState));
        state.manaBar.update(getCurrentBar(tickContext, manaState), getMaxBar(tickContext, manaState));
        state.oxygenBar.update(getCurrentBar(tickContext, oxygenState), getMaxBar(tickContext, oxygenState));
    }

    private float getCurrentBar(PlayerTickContext tickContext, int statIndex) {
        if (tickContext.stats() == null) {
            return 0f;
        }

        var component = tickContext.stats().get(statIndex);
        return component != null ? component.get() : 0f;
    }

    private float getMaxBar(PlayerTickContext tickContext, int statIndex) {
        if (tickContext.stats() == null) {
            return 0f;
        }

        var component = tickContext.stats().get(statIndex);
        return component != null ? component.getMax() : 0f;
    }
}