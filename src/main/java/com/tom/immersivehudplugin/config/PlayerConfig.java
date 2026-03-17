package com.tom.immersivehudplugin.config;

public final class PlayerConfig {

    private HudComponentsConfig hudComponents = new HudComponentsConfig();
    private DynamicHudConfig dynamicHud = new DynamicHudConfig();

    public HudComponentsConfig getHudComponents() {
        return hudComponents;
    }

    public void setHudComponents(HudComponentsConfig hudComponents) {
        this.hudComponents = (hudComponents != null) ? hudComponents : new HudComponentsConfig();
    }

    public DynamicHudConfig getDynamicHud() {
        return dynamicHud;
    }

    public void setDynamicHud(DynamicHudConfig dynamicHud) {
        this.dynamicHud = (dynamicHud != null) ? dynamicHud : new DynamicHudConfig();
    }

    public boolean sanitize() {
        boolean changed = false;

        if (hudComponents == null) {
            hudComponents = new HudComponentsConfig();
            changed = true;
        } else {
            changed |= hudComponents.sanitize();
        }

        if (dynamicHud == null) {
            dynamicHud = new DynamicHudConfig();
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
        cfg.setHudComponents(defaultHud != null ? defaultHud.copy() : new HudComponentsConfig());
        cfg.setDynamicHud(defaultDynamic != null ? defaultDynamic.copy() : new DynamicHudConfig());
        return cfg;
    }
}