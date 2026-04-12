package com.tom.immersivehudplugin.registry;

import com.tom.immersivehudplugin.rules.DynamicHudTriggerCategory;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;
import com.tom.immersivehudplugin.rules.HudRuleDefinitions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public final class HudRuleCatalog {

    private static final Map<DynamicHudTriggers, HudRuleDefinitions.RuleMeta> META_BY_TRIGGER;
    private static final List<DynamicHudTriggerCategory> GROUP_ORDER;
    private static final List<DynamicHudTriggers> ALL_RULES;

    static {
        META_BY_TRIGGER = HudRuleDefinitions.byTrigger();
        GROUP_ORDER = List.copyOf(HudRuleDefinitions.groupOrder());
        ALL_RULES = HudRuleDefinitions.all().stream()
                .map(HudRuleDefinitions.RuleMeta::trigger)
                .toList();
    }

    private HudRuleCatalog() {
    }

    @Nonnull
    public static EnumSet<DynamicHudTriggers> getAllowedRules(@Nonnull HudEntry entry) {
        return entry.allowedRulesCopy();
    }

    public static boolean supportsRule(
            @Nonnull HudEntry entry,
            @Nonnull DynamicHudTriggers rule
    ) {
        return entry.allowedRules().contains(rule);
    }

    @Nonnull
    public static List<DynamicHudTriggers> allRules() {
        return ALL_RULES;
    }

    @Nonnull
    public static List<DynamicHudTriggerCategory> groupOrder() {
        return GROUP_ORDER;
    }

    @Nullable
    public static HudRuleDefinitions.RuleMeta findMeta(@Nullable DynamicHudTriggers trigger) {
        return trigger == null ? null : META_BY_TRIGGER.get(trigger);
    }

    @Nullable
    public static DynamicHudTriggerCategory getGroup(@Nullable DynamicHudTriggers trigger) {
        HudRuleDefinitions.RuleMeta meta = findMeta(trigger);
        return meta != null ? meta.category() : null;
    }

    @Nonnull
    public static String getLabel(@Nonnull DynamicHudTriggers trigger) {
        HudRuleDefinitions.RuleMeta meta = META_BY_TRIGGER.get(trigger);
        return meta != null ? meta.label() : DynamicHudTriggers.prettyName(trigger);
    }

    @Nonnull
    public static List<DynamicHudTriggers> getRulesForGroup(
            @Nonnull DynamicHudTriggerCategory category
    ) {
        return HudRuleDefinitions.all().stream()
                .filter(meta -> meta.category() == category)
                .map(HudRuleDefinitions.RuleMeta::trigger)
                .toList();
    }
}