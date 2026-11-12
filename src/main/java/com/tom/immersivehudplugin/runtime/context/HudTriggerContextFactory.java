package com.tom.immersivehudplugin.runtime.context;

import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.HudDetectionPlan;

import java.util.EnumSet;
import javax.annotation.Nullable;

public final class HudTriggerContextFactory {

    public HudTriggerContext create(PlayerHudState state, long now, HudDetectionPlan plan) {
        return create(state, now, plan, null);
    }

    public HudTriggerContext create(PlayerHudState state, long now, HudDetectionPlan plan,
                                    @Nullable PlayerTickContext tickContext) {

        EnumSet<HudTrigger> activeSignals = EnumSet.noneOf(HudTrigger.class);

        for (HudTrigger trigger : plan.triggers()) {
            if (trigger.source() != HudTrigger.Source.SIGNAL) { continue; }
            boolean active = switch (trigger) {
                case HAS_ASSIGNED_RUNE ->
                        state.heldItem.hasAnyWeaponInHand() && hasAssignedRune(tickContext);
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

    private boolean hasAssignedRune(@Nullable PlayerTickContext context) {
        if (context == null) return false;
        var slots = context.store().getComponent(context.ref(), InventoryComponent.AbilitySlots.getComponentType());
        return slots != null && !slots.getInventory().isEmpty();
    }

}
