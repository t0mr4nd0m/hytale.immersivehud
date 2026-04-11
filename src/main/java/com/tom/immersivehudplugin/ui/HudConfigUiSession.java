package com.tom.immersivehudplugin.ui;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudConfigAccess;
import com.tom.immersivehudplugin.registry.HudRuleCatalog;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HudConfigUiSession {

    private final Map<String, Boolean> moreTriggersExpandedByComponent = new HashMap<>();

    private boolean dirty;
    private HudConfigView currentView = HudConfigView.PROFILES;
    private Profile selectedProfile = Profile.DEFAULT;

    @Nullable
    private HudComponentRegistry.Group expandedVisibilityGroup = HudComponentRegistry.Group.CORE;

    private HudComponentsConfig draftHudComponents;
    private DynamicHudConfig draftDynamicHud;

    public HudConfigUiSession(@Nonnull PlayerConfig source) {
        this.draftHudComponents = source.getHudComponents().copy();
        this.draftDynamicHud = source.getDynamicHud().copy();
    }

    @SuppressWarnings("unused")
    public boolean isDirty() {
        return dirty;
    }

    public HudConfigView getCurrentView() {
        return currentView;
    }

    public void showProfilesView() {
        resetDynamicUiState();
        currentView = HudConfigView.PROFILES;
    }

    public void showVisibilityView() {
        resetDynamicUiState();
        currentView = HudConfigView.VISIBILITY;
    }

    public void showDynamicRulesView() {
        currentView = HudConfigView.DYNAMIC_RULES;
    }

    @Nonnull
    public Profile getSelectedProfile() {
        return selectedProfile;
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

    public HudComponentsConfig getDraftHudComponents() {
        return draftHudComponents;
    }

    public DynamicHudConfig getDraftDynamicHud() {
        return draftDynamicHud;
    }

    public boolean isHidden(@Nonnull HudComponentRegistry.HudEntry entry) {
        return HudConfigAccess.isHidden(entry, draftHudComponents);
    }

    public void toggleVisibility(@Nonnull String componentKey) {
        HudComponentRegistry.HudEntry entry = HudComponentRegistry.find(componentKey);
        if (entry == null) {
            return;
        }

        boolean hidden = HudConfigAccess.isHidden(entry, draftHudComponents);
        HudConfigAccess.setHidden(entry, draftHudComponents, !hidden);
        dirty = true;
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

    @Nonnull
    public List<HudComponentRegistry.HudEntry> getDynamicEntries() {
        return HudComponentRegistry.dynamicList();
    }

    @Nonnull
    public DynamicHudRuleConfig getDynamicRuleConfig(@Nonnull HudComponentRegistry.HudEntry entry) {
        DynamicHudRuleConfig cfg = HudConfigAccess.getDynamicRuleConfig(entry, draftDynamicHud);
        if (cfg == null) {
            throw new IllegalStateException("Entry is not dynamic-capable: " + entry.key());
        }
        return cfg;
    }

    public boolean isRuleEnabled(
            @Nonnull HudComponentRegistry.HudEntry entry,
            @Nonnull DynamicHudTriggers rule
    ) {
        return getDynamicRuleConfig(entry).getRules().contains(rule);
    }

    public void toggleRule(
            @Nonnull HudComponentRegistry.HudEntry entry,
            @Nonnull DynamicHudTriggers rule
    ) {
        DynamicHudRuleConfig cfg = getDynamicRuleConfig(entry);

        if (cfg.getRules().contains(rule)) {
            cfg.removeRule(rule);
        } else {
            cfg.addRule(rule);
        }

        dirty = true;
    }

    public float getDynamicThreshold(@Nonnull HudComponentRegistry.HudEntry entry) {
        return getDynamicRuleConfig(entry).getThreshold();
    }

    public void setDynamicThreshold(
            @Nonnull HudComponentRegistry.HudEntry entry,
            float value
    ) {
        getDynamicRuleConfig(entry).setThreshold(value);
        dirty = true;
    }

    public boolean isDynamicThresholdEnabled(@Nonnull HudComponentRegistry.HudEntry entry) {
        if (!entry.supportsThreshold()) {
            return false;
        }

        return switch (entry.key()) {
            case "health" -> isRuleEnabled(entry, DynamicHudTriggers.HEALTH_NOT_FULL);
            case "stamina" -> isRuleEnabled(entry, DynamicHudTriggers.STAMINA_NOT_FULL);
            case "mana" -> isRuleEnabled(entry, DynamicHudTriggers.MANA_NOT_FULL);
            case "oxygen" -> isRuleEnabled(entry, DynamicHudTriggers.OXYGEN_NOT_FULL);
            default -> false;
        };
    }

    public boolean isMoreTriggersRevealed(@Nonnull String componentKey) {
        return moreTriggersExpandedByComponent.getOrDefault(componentKey, false);
    }

    public void revealMoreTriggers(@Nonnull String componentKey) {
        moreTriggersExpandedByComponent.put(componentKey, true);
    }

    private void resetDynamicUiState() {
        moreTriggersExpandedByComponent.clear();
    }

    @Nonnull
    public List<DynamicHudTriggers> getBaseRulesInDisplayOrder(
            @Nonnull HudComponentRegistry.HudEntry entry
    ) {
        return DynamicHudTriggers.displayCategoryOrder().stream()
                .flatMap(category -> Arrays.stream(DynamicHudTriggers.values())
                        .filter(trigger -> trigger.category() == category)
                        .filter(trigger -> HudRuleCatalog.supportsRule(entry, trigger)))
                .toList();
    }

    @Nonnull
    public List<DynamicHudTriggers> getExtraRulesInDisplayOrder(
            @Nonnull HudComponentRegistry.HudEntry entry
    ) {
        return DynamicHudTriggers.displayCategoryOrder().stream()
                .flatMap(category -> Arrays.stream(DynamicHudTriggers.values())
                        .filter(trigger -> trigger.category() == category)
                        .filter(trigger -> !HudRuleCatalog.supportsRule(entry, trigger))
                        .filter(trigger -> trigger != DynamicHudTriggers.HEALTH_NOT_FULL)
                        .filter(trigger -> trigger != DynamicHudTriggers.STAMINA_NOT_FULL)
                        .filter(trigger -> trigger != DynamicHudTriggers.MANA_NOT_FULL)
                        .filter(trigger -> trigger != DynamicHudTriggers.OXYGEN_NOT_FULL))
                .toList();
    }

    public boolean isDynamicComponentVisible(@Nonnull HudComponentRegistry.HudEntry entry) {
        return !HudConfigAccess.isHidden(entry, draftHudComponents);
    }

    @Nonnull
    public String getDynamicComponentVisibilityLabel(@Nonnull HudComponentRegistry.HudEntry entry) {
        return isDynamicComponentVisible(entry) ? "<VISIBLE>" : "<HIDDEN>";
    }
}