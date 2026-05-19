package com.tom.immersivehudplugin.runtime.context;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.tom.immersivehudplugin.hud.trigger.HudBarState;
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

        updateBar(state.healthBar, stats, healthState);
        updateBar(state.staminaBar, stats, staminaState);
        updateBar(state.manaBar, stats, manaState);
        updateBar(state.oxygenBar, stats, oxygenState);
    }

    private void updateBar(HudBarState barState, EntityStatMap stats, int statIndex) {
        var component = stats == null ? null : stats.get(statIndex);

        if (component == null) {
            barState.update(1f, 1f);
            return;
        }

        barState.update(component.get(), component.getMax());
    }
}