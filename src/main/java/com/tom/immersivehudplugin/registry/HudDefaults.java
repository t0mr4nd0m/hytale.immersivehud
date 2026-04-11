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
            HudConfigAccess.setHidden(entry, cfg, entry.defaultHidden());
        }

        return cfg;
    }

    public static DynamicHudConfig buildDefaultDynamicHud() {
        DynamicHudConfig cfg = new DynamicHudConfig();

        for (HudComponentRegistry.HudEntry entry : HudComponentRegistry.dynamicList()) {

            DynamicHudRuleConfig ruleCfg = HudConfigAccess.getDynamicRuleConfig(entry, cfg);

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