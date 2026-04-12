package com.tom.immersivehudplugin.registry;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Function;

public record HudEntry(
        String key,
        String label,
        HudComponentRegistry.Group group,
        HudComponent hudComponent,
        String staticConfigKey,
        @Nullable String dynamicConfigKey,
        HudComponentRegistry.BoolGetter<HudComponentsConfig> staticGetter,
        HudComponentRegistry.BoolSetter<HudComponentsConfig> staticSetter,
        @Nullable Function<DynamicHudConfig, DynamicHudRuleConfig> dynamicGetter,
        boolean defaultHidden,
        EnumSet<DynamicHudTriggers> defaultRules,
        @Nullable Float defaultThreshold
) {
    public HudEntry {
        defaultRules = defaultRules == null
                ? EnumSet.noneOf(DynamicHudTriggers.class)
                : EnumSet.copyOf(defaultRules);

        if (defaultThreshold != null) {
            defaultThreshold = Math.max(0f, Math.min(100f, defaultThreshold));
        }
    }

    public boolean supportsDynamicRules() {
        return dynamicGetter != null && dynamicConfigKey != null;
    }

    public boolean supportsThreshold() {
        return defaultThreshold != null;
    }
}