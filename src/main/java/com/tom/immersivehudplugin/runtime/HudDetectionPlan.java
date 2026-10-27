package com.tom.immersivehudplugin.runtime;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Immutable union of signals used by currently hidden HUD components. */
public final class HudDetectionPlan {
    private final Set<HudTrigger> triggers;
    private final boolean movement;

    private HudDetectionPlan(EnumSet<HudTrigger> triggers) {
        this.triggers = Collections.unmodifiableSet(EnumSet.copyOf(triggers));
        this.movement = triggers.stream().anyMatch(t -> t.category() == HudTrigger.Category.MOVEMENT);
    }

    public static HudDetectionPlan build(HudComponentsConfig hud, DynamicHudConfig dynamic) {
        EnumSet<HudTrigger> triggers = EnumSet.noneOf(HudTrigger.class);
        for (var component : HudComponentRegistry.dynamicList()) {
            if (component.isHidden(hud)) {
                triggers.addAll(component.requireDynamicRuleConfig(dynamic).getRules());
            }
        }
        return new HudDetectionPlan(triggers);
    }

    public Set<HudTrigger> triggers() { return triggers; }
    public boolean requires(HudTrigger trigger) { return triggers.contains(trigger); }
    public boolean enabled() { return !triggers.isEmpty(); }
    public boolean movement() { return movement; }
    public boolean reticle() {
        return requires(HudTrigger.TARGET_ENTITY) || requires(HudTrigger.INTERACTABLE_BLOCK);
    }
    public boolean interactions() {
        return requires(HudTrigger.CHARGING_WEAPON) || requires(HudTrigger.BLOCKING_ATTACK)
                || requires(HudTrigger.CONSUMABLE_USE);
    }
    public boolean weapons() {
        return requires(HudTrigger.HOLDING_RANGED_WEAPON) || requires(HudTrigger.HOLDING_MELEE_WEAPON)
                || requires(HudTrigger.CHARGING_WEAPON) || requires(HudTrigger.BLOCKING_ATTACK);
    }
    public boolean heldItem() {
        return weapons() || requires(HudTrigger.CONSUMABLE_USE) || requires(HudTrigger.HOLDING_BUILDING_BLOCK);
    }
    public boolean hotbar() {
        // Slot changes cancel weapon-action timers, even when HOTBAR_INPUT is disabled.
        return requires(HudTrigger.HOTBAR_INPUT) || requires(HudTrigger.CHARGING_WEAPON)
                || requires(HudTrigger.BLOCKING_ATTACK);
    }
    public boolean bars() {
        return requires(HudTrigger.HEALTH_NOT_FULL) || requires(HudTrigger.STAMINA_NOT_FULL)
                || requires(HudTrigger.MANA_NOT_FULL) || requires(HudTrigger.OXYGEN_NOT_FULL);
    }
}
