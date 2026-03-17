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

        long requiredMask = 0L;

        for (ManagedHudRuntimeDef def : MANAGED_HUD_COMPONENTS) {
            if (!Boolean.TRUE.equals(def.hideFlag().apply(hc))) {
                continue;
            }

            DynamicHudRuleConfig rule = def.ruleGetter().apply(dh);

            if (rule != null && !rule.isAlwaysHidden()) {
                requiredMask |= rule.getRulesMask();
            }
        }

        long activeMask = DynamicHudTriggers.activeMask(dyn, requiredMask);

        for (ManagedHudRuntimeDef def : MANAGED_HUD_COMPONENTS) {
            if (!Boolean.TRUE.equals(def.hideFlag().apply(hc))) {
                continue;
            }

            DynamicHudRuleConfig rule = def.ruleGetter().apply(dh);

            boolean hidden = rule == null
                    || rule.isAlwaysHidden()
                    || !shouldShowDynamic(rule, activeMask);

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
            DynamicHudRuleConfig rule,
            long activeMask
    ) {
        if (rule == null) {
            return false;
        }

        return (rule.getRulesMask() & activeMask) != 0L;
    }
}