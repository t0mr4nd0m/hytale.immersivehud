package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.tom.immersivehudplugin.context.PlayerTickContext;
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
            boolean secondaryStart = isSecondaryStart(update);

            if (update.itemInHandId != null && secondaryStart) {
                handleSecondaryInteractionType(state, update.itemInHandId, now);
            }

            if (hasHotbarSlot(state, update)) {
                hotbarEvent |= applyHotbarSlotUpdate(state, now);
            }

            chargingStart |= isChargingStart(update);
            chargingEnd |= isChargingEnd(update);
        }

        if (chargingEnd) {
            state.t.clear(HudSignal.CHARGING_WEAPON);
        } else if (chargingStart) {
            state.t.pulse(HudSignal.CHARGING_WEAPON, now, state.hideDelayMsHint);
        } else if (hotbarEvent) {
            state.t.clear(HudSignal.CHARGING_WEAPON);
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
            state.t.clear(HudSignal.HOLDING_MELEE_WEAPON);
        }

        if (!state.rangedWeaponInHand) {
            state.t.clear(HudSignal.HOLDING_RANGED_WEAPON);
        }
    }

    private void handleSecondaryInteractionType(
            PlayerHudState state,
            String itemInHandId,
            long now
    ) {
        Item item = Item.getAssetMap().getAsset(itemInHandId);

        if (ItemInHand.isConsumable(item)) {
            state.t.pulse(HudSignal.CONSUMABLE_USE, now, state.hideDelayMsHint);
        }
    }

    private boolean applyHotbarSlotUpdate(
            PlayerHudState state,
            long now
    ) {
        state.invalidateHeldItemStateForHotbarSwitch();

        state.t.clear(HudSignal.CHARGING_WEAPON);
        state.t.clear(HudSignal.HOLDING_RANGED_WEAPON);
        state.t.clear(HudSignal.HOLDING_MELEE_WEAPON);
        state.t.pulse(HudSignal.HOTBAR_INPUT, now, state.hideDelayMsHint);
        return true;
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

    private static boolean isChargingStart(SyncInteractionChain update) {
        return update.interactionType == InteractionType.Primary;
    }

    private static boolean isChargingEnd(SyncInteractionChain update) {
        return update.interactionType == InteractionType.ProjectileHit
                || update.interactionType == InteractionType.ProjectileBounce
                || update.interactionType == InteractionType.ProjectileMiss;
    }

    private static boolean isSecondaryStart(SyncInteractionChain update) {
        return update.interactionType == InteractionType.Secondary;
    }

    private static boolean hasHotbarSlot(
            PlayerHudState state,
            SyncInteractionChain update
    ) {
        int slot = update.activeHotbarSlot;
        if (slot < 0 || slot > 8) { return false; }

        if (state.lastActiveHotbarSlot == -1) {
            state.lastActiveHotbarSlot = slot;
            return false;
        }

        if (state.lastActiveHotbarSlot != slot) {
            state.lastActiveHotbarSlot = slot;
            return true;
        }

        return false;
    }
}