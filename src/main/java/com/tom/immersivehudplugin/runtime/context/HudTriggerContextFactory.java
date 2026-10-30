package com.tom.immersivehudplugin.runtime.context;

import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.HudDetectionPlan;

import java.util.EnumSet;

public final class HudTriggerContextFactory {

    public HudTriggerContext create(PlayerHudState state, long now, HudDetectionPlan plan) {

        EnumSet<HudTrigger> activeSignals = EnumSet.noneOf(HudTrigger.class);

        for (HudTrigger trigger : plan.triggers()) {
            if (trigger.source() != HudTrigger.Source.SIGNAL) { continue; }
            boolean active = switch (trigger) {
                case HOLDING_RANGED_WEAPON -> state.heldItem.rangedWeaponInHand;
                case HOLDING_MELEE_WEAPON -> state.heldItem.meleeWeaponInHand;
                case HOLDING_BUILDING_BLOCK -> state.heldItem.buildingBlockInHand;
                case CHARGING_WEAPON, BLOCKING_ATTACK ->
                        state.heldItem.hasAnyWeaponInHand() && state.t.active(trigger, now);
                default -> state.t.active(trigger, now);
            };
            if (active) activeSignals.add(trigger);
        }

        return new HudTriggerContext(
                activeSignals,
                state.healthBar,
                state.staminaBar,
                state.manaBar,
                state.oxygenBar
        );
    }

}
