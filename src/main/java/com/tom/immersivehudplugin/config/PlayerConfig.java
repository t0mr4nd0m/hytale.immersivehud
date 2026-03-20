package com.tom.immersivehudplugin.config;

import com.tom.immersivehudplugin.registry.HudComponentRegistry;

public final class PlayerConfig {

    private HudComponentsConfig hudComponents = HudComponentRegistry.buildDefaultHudComponents();
    private DynamicHudConfig dynamicHud = HudComponentRegistry.buildDefaultDynamicHud();

    public HudComponentsConfig getHudComponents() {
        return hudComponents;
    }

    public void setHudComponents(HudComponentsConfig hudComponents) {
        this.hudComponents = (hudComponents != null) ? hudComponents : HudComponentRegistry.buildDefaultHudComponents();
    }

    public DynamicHudConfig getDynamicHud() {
        return dynamicHud;
    }

    public void setDynamicHud(DynamicHudConfig dynamicHud) {
        this.dynamicHud = (dynamicHud != null) ? dynamicHud : HudComponentRegistry.buildDefaultDynamicHud();
    }

    public boolean sanitize() {
        boolean changed = false;

        if (hudComponents == null) {
            hudComponents = HudComponentRegistry.buildDefaultHudComponents();
            changed = true;
        } else {
            changed |= hudComponents.sanitize();
        }

        if (dynamicHud == null) {
            dynamicHud = HudComponentRegistry.buildDefaultDynamicHud();
            changed = true;
        } else {
            changed |= dynamicHud.sanitize();
        }

        return changed;
    }

    public static PlayerConfig fromDefaults(
            HudComponentsConfig defaultHud,
            DynamicHudConfig defaultDynamic
    ) {
        PlayerConfig cfg = new PlayerConfig();
        cfg.setHudComponents(defaultHud != null ? defaultHud.copy() : HudComponentRegistry.buildDefaultHudComponents());
        cfg.setDynamicHud(defaultDynamic != null ? defaultDynamic.copy() : HudComponentRegistry.buildDefaultDynamicHud());
        return cfg;
    }
}