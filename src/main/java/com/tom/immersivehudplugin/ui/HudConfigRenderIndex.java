package com.tom.immersivehudplugin.ui;

import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class HudConfigRenderIndex {

    private record DynamicRuleRowKey(String componentKey, String host, DynamicHudTriggers trigger) {}
    private record DynamicComponentRowKey(String componentKey) {}

    private final Map<String, Integer> visibilitySectionRowIndexes = new HashMap<>();
    private final Map<String, Integer> visibilityRowIndexes = new HashMap<>();
    private final Map<DynamicRuleRowKey, Integer> dynamicRuleRowIndexes = new HashMap<>();
    private final Map<DynamicComponentRowKey, Integer> dynamicComponentRowIndexes = new HashMap<>();

    public void clearVisibilitySectionRowIndexes() {
        visibilitySectionRowIndexes.clear();
    }

    public void putVisibilitySectionRowIndex(
            @Nonnull HudComponentRegistry.Group group,
            int rowIndex
    ) {
        visibilitySectionRowIndexes.put(group.name(), rowIndex);
    }

    @Nullable
    public Integer getVisibilitySectionRowIndex(@Nonnull HudComponentRegistry.Group group) {
        return visibilitySectionRowIndexes.get(group.name());
    }

    public void clearVisibilityRowIndexes() {
        visibilityRowIndexes.clear();
    }

    public void putVisibilityRowIndex(@Nonnull String componentKey, int rowIndex) {
        visibilityRowIndexes.put(componentKey, rowIndex);
    }

    @Nullable
    public Integer getVisibilityRowIndex(@Nonnull String componentKey) {
        return visibilityRowIndexes.get(componentKey);
    }

    public void clearDynamicRuleRowIndexes() {
        dynamicRuleRowIndexes.clear();
    }

    public void putDynamicRuleRowIndex(
            @Nonnull String componentKey,
            @Nonnull String host,
            @Nonnull DynamicHudTriggers trigger,
            int rowIndex
    ) {
        dynamicRuleRowIndexes.put(new DynamicRuleRowKey(componentKey, host, trigger), rowIndex);
    }

    @Nullable
    public Integer getDynamicRuleRowIndex(
            @Nonnull String componentKey,
            @Nonnull String host,
            @Nonnull DynamicHudTriggers trigger
    ) {
        return dynamicRuleRowIndexes.get(new DynamicRuleRowKey(componentKey, host, trigger));
    }

    public void clearDynamicComponentRowIndexes() {
        dynamicComponentRowIndexes.clear();
    }

    public void putDynamicComponentRowIndex(@Nonnull String componentKey, int rowIndex) {
        dynamicComponentRowIndexes.put(new DynamicComponentRowKey(componentKey), rowIndex);
    }

    @Nullable
    public Integer getDynamicComponentRowIndex(@Nonnull String componentKey) {
        return dynamicComponentRowIndexes.get(new DynamicComponentRowKey(componentKey));
    }

    public void clearAll() {
        clearVisibilitySectionRowIndexes();
        clearVisibilityRowIndexes();
        clearDynamicRuleRowIndexes();
        clearDynamicComponentRowIndexes();
    }
}
