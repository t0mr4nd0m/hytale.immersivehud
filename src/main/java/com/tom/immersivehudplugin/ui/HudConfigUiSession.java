package com.tom.immersivehudplugin.ui;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public final class HudConfigUiSession {

    private boolean dirty;
    private HudConfigView currentView = HudConfigView.PROFILES;

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
        currentView = HudConfigView.PROFILES;
    }

    public void showVisibilityView() {
        currentView = HudConfigView.VISIBILITY;
    }

    public void showDynamicRulesView() {
        currentView = HudConfigView.DYNAMIC_RULES;
    }

    public void selectProfile(@Nonnull Profile profile) {
        PlayerConfig temp = new PlayerConfig();
        temp.setHudComponents(draftHudComponents);
        temp.setDynamicHud(draftDynamicHud);

        ProfilePresets.applyTo(temp, profile);

        this.draftHudComponents = temp.getHudComponents();
        this.draftDynamicHud = temp.getDynamicHud();
        this.dirty = true;
    }

    public HudComponentsConfig getDraftHudComponents() {
        return draftHudComponents;
    }

    public DynamicHudConfig getDraftDynamicHud() {
        return draftDynamicHud;
    }

    public boolean isHidden(@Nonnull HudComponent entry) {
        return entry.isHidden(draftHudComponents);
    }

    public void toggleVisibility(@Nonnull String componentKey) {
        HudComponent entry = HudComponentRegistry.find(componentKey);
        if (entry == null) {
            return;
        }

        boolean hidden = entry.isHidden(draftHudComponents);
        entry.setHidden(draftHudComponents, !hidden);
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
    public List<HudComponent> getDynamicEntries() {
        return HudComponentRegistry.dynamicList();
    }

    @Nonnull
    public DynamicHudRuleConfig getDynamicRuleConfig(@Nonnull HudComponent entry) {
        DynamicHudRuleConfig cfg = entry.getDynamicRuleConfig(draftDynamicHud);
        if (cfg == null) {
            throw new IllegalStateException("Entry is not dynamic-capable: " + entry.key());
        }
        return cfg;
    }

    public boolean isRuleEnabled(
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger rule
    ) {
        return getDynamicRuleConfig(entry).getRules().contains(rule);
    }

    public void toggleRule(
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger rule
    ) {
        if (!entry.supportsRule(rule)) {
            throw new IllegalArgumentException(
                    "Rule " + rule + " is not supported by component " + entry.key()
            );
        }

        DynamicHudRuleConfig cfg = getDynamicRuleConfig(entry);

        if (cfg.getRules().contains(rule)) {
            cfg.removeRule(rule);
        } else {
            cfg.addRule(rule);
        }

        dirty = true;
    }

    public float getDynamicThreshold(@Nonnull HudComponent entry) {
        return getDynamicRuleConfig(entry).getThreshold();
    }

    public void setDynamicThreshold(
            @Nonnull HudComponent entry,
            float value
    ) {
        getDynamicRuleConfig(entry).setThreshold(value);
        dirty = true;
    }

    public boolean isDynamicThresholdEnabled(@Nonnull HudComponent entry) {
        HudTrigger thresholdRule = entry.thresholdRule();
        return thresholdRule != null && isRuleEnabled(entry, thresholdRule);
    }

    @Nonnull
    public List<HudTrigger> getBaseRulesInDisplayOrder(
            @Nonnull HudComponent entry
    ) {
        return HudTrigger.displayCategoryOrder().stream()
                .flatMap(category -> Arrays.stream(HudTrigger.values())
                        .filter(trigger -> trigger.category() == category)
                        .filter(entry::supportsRule))
                .toList();
    }

    public boolean isDynamicComponentVisible(@Nonnull HudComponent entry) {
        return !entry.isHidden(draftHudComponents);
    }

    @Nonnull
    public String getDynamicComponentVisibilityLabel(@Nonnull HudComponent entry) {
        return isDynamicComponentVisible(entry) ? "<VISIBLE>" : "<HIDDEN>";
    }
}