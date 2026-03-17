package com.tom.immersivehudplugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class GlobalConfig {

    public static final String CONFIG_SCHEMA_VERSION = "1.0.0";

    public static final int INTERVAL_MS = 250;
    public static final int HIDE_DELAY_MS = 2000;
    public static final float RETICLE_TARGET_RANGE = 8.0f;

    private String configVersion = CONFIG_SCHEMA_VERSION;
    private int intervalMs = INTERVAL_MS;
    private int hideDelayMs = HIDE_DELAY_MS;
    private float reticleTargetRange = RETICLE_TARGET_RANGE;

    private HudComponentsConfig defaultHudComponents = new HudComponentsConfig();
    private DynamicHudConfig defaultDynamicHud = new DynamicHudConfig();

    public static final BuilderCodec<GlobalConfig> CODEC =
            BuilderCodec.builder(GlobalConfig.class, GlobalConfig::new)

                    .append(new KeyedCodec<>("ConfigVersion", Codec.STRING),
                            (cfg, v) -> cfg.configVersion = v,
                            GlobalConfig::getConfigVersion)
                    .add()

                    .append(new KeyedCodec<>("IntervalMs", Codec.INTEGER),
                            (cfg, v) -> cfg.intervalMs = v,
                            GlobalConfig::getIntervalMs)
                    .add()

                    .append(new KeyedCodec<>("HideDelayMs", Codec.INTEGER),
                            (cfg, v) -> cfg.hideDelayMs = v,
                            GlobalConfig::getHideDelayMs)
                    .add()

                    .append(new KeyedCodec<>("ReticleTargetRange", Codec.FLOAT),
                            (cfg, v) -> cfg.reticleTargetRange = v,
                            GlobalConfig::getReticleTargetRange)
                    .add()

                    .append(new KeyedCodec<>("DefaultHudComponents", HudComponentsConfig.CODEC),
                            (cfg, v) -> cfg.defaultHudComponents = (v != null ? v : new HudComponentsConfig()),
                            GlobalConfig::getDefaultHudComponents)
                    .add()

                    .append(new KeyedCodec<>("DefaultDynamicHud", DynamicHudConfig.CODEC),
                            (cfg, v) -> cfg.defaultDynamicHud = (v != null ? v : new DynamicHudConfig()),
                            GlobalConfig::getDefaultDynamicHud)
                    .add()

                    .build();

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public int getIntervalMs() {
        return intervalMs;
    }

    public int getHideDelayMs() {
        return hideDelayMs;
    }

    public float getReticleTargetRange() {
        return reticleTargetRange;
    }

    public HudComponentsConfig getDefaultHudComponents() {
        if (defaultHudComponents == null) {
            defaultHudComponents = new HudComponentsConfig();
        }
        return defaultHudComponents;
    }

    public void setDefaultHudComponents(HudComponentsConfig defaultHudComponents) {
        this.defaultHudComponents = (defaultHudComponents != null) ? defaultHudComponents : new HudComponentsConfig();
    }

    public DynamicHudConfig getDefaultDynamicHud() {
        if (defaultDynamicHud == null) {
            defaultDynamicHud = new DynamicHudConfig();
        }
        return defaultDynamicHud;
    }

    public void setDefaultDynamicHud(DynamicHudConfig defaultDynamicHud) {
        this.defaultDynamicHud = (defaultDynamicHud != null) ? defaultDynamicHud : new DynamicHudConfig();
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
            defaultHudComponents = new HudComponentsConfig();
            changed = true;
        } else {
            changed |= defaultHudComponents.sanitize();
        }

        if (defaultDynamicHud == null) {
            defaultDynamicHud = new DynamicHudConfig();
            changed = true;
        } else {
            changed |= defaultDynamicHud.sanitize();
        }

        return changed;
    }
}