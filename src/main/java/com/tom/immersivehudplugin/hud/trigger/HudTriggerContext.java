package com.tom.immersivehudplugin.hud.trigger;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class HudTriggerContext {

    private final EnumSet<HudTrigger> activeSignals;
    private final HudBarState healthBar;
    private final HudBarState staminaBar;
    private final HudBarState manaBar;
    private final HudBarState oxygenBar;

    public HudTriggerContext(
            EnumSet<HudTrigger> activeSignals,
            HudBarState healthBar,
            HudBarState staminaBar,
            HudBarState manaBar,
            HudBarState oxygenBar
    ) {
        this.activeSignals = activeSignals == null || activeSignals.isEmpty()
                ? EnumSet.noneOf(HudTrigger.class)
                : EnumSet.copyOf(activeSignals);

        this.healthBar = healthBar != null ? healthBar : new HudBarState();
        this.staminaBar = staminaBar != null ? staminaBar : new HudBarState();
        this.manaBar = manaBar != null ? manaBar : new HudBarState();
        this.oxygenBar = oxygenBar != null ? oxygenBar : new HudBarState();
    }

    public boolean active(HudTrigger trigger) {
        return trigger != null && activeSignals.contains(trigger);
    }

    public Set<HudTrigger> activeSignals() {
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