package com.tom.immersivehudplugin.visibility;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.context.PlayerTickContext;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudComponentRegistry.HudEntry;
import com.tom.immersivehudplugin.registry.HudConfigAccess;
import com.tom.immersivehudplugin.rules.DynamicHudTriggersContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

import java.util.List;

public final class HudVisibilityService {

    private static final List<HudEntry> ALL_ENTRIES = HudComponentRegistry.allList();
    private static final List<HudEntry> DYNAMIC_ENTRIES = HudComponentRegistry.dynamicList();
    private static final List<HudEntry> STATIC_ENTRIES = ALL_ENTRIES.stream()
            .filter(entry -> !entry.supportsDynamicRules())
            .toList();

    private final HudRuleEvaluator hudRuleEvaluator = new HudRuleEvaluator();
    private final HudDeltaApplier hudDeltaApplier = new HudDeltaApplier();

    public boolean hasAnyDynamicHudEnabled(HudComponentsConfig hudConfig) {
        for (HudEntry entry : DYNAMIC_ENTRIES) {
            if (HudConfigAccess.isHidden(entry, hudConfig)) {
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

        for (HudEntry entry : STATIC_ENTRIES) {
            if (HudConfigAccess.isHidden(entry, hudConfig)) {
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
            DynamicHudTriggersContext triggersContext
    ) {
        hudRuleEvaluator.rebuildDynamicHidden(state, hudConfig, dynamicConfig, triggersContext);
    }

    public void applyHudDelta(PlayerTickContext tickContext, PlayerHudState state) {
        hudDeltaApplier.apply(tickContext, state);
    }
}