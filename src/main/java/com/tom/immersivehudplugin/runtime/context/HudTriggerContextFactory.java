package com.tom.immersivehudplugin.runtime.context;

import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

import java.util.EnumSet;

public final class HudTriggerContextFactory {

    public HudTriggerContext create(PlayerHudState state, long now) {

        EnumSet<HudTrigger> activeSignals = EnumSet.noneOf(HudTrigger.class);

        // --- SIGNAL triggers
        for (HudTrigger trigger : HudTrigger.values()) {
            if (trigger.source() != HudTrigger.Source.SIGNAL) { continue; }
            if (state.t.active(trigger, now)) { activeSignals.add(trigger); }
        }

        addWeaponSignals(activeSignals, state, now);

        // --- STATUS triggers
        return new HudTriggerContext(
                activeSignals,
                state.healthBar,
                state.staminaBar,
                state.manaBar,
                state.oxygenBar
        );
    }

    private void addWeaponSignals(
            EnumSet<HudTrigger> activeSignals,
            PlayerHudState state,
            long now
    ) {
        if (state.heldItem.hasAnyWeaponInHand() && state.t.active(HudTrigger.CHARGING_WEAPON, now)) {
            activeSignals.add(HudTrigger.CHARGING_WEAPON);
        }

        if (state.heldItem.hasAnyWeaponInHand() && state.t.active(HudTrigger.BLOCKING_ATTACK, now)) {
            activeSignals.add(HudTrigger.BLOCKING_ATTACK);
        }

        if (state.heldItem.rangedWeaponInHand) {
            activeSignals.add(HudTrigger.HOLDING_RANGED_WEAPON);
        }

        if (state.heldItem.meleeWeaponInHand) {
            activeSignals.add(HudTrigger.HOLDING_MELEE_WEAPON);
        }
    }
}