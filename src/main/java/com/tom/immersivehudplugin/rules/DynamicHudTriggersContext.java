package com.tom.immersivehudplugin.rules;

import com.tom.immersivehudplugin.utils.HudBarState;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class DynamicHudTriggersContext {

    private final EnumSet<DynamicHudTriggers> activeSignals;
    private final HudBarState healthBar;
    private final HudBarState staminaBar;
    private final HudBarState manaBar;
    private final HudBarState oxygenBar;

    public DynamicHudTriggersContext(
            EnumSet<DynamicHudTriggers> activeSignals,
            HudBarState healthBar,
            HudBarState staminaBar,
            HudBarState manaBar,
            HudBarState oxygenBar
    ) {
        this.activeSignals = activeSignals == null || activeSignals.isEmpty()
                ? EnumSet.noneOf(DynamicHudTriggers.class)
                : EnumSet.copyOf(activeSignals);

        this.healthBar = healthBar != null ? healthBar : new HudBarState();
        this.staminaBar = staminaBar != null ? staminaBar : new HudBarState();
        this.manaBar = manaBar != null ? manaBar : new HudBarState();
        this.oxygenBar = oxygenBar != null ? oxygenBar : new HudBarState();
    }

    public boolean active(DynamicHudTriggers trigger) {
        return trigger != null && activeSignals.contains(trigger);
    }

    public Set<DynamicHudTriggers> activeSignals() {
        return Collections.unmodifiableSet(activeSignals);
    }

    public HudBarState healthBar() {
        return healthBar;
    }

    public HudBarState staminaBar() {
        return staminaBar;
    }

    public HudBarState manaBar() {
        return manaBar;
    }

    public HudBarState oxygenBar() {
        return oxygenBar;
    }
}