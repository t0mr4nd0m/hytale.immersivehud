package com.tom.immersivehudplugin.runtime;

public final class PlayerHeldItemState {

    public volatile boolean rangedWeaponInHand;
    public volatile boolean meleeWeaponInHand;
    public volatile boolean consumableInHand;
    public volatile boolean buildingBlockInHand;
    private int activeHotbarSlot = Integer.MIN_VALUE;
    private boolean hotbarSlotInitialized;

    public void apply(
            boolean rangedWeapon,
            boolean meleeWeapon,
            boolean consumableItem,
            boolean buildingBlock
    ) {
        this.rangedWeaponInHand = rangedWeapon;
        this.meleeWeaponInHand = meleeWeapon;
        this.consumableInHand = consumableItem;
        this.buildingBlockInHand = buildingBlock;
    }

    public void reset() {
        rangedWeaponInHand = false;
        meleeWeaponInHand = false;
        consumableInHand = false;
        buildingBlockInHand = false;
        activeHotbarSlot = Integer.MIN_VALUE;
        hotbarSlotInitialized = false;
    }

    public boolean hasAnyWeaponInHand() {
        return rangedWeaponInHand || meleeWeaponInHand;
    }

    public boolean updateActiveHotbarSlot(int activeHotbarSlot) {
        if (!hotbarSlotInitialized) {
            this.activeHotbarSlot = activeHotbarSlot;
            this.hotbarSlotInitialized = true;
            return false;
        }

        if (this.activeHotbarSlot == activeHotbarSlot) {
            return false;
        }

        this.activeHotbarSlot = activeHotbarSlot;
        return true;
    }
}
