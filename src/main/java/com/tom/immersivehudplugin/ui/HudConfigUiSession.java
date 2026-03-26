package com.tom.immersivehudplugin.ui;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggerCategory;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HudConfigUiSession {

    public HudConfigUiSession(@Nonnull PlayerConfig source) {
        this.draftHudComponents = source.getHudComponents().copy();
        this.draftDynamicHud = source.getDynamicHud().copy();
    }

    private boolean dirty;
    @SuppressWarnings("unused")
    public boolean isDirty() {
        return dirty;
    }

    // VIEWS =====================================================

    private HudConfigView currentView = HudConfigView.PROFILES;

    public HudConfigView getCurrentView() {
        return currentView;
    }

    public void showProfilesView() {
        currentView = HudConfigView.PROFILES;
    }

    public void showVisibilityView() {
        currentView = HudConfigView.VISIBILITY;
    }

    public void showDynamicRulesView() {
        currentView = HudConfigView.DYNAMIC_RULES;
    }

    // PROFILES =====================================================

    private Profile selectedProfile = Profile.DEFAULT;

    @Nonnull
    public Profile getSelectedProfile() {
        this.selectedProfile = resolveCurrentProfile();
        return this.selectedProfile;
    }

    public void selectProfile(@Nonnull Profile profile) {
        PlayerConfig temp = new PlayerConfig();
        temp.setHudComponents(draftHudComponents);
        temp.setDynamicHud(draftDynamicHud);

        ProfilePresets.applyTo(temp, profile);

        this.draftHudComponents = temp.getHudComponents();
        this.draftDynamicHud = temp.getDynamicHud();
        this.selectedProfile = profile;
        this.dirty = true;
    }

    @Nonnull
    private Profile resolveCurrentProfile() {
        for (Profile profile : Profile.values()) {
            if (profile == Profile.CUSTOM) {
                continue;
            }

            if (matchesProfile(profile)) {
                return profile;
            }
        }

        return Profile.CUSTOM;
    }

    private boolean matchesProfile(@Nonnull Profile profile) {
        PlayerConfig temp = new PlayerConfig();
        ProfilePresets.applyTo(temp, profile);

        HudComponentsConfig presetHudComponents = temp.getHudComponents();
        DynamicHudConfig presetDynamicHud = temp.getDynamicHud();

        return hudComponentsEqual(draftHudComponents, presetHudComponents)
                && dynamicHudEqual(draftDynamicHud, presetDynamicHud);
    }

    private boolean hudComponentsEqual(
            @Nonnull HudComponentsConfig a,
            @Nonnull HudComponentsConfig b
    ) {
        for (HudComponentRegistry.HudEntry entry : HudComponentRegistry.allList()) {
            boolean aHidden = entry.staticGetter().get(a);
            boolean bHidden = entry.staticGetter().get(b);

            if (aHidden != bHidden) {
                return false;
            }
        }

        return true;
    }

    private boolean dynamicHudEqual(
            @Nonnull DynamicHudConfig a,
            @Nonnull DynamicHudConfig b
    ) {
        for (HudComponentRegistry.HudEntry entry : HudComponentRegistry.allList()) {
            if (!entry.supportsDynamicRules() || entry.dynamicGetter() == null) {
                continue;
            }

            DynamicHudRuleConfig aRuleConfig = entry.dynamicGetter().apply(a);
            DynamicHudRuleConfig bRuleConfig = entry.dynamicGetter().apply(b);

            if (!aRuleConfig.getRules().equals(bRuleConfig.getRules())) {
                return false;
            }
        }

        return true;
    }

    // HUD COMPONENTS VISIBILITY  =====================================================

    private final Map<String, Integer> visibilitySectionRowIndexes = new HashMap<>();

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

    private final Map<String, Integer> visibilityRowIndexes = new HashMap<>();

    @Nullable
    private HudComponentRegistry.Group expandedVisibilityGroup = HudComponentRegistry.Group.CORE;

    private HudComponentsConfig draftHudComponents;

    public HudComponentsConfig getDraftHudComponents() {
        return draftHudComponents;
    }

    public boolean isHidden(@Nonnull HudComponentRegistry.HudEntry entry) {
        return entry.staticGetter().get(draftHudComponents);
    }

    public int getGroupTotal(@Nonnull HudComponentRegistry.Group group) {
        return (int) HudComponentRegistry.allList().stream()
                .filter(entry -> entry.group() == group)
                .count();
    }

    public int getGroupHiddenCount(@Nonnull HudComponentRegistry.Group group) {
        return (int) HudComponentRegistry.allList().stream()
                .filter(entry -> entry.group() == group)
                .filter(this::isHidden)
                .count();
    }

    @Nonnull
    public String getVisibilityGroupCounterLabel(@Nonnull HudComponentRegistry.Group group) {
        int hidden = getGroupHiddenCount(group);
        int total = getGroupTotal(group);
        int shown = total - hidden;
        return "[" + shown + "/" + total + "]";
    }

    public void toggleVisibility(@Nonnull String componentKey) {
        HudComponentRegistry.HudEntry entry = HudComponentRegistry.find(componentKey);
        if (entry == null) {
            return;
        }

        boolean hidden = entry.staticGetter().get(draftHudComponents);
        entry.staticSetter().set(draftHudComponents, !hidden);
        dirty = true;
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

    @Nullable
    public HudComponentRegistry.Group getExpandedVisibilityGroup() {
        return expandedVisibilityGroup;
    }

    public void toggleVisibilityGroup(@Nonnull HudComponentRegistry.Group group) {
        if (expandedVisibilityGroup == group) {
            expandedVisibilityGroup = null;
        } else {
            expandedVisibilityGroup = group;
        }
    }

    // DYNAMIC RULES =====================================================

    private final Map<String, Integer> dynamicCategoryRowIndexes = new HashMap<>();

    public void clearDynamicCategoryRowIndexes() {
        dynamicCategoryRowIndexes.clear();
    }

    public void putDynamicCategoryRowIndex(
            @Nonnull DynamicHudTriggerCategory category,
            int rowIndex
    ) {
        dynamicCategoryRowIndexes.put(category.name(), rowIndex);
    }

    @Nullable
    public Integer getDynamicCategoryRowIndex(@Nonnull DynamicHudTriggerCategory category) {
        return dynamicCategoryRowIndexes.get(category.name());
    }

    @Nullable
    private DynamicHudTriggerCategory expandedDynamicCategory = null;

    public void collapseAllDynamicCategories() {
        this.expandedDynamicCategory = null;
    }

    private DynamicHudConfig draftDynamicHud;

    private int dynamicComponentIndex = 0;

    public DynamicHudConfig getDraftDynamicHud() {
        return draftDynamicHud;
    }

    public HudComponentRegistry.HudEntry currentDynamicEntry() {

        List<HudComponentRegistry.HudEntry> all = HudComponentRegistry.dynamicList();
        if (all.isEmpty()) {
            throw new IllegalStateException("No dynamic HUD entries found");
        }

        dynamicComponentIndex = clamp(dynamicComponentIndex, 0, all.size() - 1);
        return all.get(dynamicComponentIndex);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public void nextDynamicEntry() {
        List<HudComponentRegistry.HudEntry> all = HudComponentRegistry.dynamicList();
        if (!all.isEmpty()) {
            dynamicComponentIndex = (dynamicComponentIndex + 1) % all.size();
            collapseAllDynamicCategories();
        }
    }

    public void previousDynamicEntry() {
        List<HudComponentRegistry.HudEntry> all = HudComponentRegistry.dynamicList();
        if (!all.isEmpty()) {
            dynamicComponentIndex = (dynamicComponentIndex - 1 + all.size()) % all.size();
            collapseAllDynamicCategories();
        }
    }

    public DynamicHudRuleConfig currentDynamicRuleConfig() {
        HudComponentRegistry.HudEntry entry = currentDynamicEntry();
        if (!entry.supportsDynamicRules() || entry.dynamicGetter() == null) {
            throw new IllegalStateException("Current dynamic entry is not dynamic-capable");
        }
        return entry.dynamicGetter().apply(draftDynamicHud);
    }

    private final Map<String, Integer> dynamicRuleRowIndexes = new HashMap<>();

    public void clearDynamicRuleRowIndexes() {
        dynamicRuleRowIndexes.clear();
    }

    public void putDynamicRuleRowIndex(@Nonnull DynamicHudTriggers trigger, int rowIndex) {
        dynamicRuleRowIndexes.put(trigger.name(), rowIndex);
    }

    @Nullable
    public Integer getDynamicRuleRowIndex(@Nonnull DynamicHudTriggers trigger) {
        return dynamicRuleRowIndexes.get(trigger.name());
    }

    public void toggleRule(@Nonnull DynamicHudTriggers rule) {
        DynamicHudRuleConfig cfg = currentDynamicRuleConfig();
        if (cfg.getRules().contains(rule)) {
            cfg.removeRule(rule);
        } else {
            cfg.addRule(rule);
        }

        dirty = true;
    }

    public boolean isRuleEnabled(@Nonnull DynamicHudTriggers rule) {
        return currentDynamicRuleConfig().getRules().contains(rule);
    }

    @Nullable
    public DynamicHudTriggerCategory getExpandedDynamicCategory() {
        return expandedDynamicCategory;
    }

    public void toggleDynamicCategory(@Nonnull DynamicHudTriggerCategory category) {
        if (expandedDynamicCategory == category) {
            expandedDynamicCategory = null;
        } else {
            expandedDynamicCategory = category;
        }
    }

    public int getDynamicCategoryTotal(@Nonnull DynamicHudTriggerCategory category) {
        return (int) Arrays.stream(DynamicHudTriggers.values())
                .filter(trigger -> trigger.category() == category)
                .count();
    }

    public int getDynamicCategoryEnabledCount(@Nonnull DynamicHudTriggerCategory category) {
        return (int) currentDynamicRuleConfig().getRules().stream()
                .filter(trigger -> trigger.category() == category)
                .count();
    }

    @Nonnull
    public String getDynamicCategoryCounterLabel(@Nonnull DynamicHudTriggerCategory category) {
        int enabled = getDynamicCategoryEnabledCount(category);
        int total = getDynamicCategoryTotal(category);
        return "[" + enabled + "/" + total + "]";
    }

    @Nonnull
    public List<DynamicHudTriggers> getCurrentSelectedRulesInDisplayOrder() {
        DynamicHudRuleConfig cfg = currentDynamicRuleConfig();

        return DynamicHudTriggers.displayCategoryOrder().stream()
                .flatMap(category -> Arrays.stream(DynamicHudTriggers.values())
                        .filter(trigger -> trigger.category() == category))
                .filter(cfg.getRules()::contains)
                .toList();
    }
}