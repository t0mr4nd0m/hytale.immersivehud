package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.utils.HudBarState;

import javax.annotation.Nullable;
import java.util.EnumSet;

public final class PlayerHudState {

    private final EnumSet<HudComponent> staticHidden = EnumSet.noneOf(HudComponent.class);
    private final EnumSet<HudComponent> dynamicHidden = EnumSet.noneOf(HudComponent.class);
    private final EnumSet<HudComponent> lastAppliedHidden = EnumSet.noneOf(HudComponent.class);

    private final EnumSet<HudComponent> tempHidden = EnumSet.noneOf(HudComponent.class);
    private final EnumSet<HudComponent> tempToHide = EnumSet.noneOf(HudComponent.class);
    private final EnumSet<HudComponent> tempToShow = EnumSet.noneOf(HudComponent.class);

    public final HudBarState healthBar = new HudBarState();
    public final HudBarState staminaBar = new HudBarState();
    public final HudBarState manaBar = new HudBarState();
    public final HudBarState oxygenBar = new HudBarState();

    public final HudTimers t = new HudTimers();

    public boolean staticHudInitialized;
    public boolean staticDirty = true;

    public volatile long lastReticleScanMs;
    public volatile int lastSeenActiveHotbarSlot = -1;

    public volatile boolean rangedWeaponInHand;
    public volatile boolean meleeWeaponInHand;
    public volatile @Nullable Item heldItem;
    public volatile boolean heldItemStateInitialized;
    public volatile boolean heldItemRefreshRequested;

    public boolean dynamicHudEnabledKnown;
    public boolean dynamicHudEnabled;

    public volatile int hideDelayMsHint = GlobalConfig.HIDE_DELAY_MS;

    public void reset(int hideDelay) {
        resetHudVisibilityState();
        resetBars();
        resetTimersAndScans();
        resetHeldItemState();
        resetDynamicHudCache();

        hideDelayMsHint = hideDelay;
    }

    public void markStaticHudDirty() {
        staticHudInitialized = false;
        staticDirty = true;
    }

    public void invalidateDynamicHudEnabledCache() {
        dynamicHudEnabledKnown = false;
    }

    public void cacheDynamicHudEnabled(boolean enabled) {
        dynamicHudEnabled = enabled;
        dynamicHudEnabledKnown = true;
    }

    public boolean hasDynamicHudEnabledCache() {
        return dynamicHudEnabledKnown;
    }

    public boolean isDynamicHudEnabledCached() {
        return dynamicHudEnabled;
    }

    public void applyHeldItemState(
            @Nullable Item item,
            boolean rangedWeapon,
            boolean meleeWeapon
    ) {
        heldItem = item;
        rangedWeaponInHand = rangedWeapon;
        meleeWeaponInHand = meleeWeapon;
        heldItemStateInitialized = true;
        heldItemRefreshRequested = false;
    }

    public void requestHeldItemRefresh() {
        heldItemRefreshRequested = true;
        heldItemStateInitialized = false;
    }

    public void clearHeldItemState() {
        heldItem = null;
        rangedWeaponInHand = false;
        meleeWeaponInHand = false;
        heldItemStateInitialized = false;
        heldItemRefreshRequested = true;
    }

    public boolean isAnyWeaponInHand() {
        return rangedWeaponInHand || meleeWeaponInHand;
    }

    public void clearStaticHidden() {
        staticHidden.clear();
    }

    public void addStaticHidden(HudComponent component) {
        staticHidden.add(component);
    }

    public void clearDynamicHidden() {
        dynamicHidden.clear();
    }

    public void addDynamicHidden(HudComponent component) {
        dynamicHidden.add(component);
    }

    public boolean hasDynamicHidden() {
        return !dynamicHidden.isEmpty();
    }

    public HudDelta prepareHudDelta() {
        tempHidden.clear();
        tempHidden.addAll(staticHidden);
        tempHidden.addAll(dynamicHidden);

        boolean changed = !tempHidden.equals(lastAppliedHidden);

        tempToHide.clear();
        tempToHide.addAll(tempHidden);
        tempToHide.removeAll(lastAppliedHidden);

        tempToShow.clear();
        tempToShow.addAll(lastAppliedHidden);
        tempToShow.removeAll(tempHidden);

        return new HudDelta(tempHidden, tempToHide, tempToShow, changed);
    }

    public void commitAppliedHidden(EnumSet<HudComponent> effectiveHidden) {
        lastAppliedHidden.clear();
        lastAppliedHidden.addAll(effectiveHidden);
    }

    private void resetHudVisibilityState() {
        staticHidden.clear();
        dynamicHidden.clear();
        lastAppliedHidden.clear();
        tempHidden.clear();
        tempToHide.clear();
        tempToShow.clear();

        staticHudInitialized = false;
        staticDirty = true;
    }

    private void resetBars() {
        healthBar.reset();
        staminaBar.reset();
        manaBar.reset();
        oxygenBar.reset();
    }

    private void resetTimersAndScans() {
        t.clearAll();
        lastReticleScanMs = 0L;
        lastSeenActiveHotbarSlot = -1;
    }

    private void resetHeldItemState() {
        heldItem = null;
        rangedWeaponInHand = false;
        meleeWeaponInHand = false;
        heldItemStateInitialized = false;
        heldItemRefreshRequested = true;
    }

    private void resetDynamicHudCache() {
        dynamicHudEnabledKnown = false;
        dynamicHudEnabled = false;
    }

    public record HudDelta(
            EnumSet<HudComponent> effectiveHidden,
            EnumSet<HudComponent> toHide,
            EnumSet<HudComponent> toShow,
            boolean changed
    ) {}

    public boolean needsHeldItemRepair() {
        return !heldItemStateInitialized || heldItemRefreshRequested;
    }

    public void invalidateHeldItemStateForHotbarSwitch() {
        heldItem = null;
        rangedWeaponInHand = false;
        meleeWeaponInHand = false;
        heldItemStateInitialized = false;
        heldItemRefreshRequested = true;
    }
}