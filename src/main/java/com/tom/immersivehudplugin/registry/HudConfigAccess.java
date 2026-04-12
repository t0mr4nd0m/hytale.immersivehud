package com.tom.immersivehudplugin.registry;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HudConfigAccess {

    private HudConfigAccess() {
    }

    public static boolean isHidden(
            @Nonnull HudEntry entry,
            @Nonnull HudComponentsConfig hudConfig
    ) {
        return entry.staticGetter().get(hudConfig);
    }

    public static void setHidden(
            @Nonnull HudEntry entry,
            @Nonnull HudComponentsConfig hudConfig,
            boolean hidden
    ) {
        entry.staticSetter().set(hudConfig, hidden);
    }

    @Nullable
    public static DynamicHudRuleConfig getDynamicRuleConfig(
            @Nonnull HudEntry entry,
            @Nonnull DynamicHudConfig dynamicConfig
    ) {
        return entry.dynamicGetter() != null
                ? entry.dynamicGetter().apply(dynamicConfig)
                : null;
    }

    public static boolean supportsDynamicRules(@Nonnull HudEntry entry) {
        return entry.dynamicGetter() != null && entry.dynamicConfigKey() != null;
    }
}