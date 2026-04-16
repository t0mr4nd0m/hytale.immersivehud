package com.tom.immersivehudplugin.runtime.visibility;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

import java.util.List;

public final class HudVisibilityCoordinator {

    private static final List<HudComponent> DYNAMIC_ENTRIES = HudComponentRegistry.dynamicList();
    private static final List<HudComponent> ALL_ENTRIES = HudComponentRegistry.allList();

    private final HudRuleEvaluator hudRuleEvaluator = new HudRuleEvaluator();
    private final HudDeltaApplier hudDeltaApplier = new HudDeltaApplier();

    public boolean hasAnyActiveDynamicRules(
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig
    ) {
        for (HudComponent entry : DYNAMIC_ENTRIES) {
            if (!entry.isHidden(hudConfig)) {
                continue;
            }

            if (hasActiveRules(entry, dynamicConfig)) {
                return true;
            }
        }
        return false;
    }

    public void ensureStaticHudBuilt(
            PlayerHudState state,
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig
    ) {
        if (state.staticHudInitialized && !state.staticDirty) {
            return;
        }

        rebuildStaticHidden(state, hudConfig, dynamicConfig);
    }

    public void clearDynamicHidden(PlayerHudState state) {
        if (state.hasDynamicHidden()) {
            state.clearDynamicHidden();
        }
    }

    public void rebuildStaticHidden(
            PlayerHudState state,
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig
    ) {
        state.clearStaticHidden();

        for (HudComponent entry : ALL_ENTRIES) {
            if (!entry.isHidden(hudConfig)) {
                continue;
            }

            if (!entry.supportsDynamicRules()) {
                state.addStaticHidden(entry.hudComponent());
                continue;
            }

            if (!hasActiveRules(entry, dynamicConfig)) {
                state.addStaticHidden(entry.hudComponent());
            }
        }

        state.staticHudInitialized = true;
        state.staticDirty = false;
    }

    public void rebuildDynamicHidden(
            PlayerHudState state,
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig,
            HudTriggerContext triggersContext
    ) {
        hudRuleEvaluator.rebuildDynamicHidden(state, hudConfig, dynamicConfig, triggersContext);
    }

    public void applyHudDelta(PlayerTickContext tickContext, PlayerHudState state) {
        hudDeltaApplier.apply(tickContext, state);
    }

    private boolean hasActiveRules(HudComponent entry, DynamicHudConfig dynamicConfig) {
        return dynamicConfig.hasRules(entry.key());
    }
}