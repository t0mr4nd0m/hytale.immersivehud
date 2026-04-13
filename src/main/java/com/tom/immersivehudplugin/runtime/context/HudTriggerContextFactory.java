package com.tom.immersivehudplugin.runtime.context;

import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

import java.util.EnumSet;

public final class HudTriggerContextFactory {

    public HudTriggerContext create(PlayerHudState state, long now) {
        EnumSet<HudTrigger> activeSignals = EnumSet.noneOf(HudTrigger.class);

        addIfActive(activeSignals, state, HudTrigger.HOTBAR_INPUT, now);
        addIfActive(activeSignals, state, HudTrigger.CONSUMABLE_USE, now);
        addIfActive(activeSignals, state, HudTrigger.TARGET_ENTITY, now);
        addIfActive(activeSignals, state, HudTrigger.INTERACTABLE_BLOCK, now);

        addIfActive(activeSignals, state, HudTrigger.PLAYER_MOVING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_WALKING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_RUNNING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_SPRINTING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_MOUNTING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_SWIMMING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_FLYING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_GLIDING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_JUMPING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_CROUCHING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_CLIMBING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_FALLING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_ROLLING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_IDLE, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_SITTING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_SLEEPING, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_IN_FLUID, now);
        addIfActive(activeSignals, state, HudTrigger.PLAYER_ON_GROUND, now);

        if ((state.rangedWeaponInHand || state.meleeWeaponInHand)
                && state.t.active(HudTrigger.CHARGING_WEAPON, now)) {
            activeSignals.add(HudTrigger.CHARGING_WEAPON);
        }

        if ((state.rangedWeaponInHand || state.meleeWeaponInHand)
                && state.t.active(HudTrigger.BLOCKING_ATTACK, now)) {
            activeSignals.add(HudTrigger.BLOCKING_ATTACK);
        }

        if (state.rangedWeaponInHand) {
            activeSignals.add(HudTrigger.HOLDING_RANGED_WEAPON);
        }

        if (state.meleeWeaponInHand) {
            activeSignals.add(HudTrigger.HOLDING_MELEE_WEAPON);
        }

        return new HudTriggerContext(
                activeSignals,
                state.healthBar,
                state.staminaBar,
                state.manaBar,
                state.oxygenBar
        );
    }

    private void addIfActive(
            EnumSet<HudTrigger> activeSignals,
            PlayerHudState state,
            HudTrigger trigger,
            long now
    ) {
        if (state.t.active(trigger, now)) {
            activeSignals.add(trigger);
        }
    }
}