package com.tom.immersivehudplugin.hud.component;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.function.Function;

public record HudComponent(
        String key,
        String label,
        HudComponentRegistry.Group group,
        com.hypixel.hytale.protocol.packets.interface_.HudComponent hudComponent,
        String staticConfigKey,
        @Nullable String dynamicConfigKey,
        HudComponentRegistry.BoolGetter<HudComponentsConfig> staticGetter,
        HudComponentRegistry.BoolSetter<HudComponentsConfig> staticSetter,
        @Nullable Function<DynamicHudConfig, DynamicHudRuleConfig> dynamicGetter,
        boolean defaultHidden,
        EnumSet<HudTrigger> allowedRules,
        EnumSet<HudTrigger> defaultRules,
        @Nullable Float defaultThreshold
) {
    public HudComponent {
        allowedRules = allowedRules == null
                ? EnumSet.noneOf(HudTrigger.class)
                : EnumSet.copyOf(allowedRules);

        defaultRules = defaultRules == null
                ? EnumSet.noneOf(HudTrigger.class)
                : EnumSet.copyOf(defaultRules);

        if (!allowedRules.containsAll(defaultRules)) {
            throw new IllegalArgumentException(
                    "defaultRules must be a subset of allowedRules for component: " + key
            );
        }

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

    public boolean supportsRule(@Nullable HudTrigger rule) {
        return rule != null && allowedRules.contains(rule);
    }

    public boolean isHidden(HudComponentsConfig hudConfig) {
        return staticGetter.get(hudConfig);
    }

    public void setHidden(HudComponentsConfig hudConfig, boolean hidden) {
        staticSetter.set(hudConfig, hidden);
    }

    @Nullable
    public DynamicHudRuleConfig getDynamicRuleConfig(DynamicHudConfig dynamicConfig) {
        return dynamicGetter != null ? dynamicGetter.apply(dynamicConfig) : null;
    }
}