package com.tom.immersivehudplugin.visibility;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudEntry;
import com.tom.immersivehudplugin.registry.HudConfigAccess;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;
import com.tom.immersivehudplugin.rules.DynamicHudTriggersContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

import java.util.EnumSet;
import java.util.List;

public final class HudRuleEvaluator {

    private static final List<HudEntry> DYNAMIC_ENTRIES = HudComponentRegistry.dynamicList();

    public void rebuildDynamicHidden(
            PlayerHudState state,
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig,
            DynamicHudTriggersContext triggersContext
    ) {
        state.clearDynamicHidden();

        for (HudEntry entry : DYNAMIC_ENTRIES) {
            if (!HudConfigAccess.isHidden(entry, hudConfig)) {
                continue;
            }

            DynamicHudRuleConfig ruleConfig = HudConfigAccess.getDynamicRuleConfig(entry, dynamicConfig);
            boolean shouldShow = shouldShowDynamic(ruleConfig, triggersContext);

            if (!shouldShow) {
                state.addDynamicHidden(entry.hudComponent());
            }
        }
    }

    public boolean shouldShowDynamic(
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

    public boolean matchesTrigger(
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