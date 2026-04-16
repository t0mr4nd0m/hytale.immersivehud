package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.trigger.HudBarState;

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
    public final PlayerHeldItemState heldItem = new PlayerHeldItemState();
    public final DynamicHudCache dynamicHudCache = new DynamicHudCache();

    public boolean staticHudInitialized;
    public boolean staticDirty = true;

    public volatile long lastReticleScanMs;
    public volatile int hideDelayMs = GlobalConfig.HIDE_DELAY_MS;

    public void reset(int hideDelay) {
        resetHudVisibilityState();
        resetBars();
        resetTimersAndScans();
        heldItem.reset();
        dynamicHudCache.reset();

        hideDelayMs = hideDelay;
    }

    public void markStaticHudDirty() {
        staticHudInitialized = false;
        staticDirty = true;
    }

    public void invalidateDynamicHudEnabledCache() {
        dynamicHudCache.invalidate();
    }

    public void cacheDynamicHudEnabled(boolean enabled) {
        dynamicHudCache.cache(enabled);
    }

    public boolean hasDynamicHudEnabledCache() {
        return dynamicHudCache.isKnown();
    }

    public boolean isDynamicHudEnabledCached() {
        return dynamicHudCache.isEnabled();
    }

    public void applyHeldItemState(
            boolean rangedWeapon,
            boolean meleeWeapon,
            boolean consumableItem
    ) {
        heldItem.apply(rangedWeapon, meleeWeapon, consumableItem);
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
    }

    public record HudDelta(
            EnumSet<HudComponent> effectiveHidden,
            EnumSet<HudComponent> toHide,
            EnumSet<HudComponent> toShow,
            boolean changed
    ) {}

    public boolean needsHeldItemRepair() {
        return heldItem.needsRepair();
    }
}