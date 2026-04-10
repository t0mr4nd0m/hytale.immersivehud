package com.tom.immersivehudplugin.visibility;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudComponentRegistry.HudEntry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;
import com.tom.immersivehudplugin.rules.DynamicHudTriggersContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.context.PlayerTickContext;

import java.util.EnumSet;
import java.util.List;

public final class HudVisibilityService {

    private static final List<HudEntry> ALL_ENTRIES = HudComponentRegistry.allList();
    private static final List<HudEntry> DYNAMIC_ENTRIES = HudComponentRegistry.dynamicList();
    private static final List<HudEntry> STATIC_ENTRIES = ALL_ENTRIES.stream()
            .filter(entry -> !entry.supportsDynamicRules())
            .toList();

    private final HudDeltaApplier hudDeltaApplier = new HudDeltaApplier();

    public boolean hasAnyDynamicHudEnabled(HudComponentsConfig hudConfig) {
        for (HudEntry entry : DYNAMIC_ENTRIES) {
            if (entry.staticGetter().get(hudConfig)) {
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
            if (entry.staticGetter().get(hudConfig)) {
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
        state.clearDynamicHidden();

        for (HudEntry entry : DYNAMIC_ENTRIES) {
            if (!entry.staticGetter().get(hudConfig)) {
                continue;
            }

            DynamicHudRuleConfig ruleConfig = entry.dynamicGetter().apply(dynamicConfig);
            boolean shouldShow = shouldShowDynamic(ruleConfig, triggersContext);

            if (!shouldShow) {
                state.addDynamicHidden(entry.hudComponent());
            }
        }
    }

    public void applyHudDelta(PlayerTickContext tickContext, PlayerHudState state) {
        hudDeltaApplier.apply(tickContext, state);
    }

    private boolean shouldShowDynamic(
            DynamicHudRuleConfig ruleConfig,
            DynamicHudTriggersContext triggersContext
    ) {
        if (ruleConfig == null) {
            return false;
        }

        EnumSet<DynamicHudTriggers> rules = ruleConfig.getRules();
        if (rules.isEmpty()) {
            return false;
        }

        for (DynamicHudTriggers trigger : rules) {
            if (matchesTrigger(trigger, ruleConfig, triggersContext)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesTrigger(
            DynamicHudTriggers trigger,
            DynamicHudRuleConfig ruleConfig,
            DynamicHudTriggersContext triggersContext
    ) {
        return switch (trigger) {
            case HEALTH_NOT_FULL ->
                    triggersContext.healthBar() != null
                            && triggersContext.healthBar().isBelowPercent(ruleConfig.getThreshold());

            case STAMINA_NOT_FULL ->
                    triggersContext.staminaBar() != null
                            && triggersContext.staminaBar().isBelowPercent(ruleConfig.getThreshold());

            case MANA_NOT_FULL ->
                    triggersContext.manaBar() != null
                            && triggersContext.manaBar().isBelowPercent(ruleConfig.getThreshold());

            case OXYGEN_NOT_FULL ->
                    triggersContext.oxygenBar() != null
                            && triggersContext.oxygenBar().isBelowPercent(ruleConfig.getThreshold());

            default -> trigger.test(triggersContext);
        };
    }
}