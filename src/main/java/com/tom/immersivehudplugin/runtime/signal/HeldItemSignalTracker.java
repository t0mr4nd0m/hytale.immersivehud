package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

import javax.annotation.Nullable;

public final class HeldItemSignalTracker {

    public HeldItemSignalTracker() {}

    /**
     * Refreshes the held-item classification directly from the authoritative
     * inventory state.
     * This must run before updateInteractionSignals().
     */
    public void refreshFromInventory(
            PlayerHudState state,
            PlayerTickContext tickContext,
            long now
    ) {
        refreshHotbarSlot(state, tickContext, now);

        Item heldItem = getHeldItemFromInventory(tickContext);

        state.applyHeldItemState(
                HeldItemState.isRangedWeapon(heldItem),
                HeldItemState.isMeleeWeapon(heldItem),
                HeldItemState.isConsumable(heldItem)
        );
    }

    private void refreshHotbarSlot(
            PlayerHudState state,
            PlayerTickContext tickContext,
            long now
    ) {
        InventoryComponent.Hotbar hotbar =
                tickContext.store().getComponent(
                        tickContext.ref(),
                        InventoryComponent.Hotbar.getComponentType()
                );

        if (hotbar == null) return;

        int activeSlot = hotbar.getActiveSlot();

        boolean changed = state.heldItem.updateActiveHotbarSlot(activeSlot);
        if (!changed) return;

        state.t.pulse(HudTrigger.HOTBAR_INPUT, now, state.hideDelayMs);
        state.t.clear(HudTrigger.CHARGING_WEAPON);
        state.t.clear(HudTrigger.BLOCKING_ATTACK);
    }

    /**
     * Reads the active interaction chains from the player's InteractionManager
     * and updates the corresponding HUD trigger signals.
     */
    public void updateInteractionSignals(
            PlayerHudState state,
            PlayerTickContext tickContext,
            long now
    ) {
        InteractionManager interactionManager =
                tickContext.store().getComponent(
                        tickContext.ref(),
                        InteractionModule.get().getInteractionManagerComponent()
                );

        if (interactionManager == null) return;

        boolean chargingWeapon = false;
        boolean consuming = false;
        boolean blockingAttack = false;

        for (InteractionChain chain : interactionManager.getChains().values()) {
            if (chain == null || chain.getServerState() != InteractionState.NotFinished) continue;

            InteractionType type = chain.getType();

            if (type == InteractionType.Primary && state.heldItem.hasAnyWeaponInHand()) chargingWeapon = true;

            if (type == InteractionType.Secondary) {
                if (state.heldItem.consumableInHand) consuming = true;
                if (state.heldItem.hasAnyWeaponInHand()) blockingAttack = true;
            }
        }

        if (chargingWeapon) state.t.pulse(HudTrigger.CHARGING_WEAPON, now, state.hideDelayMs);
        if (consuming) state.t.pulse(HudTrigger.CONSUMABLE_USE, now, state.hideDelayMs);
        if (blockingAttack) state.t.pulse(HudTrigger.BLOCKING_ATTACK, now, state.hideDelayMs);
    }

    /**
     * Clears weapon-related signals when the corresponding weapon type is no
     * longer equipped.
     */
    public void cleanupWeaponSignals(PlayerHudState state) {
        boolean hasMelee = state.heldItem.meleeWeaponInHand;
        boolean hasRanged = state.heldItem.rangedWeaponInHand;
        boolean hasWeapon = state.heldItem.hasAnyWeaponInHand();

        if (!hasMelee) state.t.clear(HudTrigger.HOLDING_MELEE_WEAPON);
        if (!hasRanged) state.t.clear(HudTrigger.HOLDING_RANGED_WEAPON);
        if (!hasWeapon) {
            state.t.clear(HudTrigger.CHARGING_WEAPON);
            state.t.clear(HudTrigger.BLOCKING_ATTACK);
        }
    }

    @Nullable
    private Item getHeldItemFromInventory(PlayerTickContext tickContext) {
        try {
            var entityStoreHolder = tickContext.player().toHolder();
            if (entityStoreHolder == null) return null;

            var heldStack = InventoryComponent.getItemInHand(entityStoreHolder);
            if (heldStack == null) return null;

            String itemId = heldStack.getItemId();
            if (itemId == null || itemId.isBlank()) return null;

            return resolveItem(itemId);

        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Item resolveItem(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) return null;
        return Item.getAssetMap().getAsset(itemId);
    }
}