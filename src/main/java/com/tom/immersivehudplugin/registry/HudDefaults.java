package com.tom.immersivehudplugin.registry;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;

public final class HudDefaults {

    private HudDefaults() {
    }

    public static HudComponentsConfig buildDefaultHudComponents() {
        HudComponentsConfig cfg = new HudComponentsConfig();

        for (HudComponentRegistry.HudEntry entry : HudComponentRegistry.allList()) {
            entry.staticSetter().set(cfg, entry.defaultHidden());
        }

        return cfg;
    }

    public static DynamicHudConfig buildDefaultDynamicHud() {
        DynamicHudConfig cfg = new DynamicHudConfig();

        for (HudComponentRegistry.HudEntry entry : HudComponentRegistry.dynamicList()) {
            DynamicHudRuleConfig ruleCfg = entry.dynamicGetter() != null
                    ? entry.dynamicGetter().apply(cfg)
                    : null;

            if (ruleCfg != null) {
                ruleCfg.setRules(EnumSet.copyOf(entry.defaultRules()));
            }

            if (entry.defaultThreshold() != null && ruleCfg != null) {
                ruleCfg.setThreshold(entry.defaultThreshold());
            }
        }

        return cfg;
    }
}