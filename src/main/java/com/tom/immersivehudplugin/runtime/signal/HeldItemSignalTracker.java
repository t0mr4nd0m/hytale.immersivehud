package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

import javax.annotation.Nullable;

public final class HeldItemSignalTracker {

    public HeldItemSignalTracker() {}

    public void applyPacketBatch(
            PlayerHudState state,
            SyncInteractionChains updates,
            long now
    ) {
        boolean hasChargingStart = false;
        boolean hasChargingEnd = false;
        boolean hasHotbarSwap = false;

        for (SyncInteractionChain update : updates.updates) {
            if (update.itemInHandId != null && isSecondaryStart(update)) {
                handleSecondaryInteractionType(state, update.itemInHandId, now);
            }

            if (isChargingEnd(update)) {
                hasChargingEnd = true;
            }

            if (isChargingStart(update) || (!hasChargingEnd && isPrimaryStart(update))) {
                hasChargingStart = true;
            }

            if (isSwapStart(update)) {
                hasHotbarSwap = true;
            }
        }

        if (hasChargingEnd) {
            state.t.clear(HudTrigger.CHARGING_WEAPON);
        }

        if (hasChargingStart) {
            state.t.pulse(HudTrigger.CHARGING_WEAPON, now, state.hideDelayMs);
        }

        if (hasHotbarSwap) {
            state.heldItem.refreshRequested = true;
            state.t.pulse(HudTrigger.HOTBAR_INPUT, now, state.hideDelayMs);
            state.t.clear(HudTrigger.CHARGING_WEAPON);
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
        Item item = resolveItem(update.itemInHandId);
        return isPrimaryStart(update) && HeldItemState.isWeapon(item);
    }

    private static boolean isChargingEnd(SyncInteractionChain update) {
        return update.interactionType == InteractionType.ProjectileHit
                || update.interactionType == InteractionType.ProjectileBounce
                || update.interactionType == InteractionType.ProjectileMiss;
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

            if (state.heldItem.lastActiveHotbarSlot == -1) {

                int slot = inventory.getActiveHotbarSlot();
                if (slot >= 0 && slot <= 8) {
                    state.heldItem.lastActiveHotbarSlot = slot;
                }
            }

            return resolveItem(itemId);

        } catch (Throwable ignored) {
            return null;
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
                HeldItemState.isRangedWeapon(heldItem),
                HeldItemState.isMeleeWeapon(heldItem),
                HeldItemState.isConsumable(heldItem)
        );
    }

    public void cleanupWeaponSignals(PlayerHudState state) {
        boolean hasMelee = state.heldItem.meleeWeaponInHand;
        boolean hasRanged = state.heldItem.rangedWeaponInHand;
        boolean hasWeapon = state.heldItem.hasAnyWeaponInHand();

        if (!hasMelee) {
            state.t.clear(HudTrigger.HOLDING_MELEE_WEAPON);
        }

        if (!hasRanged) {
            state.t.clear(HudTrigger.HOLDING_RANGED_WEAPON);
        }

        if (!hasWeapon) {
            state.t.clear(HudTrigger.CHARGING_WEAPON);
            state.t.clear(HudTrigger.BLOCKING_ATTACK);
        }
    }

    private void handleSecondaryInteractionType(
            PlayerHudState state,
            String itemInHandId,
            long now
    ) {
        Item item = resolveItem(itemInHandId);

        if (HeldItemState.isConsumable(item)) {
            state.t.pulse(HudTrigger.CONSUMABLE_USE, now, state.hideDelayMs);
        }

        if (HeldItemState.isWeapon(item)) {
            state.t.pulse(HudTrigger.BLOCKING_ATTACK, now, state.hideDelayMs);
        }
    }

    @Nullable
    private static Item resolveItem(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        return Item.getAssetMap().getAsset(itemId);
    }
}