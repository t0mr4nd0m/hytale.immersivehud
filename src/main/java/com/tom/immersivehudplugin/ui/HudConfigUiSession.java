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
import java.util.EnumSet;
import java.util.List;

public final class HudConfigUiSession {

    private boolean dirty;
    private HudConfigView currentView = HudConfigView.PROFILES;


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

    @Nullable
    private String selectedVisibilityComponentKey;

    public void showVisibilityView() {
        currentView = HudConfigView.VISIBILITY;
    }

    @Nonnull
    public String getSelectedVisibilityComponentKey() {
        if (selectedVisibilityComponentKey != null && !selectedVisibilityComponentKey.isBlank()) {
            return selectedVisibilityComponentKey;
        }

        return HudComponentRegistry.dynamicList().isEmpty()
                ? "static"
                : HudComponentRegistry.dynamicList().getFirst().key();
    }

    public void selectVisibilityComponent(@Nullable String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        this.selectedVisibilityComponentKey = key;
    }

    public boolean isVisibilityStaticSelection() {
        return "static".equalsIgnoreCase(getSelectedVisibilityComponentKey());
    }

    @Nullable
    public HudComponent getSelectedVisibilityComponent() {
        if (isVisibilityStaticSelection()) {
            return null;
        }
        return HudComponentRegistry.find(getSelectedVisibilityComponentKey());
    }

    private boolean showOnlyCheckedTriggers;

    public boolean isShowOnlyCheckedTriggers() {
        return showOnlyCheckedTriggers;
    }

    public void toggleShowOnlyCheckedTriggers() {
        showOnlyCheckedTriggers = !showOnlyCheckedTriggers;
    }

    @Nonnull
    public List<HudTrigger> getVisibleRulesInDisplayOrder(@Nonnull HudComponent entry) {
        var rules = getDynamicRuleConfig(entry).getRules();

        return getBaseRulesInDisplayOrder(entry).stream()
                .filter(trigger -> !showOnlyCheckedTriggers || rules.contains(trigger))
                .toList();
    }

    public boolean shouldRebuildTriggerListAfterRuleToggle() {
        return showOnlyCheckedTriggers;
    }

    public void clearRules(@Nonnull HudComponent entry) {
        DynamicHudRuleConfig cfg = getDynamicRuleConfig(entry);

        if (!cfg.hasRules()) {
            return;
        }

        cfg.setRules(EnumSet.noneOf(HudTrigger.class));
        dirty = true;
    }
}