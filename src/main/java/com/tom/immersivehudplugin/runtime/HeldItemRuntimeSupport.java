package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.tom.immersivehudplugin.context.PlayerTickContext;
import com.tom.immersivehudplugin.utils.ItemInHand;

import javax.annotation.Nullable;

public final class HeldItemRuntimeSupport {

    private final AssetMap<String, Item> itemAssetMap;

    public HeldItemRuntimeSupport(AssetMap<String, Item> itemAssetMap) {
        this.itemAssetMap = itemAssetMap;
    }

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

            if (update.itemInHandId != null) {
                handlePacketItemUseOnly(state, update.itemInHandId, secondaryStart, now);
            }

            if (hasHotbarSlot(update)) {
                hotbarEvent = true;
                applyHotbarSlotUpdate(state, update.activeHotbarSlot, now);
            }

            if (isChargingStart(update)) {
                chargingStart = true;
            }

            if (isChargingEnd(update)) {
                chargingEnd = true;
            }
        }

        if (hotbarEvent) {
            state.t.clear(HudSignal.CHARGING_WEAPON);
        }

        if (chargingEnd) {
            state.t.clear(HudSignal.CHARGING_WEAPON);
        } else if (chargingStart) {
            state.t.pulse(HudSignal.CHARGING_WEAPON, now, state.hideDelayMsHint);
        }
    }

    public void repairFromInventoryIfNeeded(
            PlayerHudState state,
            PlayerTickContext tickContext
    ) {
        if (!state.needsHeldItemRepair()) {
            return;
        }

        Item heldItem = getHeldItemFromInventory(tickContext);

        state.applyHeldItemState(
                heldItem,
                ItemInHand.isRangedWeapon(heldItem),
                ItemInHand.isMeleeWeapon(heldItem)
        );
    }

    public void cleanupWeaponSignals(PlayerHudState state) {
        if (!state.meleeWeaponInHand) {
            state.t.clear(HudSignal.HOLDING_MELEE_WEAPON);
        }

        if (!state.rangedWeaponInHand) {
            state.t.clear(HudSignal.HOLDING_RANGED_WEAPON);
        }

        if (!state.isAnyWeaponInHand()) {
            state.t.clear(HudSignal.CHARGING_WEAPON);
        }
    }

    private void handlePacketItemUseOnly(
            PlayerHudState state,
            String itemInHandId,
            boolean secondaryStart,
            long now
    ) {
        Item item = itemAssetMap.getAsset(itemInHandId);

        if (secondaryStart && item != null && item.isConsumable()) {
            state.t.pulse(HudSignal.CONSUMABLE_USE, now, state.hideDelayMsHint);
        }
    }

    private void applyHotbarSlotUpdate(
            PlayerHudState state,
            int slot,
            long now
    ) {
        state.lastSeenActiveHotbarSlot = slot;
        state.invalidateHeldItemStateForHotbarSwitch();

        state.t.clear(HudSignal.CHARGING_WEAPON);
        state.t.clear(HudSignal.HOLDING_RANGED_WEAPON);
        state.t.clear(HudSignal.HOLDING_MELEE_WEAPON);
        state.t.pulse(HudSignal.HOTBAR_INPUT, now, state.hideDelayMsHint);
    }

    @Nullable
    private Item getHeldItemFromInventory(PlayerTickContext tickContext) {
        try {
            var inventory = tickContext.player().getInventory();
            if (inventory == null) {
                return null;
            }

            var heldStack = inventory.getActiveHotbarItem();
            if (heldStack == null) {
                return null;
            }

            String itemId = heldStack.getItemId();
            if (itemId == null || itemId.isBlank()) {
                return null;
            }

            return itemAssetMap.getAsset(itemId);

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

    private static boolean hasHotbarSlot(SyncInteractionChain update) {
        return update.activeHotbarSlot >= 0 && update.activeHotbarSlot <= 8;
    }
}