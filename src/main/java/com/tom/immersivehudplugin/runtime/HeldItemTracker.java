package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.tom.immersivehudplugin.context.PlayerTickContext;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;
import com.tom.immersivehudplugin.utils.ItemInHand;

import javax.annotation.Nullable;

public final class HeldItemTracker {

    public HeldItemTracker() {}

    public void applyPacketBatch(
            PlayerHudState state,
            SyncInteractionChains updates,
            long now
    ) {
        boolean chargingStart = false;
        boolean chargingEnd = false;
        boolean hotbarEvent = false;

        for (SyncInteractionChain update : updates.updates) {

            if (update.itemInHandId != null && isSecondaryStart(update)) {
                handleSecondaryInteractionType(state, update.itemInHandId, now);
            }

            chargingEnd |= isChargingEnd(update);

            chargingStart |= (isChargingStart(update) || (!chargingEnd && isPrimaryStart(update)));

            hotbarEvent |= isSwapStart(update);
        }

        if (chargingEnd) {
            state.t.clear(DynamicHudTriggers.CHARGING_WEAPON);
        }

        if (chargingStart) {
            state.t.pulse(DynamicHudTriggers.CHARGING_WEAPON, now, state.hideDelayMsHint);
        }

        if (hotbarEvent) {
            state.heldItemRefreshRequested = true;
            state.t.pulse(DynamicHudTriggers.HOTBAR_INPUT, now, state.hideDelayMsHint);
            state.t.clear(DynamicHudTriggers.CHARGING_WEAPON);
        }
    }

    public void repairFromInventoryIfNeeded(
            PlayerHudState state,
            PlayerTickContext tickContext
    ) {
        if (!state.needsHeldItemRepair()) {
            return;
        }

        Item heldItem = getHeldItemFromInventory(state, tickContext);

        state.applyHeldItemState(
                ItemInHand.isRangedWeapon(heldItem),
                ItemInHand.isMeleeWeapon(heldItem),
                ItemInHand.isConsumable(heldItem)
        );
    }

    public void cleanupWeaponSignals(PlayerHudState state) {
        if (!state.meleeWeaponInHand) {
            state.t.clear(DynamicHudTriggers.HOLDING_MELEE_WEAPON);
        }

        if (!state.rangedWeaponInHand) {
            state.t.clear(DynamicHudTriggers.HOLDING_RANGED_WEAPON);
        }

        if (!state.meleeWeaponInHand && !state.rangedWeaponInHand) {
            state.t.clear(DynamicHudTriggers.CHARGING_WEAPON);
        }
    }

    private void handleSecondaryInteractionType(
            PlayerHudState state,
            String itemInHandId,
            long now
    ) {
        Item item = Item.getAssetMap().getAsset(itemInHandId);

        if (ItemInHand.isConsumable(item)) {
            state.t.pulse(DynamicHudTriggers.CONSUMABLE_USE, now, state.hideDelayMsHint);
        }
    }

    @Nullable
    private Item getHeldItemFromInventory(
            PlayerHudState state,
            PlayerTickContext tickContext
    ) {
        try {
            var inventory = tickContext.player().getInventory();
            if (inventory == null) { return null; }

            var heldStack = inventory.getActiveHotbarItem();
            if (heldStack == null) { return null; }

            String itemId = heldStack.getItemId();
            if (itemId.isBlank()) { return null; }

            if (state.lastActiveHotbarSlot == -1) {

                int slot = inventory.getActiveHotbarSlot();
                if (slot >= 0 && slot <= 8) {
                    state.lastActiveHotbarSlot = slot;
                }
            }

            return Item.getAssetMap().getAsset(itemId);

        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isPrimaryStart(SyncInteractionChain update) {
        return update.interactionType == InteractionType.Primary;
    }

    private static boolean isSecondaryStart(SyncInteractionChain update) {
        return update.interactionType == InteractionType.Secondary;
    }

    private static boolean isSwapStart(SyncInteractionChain update) {
        return update.interactionType == InteractionType.SwapTo
                || update.interactionType == InteractionType.SwapFrom;
    }

    private static boolean isChargingStart(SyncInteractionChain update) {
        Item item = Item.getAssetMap().getAsset(update.itemInHandId);
        return isPrimaryStart(update) && ItemInHand.isWeapon(item);
    }

    private static boolean isChargingEnd(SyncInteractionChain update) {
        return update.interactionType == InteractionType.ProjectileHit
                || update.interactionType == InteractionType.ProjectileBounce
                || update.interactionType == InteractionType.ProjectileMiss;
    }
}