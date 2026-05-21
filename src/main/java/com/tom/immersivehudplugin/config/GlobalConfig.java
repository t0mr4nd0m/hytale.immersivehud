package com.tom.immersivehudplugin.config;

import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;

public final class GlobalConfig {

    public static final int INTERVAL_MS = 250;
    public static final int HIDE_DELAY_MS = 2000;
    public static final int INITIAL_HUD_GRACE_MS = 2000;

    public static final float RETICLE_TARGET_RANGE = 8.0f;
    public static final float COMBAT_SCAN_RANGE = 12f;

    private String configVersion = "";
    private int intervalMs = INTERVAL_MS;
    private int hideDelayMs = HIDE_DELAY_MS;
    private int initialHudGraceMs = INITIAL_HUD_GRACE_MS;

    private float reticleTargetRange = RETICLE_TARGET_RANGE;
    private float combatScanRange = COMBAT_SCAN_RANGE;

    private HudComponentsConfig defaultHudComponents = HudComponentRegistry.buildDefaultHudComponents();
    private DynamicHudConfig defaultDynamicHud = HudComponentRegistry.buildDefaultDynamicHud();

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public int getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(int intervalMs) {
        this.intervalMs = intervalMs;
    }

    public int getHideDelayMs() {
        return hideDelayMs;
    }

    public void setHideDelayMs(int hideDelayMs) {
        this.hideDelayMs = hideDelayMs;
    }

    public int getInitialHudGraceMs() {
        return initialHudGraceMs;
    }

    public void setInitialHudGraceMs(int initialHudGraceMs) {
        this.initialHudGraceMs = initialHudGraceMs;
    }

    public float getReticleTargetRange() {
        return reticleTargetRange;
    }

    public void setReticleTargetRange(float reticleTargetRange) {
        this.reticleTargetRange = reticleTargetRange;
    }

    public float getCombatScanRange() {
        return combatScanRange;
    }

    public void setCombatScanRange(float range) {
        this.combatScanRange = range;
    }

    public HudComponentsConfig getDefaultHudComponents() {
        if (defaultHudComponents == null) {
            defaultHudComponents = HudComponentRegistry.buildDefaultHudComponents();
        }
        return defaultHudComponents.copy();
    }

    public void setDefaultHudComponents(HudComponentsConfig value) {
        this.defaultHudComponents = (value != null)
                ? value.copy()
                : HudComponentRegistry.buildDefaultHudComponents();
    }

    public DynamicHudConfig getDefaultDynamicHud() {
        if (defaultDynamicHud == null) {
            defaultDynamicHud = HudComponentRegistry.buildDefaultDynamicHud();
        }
        return defaultDynamicHud.copy();
    }

    public void setDefaultDynamicHud(DynamicHudConfig value) {
        this.defaultDynamicHud = (value != null)
                ? value.copy()
                : HudComponentRegistry.buildDefaultDynamicHud();
    }

    public boolean sanitize() {
        boolean changed = false;

        if (intervalMs <= 0) {
            intervalMs = INTERVAL_MS;
            changed = true;
        }

        if (hideDelayMs < 0) {
            hideDelayMs = HIDE_DELAY_MS;
            changed = true;
        }

        if (initialHudGraceMs < 0) {
            initialHudGraceMs = INITIAL_HUD_GRACE_MS;
            changed = true;
        }

        if (combatScanRange <= 0f) {
            combatScanRange = COMBAT_SCAN_RANGE;
            changed = true;
        }

        if (reticleTargetRange <= 0f) {
            reticleTargetRange = RETICLE_TARGET_RANGE;
            changed = true;
        }

        if (defaultHudComponents == null) {
            defaultHudComponents = HudComponentRegistry.buildDefaultHudComponents();
            changed = true;
        } else {
            changed |= defaultHudComponents.sanitize();
        }

        if (defaultDynamicHud == null) {
            defaultDynamicHud = HudComponentRegistry.buildDefaultDynamicHud();
            changed = true;
        } else {
            changed |= defaultDynamicHud.sanitize();
        }

        if (configVersion == null) {
            configVersion = "";
            changed = true;
        }

        return changed;
    }
}