package com.tom.immersivehudplugin.runtime.visibility;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

import java.util.EnumSet;
import java.util.List;

public final class HudRuleEvaluator {

    private static final List<HudComponent> DYNAMIC_ENTRIES = HudComponentRegistry.dynamicList();

    public void rebuildDynamicHidden(
            PlayerHudState state,
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig,
            HudTriggerContext triggersContext
    ) {
        state.clearDynamicHidden();

        for (HudComponent entry : DYNAMIC_ENTRIES) {
            if (!entry.isHidden(hudConfig)) { continue; }

            DynamicHudRuleConfig ruleConfig = entry.getDynamicRuleConfig(dynamicConfig);
            boolean shouldShow = shouldShowDynamic(ruleConfig, triggersContext);

            if (!shouldShow) {
                state.addDynamicHidden(entry.hudComponent());
            }
        }
    }

    public boolean shouldShowDynamic(
            DynamicHudRuleConfig ruleConfig,
            HudTriggerContext triggersContext
    ) {
        if (ruleConfig == null) { return false; }

        EnumSet<HudTrigger> rules = ruleConfig.getRules();
        if (rules.isEmpty()) { return false; }

        for (HudTrigger trigger : rules) {
            if (matchesTrigger(trigger, ruleConfig, triggersContext)) {
                return true;
            }
        }

        return false;
    }

    public boolean matchesTrigger(
            HudTrigger trigger,
            DynamicHudRuleConfig ruleConfig,
            HudTriggerContext triggersContext
    ) {
        return switch (trigger) {
            case HEALTH_NOT_FULL ->
                    triggersContext.healthBar().isBelowPercent(ruleConfig.getThreshold());

            case STAMINA_NOT_FULL ->
                    triggersContext.staminaBar().isBelowPercent(ruleConfig.getThreshold());

            case MANA_NOT_FULL ->
                    triggersContext.manaBar().isBelowPercent(ruleConfig.getThreshold());

            case OXYGEN_NOT_FULL ->
                    triggersContext.oxygenBar().isBelowPercent(ruleConfig.getThreshold());

            default -> trigger.matchesSignal(triggersContext);
        };
    }
}