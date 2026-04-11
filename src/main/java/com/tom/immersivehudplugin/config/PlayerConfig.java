package com.tom.immersivehudplugin.config;

import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudDefaults;

public final class PlayerConfig {

    private HudComponentsConfig hudComponents = HudDefaults.buildDefaultHudComponents();
    private DynamicHudConfig dynamicHud = HudDefaults.buildDefaultDynamicHud();

    public HudComponentsConfig getHudComponents() {
        return hudComponents;
    }

    public void setHudComponents(HudComponentsConfig hudComponents) {
        this.hudComponents = (hudComponents != null) ? hudComponents : HudDefaults.buildDefaultHudComponents();
    }

    public DynamicHudConfig getDynamicHud() {
        return dynamicHud;
    }

    public void setDynamicHud(DynamicHudConfig dynamicHud) {
        this.dynamicHud = (dynamicHud != null) ? dynamicHud : HudDefaults.buildDefaultDynamicHud();
    }

    public boolean sanitize() {
        boolean changed = false;

        if (hudComponents == null) {
            hudComponents = HudDefaults.buildDefaultHudComponents();
            changed = true;
        } else {
            changed |= hudComponents.sanitize();
        }

        if (dynamicHud == null) {
            dynamicHud = HudDefaults.buildDefaultDynamicHud();
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
        cfg.setHudComponents(defaultHud != null ? defaultHud.copy() : HudDefaults.buildDefaultHudComponents());
        cfg.setDynamicHud(defaultDynamic != null ? defaultDynamic.copy() : HudDefaults.buildDefaultDynamicHud());
        return cfg;
    }
}