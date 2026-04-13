package com.tom.immersivehudplugin.runtime.visibility;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;

import java.util.List;

public final class HudVisibilityCoordinator {

    private static final List<HudComponent> ALL_ENTRIES = HudComponentRegistry.allList();
    private static final List<HudComponent> DYNAMIC_ENTRIES = HudComponentRegistry.dynamicList();
    private static final List<HudComponent> STATIC_ENTRIES = ALL_ENTRIES.stream()
            .filter(entry -> !entry.supportsDynamicRules())
            .toList();

    private final HudRuleEvaluator hudRuleEvaluator = new HudRuleEvaluator();
    private final HudDeltaApplier hudDeltaApplier = new HudDeltaApplier();

    public boolean hasAnyDynamicHudEnabled(
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig
    ) {
        for (HudComponent entry : DYNAMIC_ENTRIES) {
            if (entry.isHidden(hudConfig) && dynamicConfig.hasRules(entry.key())) {
                return true;
            }
        }
        return false;
    }

    public void ensureStaticHudBuilt(PlayerHudState state, HudComponentsConfig hudConfig) {
        if (!state.staticHudInitialized || state.staticDirty) {
            rebuildStaticHidden(state, hudConfig);
        }
    }

    public void clearDynamicHiddenIfNeeded(PlayerHudState state) {
        if (state.hasDynamicHidden()) {
            state.clearDynamicHidden();
        }
    }

    public void rebuildStaticHidden(PlayerHudState state, HudComponentsConfig hudConfig) {
        state.clearStaticHidden();

        for (HudComponent entry : STATIC_ENTRIES) {
            if (entry.isHidden(hudConfig)) {
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
}