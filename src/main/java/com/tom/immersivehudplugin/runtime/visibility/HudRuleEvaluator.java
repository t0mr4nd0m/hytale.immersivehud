package com.tom.immersivehudplugin.runtime.visibility;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.hud.trigger.HudTriggerContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;

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

        for (HudComponent component : DYNAMIC_ENTRIES) {
            if (!component.isHidden(hudConfig)) {
                continue;
            }

            if (!component.hasActiveRules(dynamicConfig)) {
                // Components without active rules are treated as static and handled
                // by HudVisibilityCoordinator (not here)
                continue;
            }

            DynamicHudRuleConfig ruleConfig = component.getDynamicRuleConfig(dynamicConfig);
            if  (ruleConfig == null) {
                continue;
            }

            if (!shouldShowDynamic(ruleConfig, triggersContext)) {
                state.addDynamicHidden(component.hudComponent());
            }
        }
    }

    private boolean shouldShowDynamic(
            DynamicHudRuleConfig ruleConfig,
            HudTriggerContext triggersContext
    ) {
        for (HudTrigger trigger : ruleConfig.getRules()) {
            if (trigger.matches(ruleConfig, triggersContext)) {
                return true;
            }
        }

        return false;
    }
}