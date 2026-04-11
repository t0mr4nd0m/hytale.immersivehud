package com.tom.immersivehudplugin.config;

import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudDefaults;

public final class GlobalConfig {

    public static final int INTERVAL_MS = 250;
    public static final int HIDE_DELAY_MS = 2000;
    public static final float RETICLE_TARGET_RANGE = 8.0f;

    private String configVersion = "";
    private int intervalMs = INTERVAL_MS;
    private int hideDelayMs = HIDE_DELAY_MS;
    private float reticleTargetRange = RETICLE_TARGET_RANGE;

    private HudComponentsConfig defaultHudComponents = HudDefaults.buildDefaultHudComponents();
    private DynamicHudConfig defaultDynamicHud = HudDefaults.buildDefaultDynamicHud();

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

    public float getReticleTargetRange() {
        return reticleTargetRange;
    }

    public void setReticleTargetRange(float reticleTargetRange) {
        this.reticleTargetRange = reticleTargetRange;
    }

    public HudComponentsConfig getDefaultHudComponents() {
        if (defaultHudComponents == null) {
            defaultHudComponents = HudDefaults.buildDefaultHudComponents();
        }
        return defaultHudComponents.copy();
    }

    public void setDefaultHudComponents(HudComponentsConfig value) {
        this.defaultHudComponents = (value != null)
                ? value.copy()
                : HudDefaults.buildDefaultHudComponents();
    }

    public DynamicHudConfig getDefaultDynamicHud() {
        if (defaultDynamicHud == null) {
            defaultDynamicHud = HudDefaults.buildDefaultDynamicHud();
        }
        return defaultDynamicHud.copy();
    }

    public void setDefaultDynamicHud(DynamicHudConfig value) {
        this.defaultDynamicHud = (value != null)
                ? value.copy()
                : HudDefaults.buildDefaultDynamicHud();
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

        if (reticleTargetRange <= 0f) {
            reticleTargetRange = RETICLE_TARGET_RANGE;
            changed = true;
        }

        if (defaultHudComponents == null) {
            defaultHudComponents = HudDefaults.buildDefaultHudComponents();
            changed = true;
        } else {
            changed |= defaultHudComponents.sanitize();
        }

        if (defaultDynamicHud == null) {
            defaultDynamicHud = HudDefaults.buildDefaultDynamicHud();
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