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
        HudComponentRegistry.BoolGetter<HudComponentsConfig> staticGetter,
        HudComponentRegistry.BoolSetter<HudComponentsConfig> staticSetter,
        @Nullable Function<DynamicHudConfig, DynamicHudRuleConfig> dynamicGetter,
        boolean defaultHidden,
        EnumSet<HudTrigger> allowedRules,
        EnumSet<HudTrigger> defaultRules,
        @Nullable Float defaultThreshold
) {
    public HudComponent {
        allowedRules = allowedRules == null || allowedRules.isEmpty()
                ? EnumSet.noneOf(HudTrigger.class)
                : EnumSet.copyOf(allowedRules);

        defaultRules = defaultRules == null || defaultRules.isEmpty()
                ? EnumSet.noneOf(HudTrigger.class)
                : EnumSet.copyOf(defaultRules);

        if (!allowedRules.containsAll(defaultRules)) {
            throw new IllegalArgumentException(
                    "defaultRules must be a subset of allowedRules for component: " + key
            );
        }

        if (defaultThreshold != null && (defaultThreshold < 0f || defaultThreshold > 100f)) {
            throw new IllegalArgumentException(
                    "defaultThreshold must be between 0 and 100 for component: " + key
            );
        }

        long thresholdRuleCount = allowedRules.stream()
                .filter(HudTrigger::usesThreshold)
                .count();

        if (thresholdRuleCount > 1) {
            throw new IllegalArgumentException(
                    "Components cannot define more than one threshold rule: " + key
            );
        }

        boolean supportsThreshold = thresholdRuleCount == 1;

        if (supportsThreshold && defaultThreshold == null) {
            throw new IllegalArgumentException(
                    "Components with threshold rules must define defaultThreshold: " + key
            );
        }

        if (!supportsThreshold && defaultThreshold != null) {
            throw new IllegalArgumentException(
                    "defaultThreshold is only valid for components with threshold rules: " + key
            );
        }
    }

    public boolean supportsDynamicRules() {
        return dynamicGetter != null;
    }

    public boolean supportsThreshold() {
        return thresholdRule() != null;
    }

    @Nullable
    public HudTrigger thresholdRule() {
        return allowedRules.stream()
                .filter(HudTrigger::usesThreshold)
                .findFirst()
                .orElse(null);
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

    public DynamicHudRuleConfig getDynamicRuleConfig(DynamicHudConfig dynamicConfig) {
        return (dynamicGetter == null)
                ? DynamicHudRuleConfig.empty()
                : dynamicGetter.apply(dynamicConfig);
    }

    public boolean hasActiveRules(DynamicHudConfig cfg) {
        return getDynamicRuleConfig(cfg).hasRules();
    }
}