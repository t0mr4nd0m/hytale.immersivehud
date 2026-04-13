package com.tom.immersivehudplugin.runtime;

public final class PlayerHeldItemState {

    public volatile boolean rangedWeaponInHand;
    public volatile boolean meleeWeaponInHand;
    public volatile boolean consumableInHand;
    public volatile boolean initialized;
    public volatile boolean refreshRequested = true;
    public volatile int lastActiveHotbarSlot = -1;

    public void apply(
            boolean rangedWeapon,
            boolean meleeWeapon,
            boolean consumableItem
    ) {
        this.rangedWeaponInHand = rangedWeapon;
        this.meleeWeaponInHand = meleeWeapon;
        this.consumableInHand = consumableItem;
        this.initialized = true;
        this.refreshRequested = false;
    }

    public void reset() {
        rangedWeaponInHand = false;
        meleeWeaponInHand = false;
        consumableInHand = false;
        initialized = false;
        refreshRequested = true;
        lastActiveHotbarSlot = -1;
    }

    public boolean needsRepair() {
        return !initialized || refreshRequested;
    }

    public boolean hasAnyWeaponInHand() {
        return rangedWeaponInHand || meleeWeaponInHand;
    }
}