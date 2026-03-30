package com.tom.immersivehudplugin.visibility;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;
import com.tom.immersivehudplugin.rules.DynamicHudTriggersContext;
import com.tom.immersivehudplugin.runtime.PlayerHudState;
import com.tom.immersivehudplugin.runtime.PlayerTickContext;

import java.util.EnumSet;
import java.util.function.Function;

public final class HudVisibilityService {

    private record ManagedHudRuntimeDef(
            HudComponent component,
            Function<HudComponentsConfig, Boolean> hideFlag,
            Function<DynamicHudConfig, DynamicHudRuleConfig> ruleGetter
    ) {}

    private record StaticHudRuntimeRule(
            HudComponent component,
            Function<HudComponentsConfig, Boolean> hiddenFlag
    ) {}

    private static final ManagedHudRuntimeDef[] MANAGED_HUD_COMPONENTS =
            HudComponentRegistry.all().values().stream()
                    .filter(HudComponentRegistry.HudEntry::supportsDynamicRules)
                    .map(entry -> new ManagedHudRuntimeDef(
                            entry.hudComponent(),
                            hc -> entry.staticGetter().get(hc),
                            entry.dynamicGetter()
                    ))
                    .toArray(ManagedHudRuntimeDef[]::new);

    private static final StaticHudRuntimeRule[] STATIC_HUD_RULES =
            HudComponentRegistry.all().values().stream()
                    .filter(entry -> !entry.supportsDynamicRules())
                    .map(entry -> new StaticHudRuntimeRule(
                            entry.hudComponent(),
                            hc -> entry.staticGetter().get(hc)
                    ))
                    .toArray(StaticHudRuntimeRule[]::new);

    public boolean hasAnyDynamicHudEnabled(HudComponentsConfig hc) {
        for (ManagedHudRuntimeDef def : MANAGED_HUD_COMPONENTS) {
            if (Boolean.TRUE.equals(def.hideFlag().apply(hc))) {
                return true;
            }
        }
        return false;
    }

    public void ensureStaticHudBuilt(PlayerHudState st, HudComponentsConfig hc) {
        if (!st.staticHudInitialized || st.staticDirty) {
            rebuildStaticHidden(st, hc);
        }
    }

    public void clearDynamicHiddenIfNeeded(PlayerHudState st) {
        if (!st.dynamicHidden.isEmpty()) {
            st.dynamicHidden.clear();
        }
    }

    public void rebuildStaticHidden(PlayerHudState st, HudComponentsConfig hc) {
        st.staticHidden.clear();

        for (StaticHudRuntimeRule rule : STATIC_HUD_RULES) {
            if (Boolean.TRUE.equals(rule.hiddenFlag().apply(hc))) {
                st.staticHidden.add(rule.component());
            }
        }

        st.staticHudInitialized = true;
        st.staticDirty = false;
    }

    public void rebuildDynamicHidden(
            PlayerHudState st,
            HudComponentsConfig hc,
            DynamicHudConfig dh,
            DynamicHudTriggersContext dyn
    ) {
        st.dynamicHidden.clear();

        for (ManagedHudRuntimeDef def : MANAGED_HUD_COMPONENTS) {
            if (!Boolean.TRUE.equals(def.hideFlag().apply(hc))) {
                continue;
            }

            DynamicHudRuleConfig ruleCfg = def.ruleGetter().apply(dh);
            boolean hidden = !shouldShowDynamic(ruleCfg, dyn);

            if (hidden) {
                st.dynamicHidden.add(def.component());
            }
        }
    }

    public void applyHudDelta(PlayerTickContext ctx, PlayerHudState st) {
        EnumSet<HudComponent> effectiveHidden = st.tempHidden;
        effectiveHidden.clear();
        effectiveHidden.addAll(st.staticHidden);
        effectiveHidden.addAll(st.dynamicHidden);

        if (effectiveHidden.equals(st.lastAppliedHidden)) {
            return;
        }

        EnumSet<HudComponent> toHide = st.tempToHide;
        toHide.clear();
        toHide.addAll(effectiveHidden);
        toHide.removeAll(st.lastAppliedHidden);

        EnumSet<HudComponent> toShow = st.tempToShow;
        toShow.clear();
        toShow.addAll(st.lastAppliedHidden);
        toShow.removeAll(effectiveHidden);

        if (!toHide.isEmpty()) {
            ctx.player().getHudManager().hideHudComponents(
                    ctx.playerRef(),
                    toHide.toArray(HudComponent[]::new)
            );
        }

        if (!toShow.isEmpty()) {
            ctx.player().getHudManager().showHudComponents(
                    ctx.playerRef(),
                    toShow.toArray(HudComponent[]::new)
            );
        }

        st.lastAppliedHidden.clear();
        st.lastAppliedHidden.addAll(effectiveHidden);
    }

    private boolean shouldShowDynamic(
            DynamicHudRuleConfig ruleCfg,
            DynamicHudTriggersContext ctx
    ) {
        if (ruleCfg == null) {
            return false;
        }

        EnumSet<DynamicHudTriggers> rules = ruleCfg.getRules();
        if (rules.isEmpty()) {
            return false;
        }

        for (DynamicHudTriggers trigger : rules) {
            if (matchesTrigger(trigger, ruleCfg, ctx)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesTrigger(
            DynamicHudTriggers trigger,
            DynamicHudRuleConfig ruleCfg,
            DynamicHudTriggersContext ctx
    ) {
        if (isThresholdRule(trigger)) {
            return matchesThresholdTrigger(trigger, ruleCfg, ctx);
        }

        return trigger.test(ctx);
    }

    private boolean isThresholdRule(DynamicHudTriggers trigger) {
        return trigger == DynamicHudTriggers.HEALTH_NOT_FULL
                || trigger == DynamicHudTriggers.STAMINA_NOT_FULL
                || trigger == DynamicHudTriggers.MANA_NOT_FULL
                || trigger == DynamicHudTriggers.OXYGEN_NOT_FULL;
    }

    private boolean matchesThresholdTrigger(
            DynamicHudTriggers trigger,
            DynamicHudRuleConfig ruleCfg,
            DynamicHudTriggersContext ctx
    ) {
        float threshold = ruleCfg != null ? ruleCfg.getThreshold() : 100f;

        return switch (trigger) {
            case HEALTH_NOT_FULL -> ctx.healthBar() != null && ctx.healthBar().isBelowPercent(threshold);
            case STAMINA_NOT_FULL -> ctx.staminaBar() != null && ctx.staminaBar().isBelowPercent(threshold);
            case MANA_NOT_FULL -> ctx.manaBar() != null && ctx.manaBar().isBelowPercent(threshold);
            case OXYGEN_NOT_FULL -> ctx.oxygenBar() != null && ctx.oxygenBar().isBelowPercent(threshold);
            default -> false;
        };
    }
}