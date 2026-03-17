package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.utils.HudBarState;

import javax.annotation.Nullable;
import java.util.EnumSet;

public final class PlayerHudState {

    public final EnumSet<HudComponent> staticHidden = EnumSet.noneOf(HudComponent.class);
    public final EnumSet<HudComponent> dynamicHidden = EnumSet.noneOf(HudComponent.class);
    public final EnumSet<HudComponent> lastAppliedHidden = EnumSet.noneOf(HudComponent.class);
    public final EnumSet<HudComponent> tempHidden = EnumSet.noneOf(HudComponent.class);
    public final EnumSet<HudComponent> tempToHide = EnumSet.noneOf(HudComponent.class);
    public final EnumSet<HudComponent> tempToShow = EnumSet.noneOf(HudComponent.class);

    public final HudBarState healthBar = new HudBarState();
    public final HudBarState staminaBar = new HudBarState();
    public final HudBarState manaBar = new HudBarState();

    public volatile boolean staticHudInitialized;
    public volatile boolean staticDirty = true;

    public final HudTimers t = new HudTimers();

    public volatile long lastReticleScanMs;
    public volatile int lastSeenActiveHotbarSlot = -1;

    public volatile boolean rangedWeaponInHand;
    public volatile boolean meleeWeaponInHand;
    public volatile @Nullable Item heldItem;
    public volatile boolean heldItemStateInitialized;
    public volatile boolean heldItemRefreshRequested;

    public volatile boolean dynamicHudEnabledKnown;
    public volatile boolean dynamicHudEnabled;

    public volatile int hideDelayMsHint = GlobalConfig.HIDE_DELAY_MS;

    public void reset(int hideDelay) {
        staticHidden.clear();
        dynamicHidden.clear();
        lastAppliedHidden.clear();
        tempHidden.clear();
        tempToHide.clear();
        tempToShow.clear();

        healthBar.reset();
        staminaBar.reset();
        manaBar.reset();

        staticHudInitialized = false;
        staticDirty = true;

        t.clearAll();

        lastReticleScanMs = 0L;
        lastSeenActiveHotbarSlot = -1;

        rangedWeaponInHand = false;
        meleeWeaponInHand = false;
        heldItem = null;
        heldItemStateInitialized = false;
        heldItemRefreshRequested = true;

        dynamicHudEnabledKnown = false;
        dynamicHudEnabled = false;

        hideDelayMsHint = hideDelay;
    }
}