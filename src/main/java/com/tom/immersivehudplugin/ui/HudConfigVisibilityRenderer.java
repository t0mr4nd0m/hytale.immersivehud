package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

public final class HudConfigVisibilityRenderer {

    private static final String ROOT_UI =
            "Pages/ImmersiveHud/Views/HudConfigVisibilityView.ui";
    private static final String TARGET_BUTTON_UI =
            "Pages/ImmersiveHud/Views/HudConfigVisibilityTargetButton.ui";
    private static final String TRIGGER_ROW_UI =
            "Pages/ImmersiveHud/Views/HudConfigVisibilityTriggerRow.ui";
    private static final String STATIC_ROW_UI =
            "Pages/ImmersiveHud/Views/HudConfigVisibilityStaticRow.ui";
    private static final String SECTION_HEADER_UI =
            "Pages/ImmersiveHud/Views/HudConfigVisibilitySectionHeader.ui";

    public void renderVisibilityView(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.append("#ContentHost", ROOT_UI);

        commands.set(
                "#ViewHelpText.TextSpans",
                Message.raw(session.getCurrentView().helpText())
        );

        renderTargetSelector(commands, events, session);
        renderDetail(commands, events, session);
    }

    private void renderTargetSelector(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.clear("#VisibilityTargetsList");

        String selectedKey = session.getSelectedVisibilityTargetKey();
        int rowIndex = 0;

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            String key = entry.key();
            boolean selected = key.equalsIgnoreCase(selectedKey);

            commands.append("#VisibilityTargetsList", TARGET_BUTTON_UI);

            String rowRoot = "#VisibilityTargetsList[" + rowIndex + "]";
            String buttonContainerSelector = rowRoot + " #TargetButtonContainer";
            String buttonSelectedContainerSelector = rowRoot + " #TargetButtonSelectedContainer";
            String buttonSelector = rowRoot + " #TargetButton";
            String buttonSelectedSelector = rowRoot + " #TargetButtonSelected";
            String iconSelector = rowRoot + " #TargetIcon";
            String iconSelectedSelector = rowRoot + " #TargetIconSelected";

            String iconRef = selectorIconRef(key);

            commands.set(buttonContainerSelector + ".Visible", !selected);
            commands.set(buttonSelectedContainerSelector + ".Visible", selected);

            commands.set(iconSelector + ".Background", Value.ref(TARGET_BUTTON_UI, iconRef));
            commands.set(iconSelectedSelector + ".Background", Value.ref(TARGET_BUTTON_UI, iconRef));

            commands.set(buttonSelector + ".TooltipText", entry.label());
            commands.set(buttonSelectedSelector + ".TooltipText", entry.label());

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    buttonSelector,
                    HudConfigPage.PageEventData.action("VIS_SELECT_TARGET")
                            .append("Value", key),
                    false
            );

            rowIndex++;
        }

        boolean staticSelected = "static".equalsIgnoreCase(selectedKey);

        commands.append("#VisibilityTargetsList", TARGET_BUTTON_UI);

        String rowRoot = "#VisibilityTargetsList[" + rowIndex + "]";
        String buttonContainerSelector = rowRoot + " #TargetButtonContainer";
        String buttonSelectedContainerSelector = rowRoot + " #TargetButtonSelectedContainer";
        String buttonSelector = rowRoot + " #TargetButton";
        String buttonSelectedSelector = rowRoot + " #TargetButtonSelected";
        String iconSelector = rowRoot + " #TargetIcon";
        String iconSelectedSelector = rowRoot + " #TargetIconSelected";

        commands.set(buttonContainerSelector + ".Visible", !staticSelected);
        commands.set(buttonSelectedContainerSelector + ".Visible", staticSelected);

        commands.set(iconSelector + ".Background", Value.ref(TARGET_BUTTON_UI, "staticIcon"));
        commands.set(iconSelectedSelector + ".Background", Value.ref(TARGET_BUTTON_UI, "staticIcon"));

        commands.set(buttonSelector + ".TooltipText", "Static Components");
        commands.set(buttonSelectedSelector + ".TooltipText", "Static Components");

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                buttonSelector,
                HudConfigPage.PageEventData.action("VIS_SELECT_TARGET")
                        .append("Value", "static"),
                false
        );
    }

    private void renderDetail(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        if (session.isVisibilityStaticSelection()) {
            renderStaticDetail(commands, events, session);
        } else {
            renderDynamicDetail(commands, events, session);
        }
    }

    private void renderDynamicDetail(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        HudComponent entry = session.getSelectedVisibilityComponent();
        if (entry == null) {
            commands.set("#DynamicDetailHost.Visible", false);
            commands.set("#StaticDetailHost.Visible", false);
            return;
        }

        commands.set("#DynamicDetailHost.Visible", true);
        commands.set("#StaticDetailHost.Visible", false);

        updateDynamicHeader(commands, session, entry);
        bindDynamicDetailEvents(events, entry);

        commands.clear("#VisibilityTriggersList");

        if (!session.isHidden(entry)) {
            return;
        }

        renderDynamicRuleRows(commands, events, session, entry);
    }

    public void updateDynamicDetail(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        updateDynamicHeader(commands, session, entry);

        commands.clear("#VisibilityTriggersList");

        if (!session.isHidden(entry)) {
            commands.set("#VisibilityNoActiveTriggersMessage.Visible", false);
            return;
        }

        renderDynamicRuleRows(commands, events, session, entry);
    }

    private void renderDynamicRuleRows(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        int rowIndex = 0;

        for (HudTrigger trigger : session.getVisibleRulesInDisplayOrder(entry)) {
            commands.append("#VisibilityTriggersList", TRIGGER_ROW_UI);

            String rowRoot = "#VisibilityTriggersList[" + rowIndex + "]";
            renderDynamicRuleRow(commands, events, session, entry, trigger, rowRoot);

            rowIndex++;
        }

        updateNoActiveTriggersMessage(commands, session, entry, rowIndex);
    }

    private void updateNoActiveTriggersMessage(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            int visibleRuleCount
    ) {
        boolean showNoActiveTriggers =
                session.isHidden(entry)
                        && session.isShowOnlyCheckedTriggers()
                        && visibleRuleCount == 0;

        commands.set("#VisibilityTriggersList.Visible", !showNoActiveTriggers);
        commands.set("#VisibilityNoActiveTriggersMessage.Visible", showNoActiveTriggers);
    }

    private void renderDynamicRuleRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger,
            @Nonnull String rowRoot
    ) {
        String checkBoxSelector = rowRoot + " #TriggerCheckBox";
        String labelSelector = rowRoot + " #TriggerLabel";

        boolean enabled = session.isRuleEnabled(entry, trigger);

        commands.set(checkBoxSelector + ".Value", enabled);
        commands.set(
                labelSelector + ".TextSpans",
                Message.raw(HudTrigger.displayNameUpper(trigger))
        );

        if (isThresholdRule(entry, trigger)) {
            updateThresholdRow(commands, session, entry, rowRoot);

            String thresholdSliderSelector = rowRoot + " #ThresholdSlider";
            events.addEventBinding(
                    CustomUIEventBindingType.ValueChanged,
                    thresholdSliderSelector,
                    HudConfigPage.PageEventData.action("VIS_SET_THRESHOLD")
                            .append("Component", entry.key())
                            .append("@DynamicThreshold", thresholdSliderSelector + ".Value"),
                    false
            );
        } else {
            commands.set(rowRoot + " #ThresholdInlineHost.Visible", false);
        }

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                checkBoxSelector,
                HudConfigPage.PageEventData.action("VIS_TOGGLE_RULE")
                        .append("Component", entry.key())
                        .append("Value", trigger.name()),
                false
        );
    }

    public void updateDynamicHeader(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        boolean componentHidden = session.isHidden(entry);

        commands.set(
                "#VisibilityComponentTitle.TextSpans",
                Message.raw(entry.label())
        );

        commands.set(
                "#VisibilityComponentStateLabel.Text",
                componentHidden ? "HIDDEN" : "VISIBLE"
        );

        commands.set("#VisibilityTriggersList.Visible", componentHidden);
        commands.set("#VisibilityNoActiveTriggersMessage.Visible", false);
        commands.set("#VisibilityTriggersMessage.Visible", !componentHidden);

        updateTriggerFilterButton(commands, session);
    }

    private void bindDynamicDetailEvents(
            @Nonnull UIEventBuilder events,
            @Nonnull HudComponent entry
    ) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#VisibilityComponentStateButton",
                HudConfigPage.PageEventData.action("VIS_TOGGLE_VISIBILITY")
                        .append("Component", entry.key()),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TriggerClearButton",
                HudConfigPage.PageEventData.action("VIS_CLEAR_TRIGGERS")
                        .append("Component", entry.key()),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TriggerFilterButton",
                HudConfigPage.PageEventData.action("VIS_TOGGLE_TRIGGER_FILTER"),
                false
        );
    }

    private void updateTriggerFilterButton(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session
    ) {

        String iconRef = session.isShowOnlyCheckedTriggers()
                ? "removeFilterIcon"
                : "applyFilterIcon";

        commands.set("#TriggerFilterButtonIcon.Background", Value.ref(ROOT_UI, iconRef));
    }

    public void updateDynamicRuleRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger
    ) {
        String rowRootSelector = findRuleRowRootSelector(session, entry, trigger);
        if (rowRootSelector == null) {
            return;
        }

        boolean enabled = session.isRuleEnabled(entry, trigger);
        boolean componentHidden = session.isHidden(entry);
        String checkBoxSelector = rowRootSelector + " #TriggerCheckBox";

        commands.set(checkBoxSelector + ".Value", enabled);
        commands.set(checkBoxSelector + ".Disabled", !componentHidden);

        if (isThresholdRule(entry, trigger)) {
            updateThresholdRow(commands, session, entry, rowRootSelector);
        }
    }

    public void updateDynamicThresholdControls(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        HudTrigger thresholdRule = entry.thresholdRule();
        if (thresholdRule == null) {
            return;
        }

        String rowRootSelector = findRuleRowRootSelector(session, entry, thresholdRule);
        if (rowRootSelector == null) {
            return;
        }

        updateThresholdRow(commands, session, entry, rowRootSelector);
    }

    private void updateThresholdRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull String rowRoot
    ) {
        String thresholdHostSelector = rowRoot + " #ThresholdInlineHost";
        String thresholdSliderSelector = rowRoot + " #ThresholdSlider";

        boolean thresholdEnabled = session.isDynamicThresholdEnabled(entry);
        boolean componentHidden = session.isHidden(entry);
        int threshold = Math.round(session.getDynamicThreshold(entry));

        commands.set(thresholdHostSelector + ".Visible", thresholdEnabled && componentHidden);
        commands.set(thresholdSliderSelector + ".Value", threshold);
    }

    @Nullable
    private String findRuleRowRootSelector(
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger
    ) {
        int rowIndex = 0;

        for (HudTrigger candidate : session.getVisibleRulesInDisplayOrder(entry)) {
            if (candidate == trigger) {
                return "#VisibilityTriggersList[" + rowIndex + "]";
            }
            rowIndex++;
        }

        return null;
    }

    private void renderStaticDetail(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.set("#DynamicDetailHost.Visible", false);
        commands.set("#StaticDetailHost.Visible", true);

        commands.clear("#VisibilityStaticList");

        int rowIndex = 0;

        for (HudComponentRegistry.Group group : HudComponentRegistry.GROUP_ORDER) {
            boolean groupHeaderAdded = false;

            for (HudComponent entry : HudComponentRegistry.entriesOf(group)) {
                if (entry.supportsDynamicRules()) {
                    continue;
                }

                if (!groupHeaderAdded) {
                    commands.append("#VisibilityStaticList", SECTION_HEADER_UI);

                    String headerRoot = "#VisibilityStaticList[" + rowIndex + "]";
                    commands.set(
                            headerRoot + " #SectionTitle.TextSpans",
                            Message.raw(group.label())
                    );

                    groupHeaderAdded = true;
                    rowIndex++;
                }

                commands.append("#VisibilityStaticList", STATIC_ROW_UI);

                String rowRoot = "#VisibilityStaticList[" + rowIndex + "]";
                String labelSelector = rowRoot + " #StaticVisibilityLabel";
                String stateButtonSelector = rowRoot + " #StaticVisibilityStateButton";
                String stateLabelSelector = rowRoot + " #StaticVisibilityStateLabel";

                commands.set(
                        labelSelector + ".TextSpans",
                        Message.raw(entry.label())
                );
                commands.set(
                        stateLabelSelector + ".Text",
                        session.isHidden(entry) ? "HIDDEN" : "VISIBLE"
                );

                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        stateButtonSelector,
                        HudConfigPage.PageEventData.action("VIS_TOGGLE_STATIC_VISIBILITY")
                                .append("Component", entry.key()),
                        false
                );

                rowIndex++;
            }
        }
    }

    public void updateStaticRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        String rowRoot = findStaticRowRootSelector(entry);
        if (rowRoot == null) {
            return;
        }

        commands.set(
                rowRoot + " #StaticVisibilityLabel.TextSpans",
                Message.raw(entry.label())
        );
        commands.set(
                rowRoot + " #StaticVisibilityStateLabel.Text",
                session.isHidden(entry) ? "HIDDEN" : "VISIBLE"
        );
    }

    @Nullable
    private String findStaticRowRootSelector(@Nonnull HudComponent target) {
        int rowIndex = 0;

        for (HudComponentRegistry.Group group : HudComponentRegistry.GROUP_ORDER) {
            boolean groupHeaderAdded = false;

            for (HudComponent entry : HudComponentRegistry.entriesOf(group)) {
                if (entry.supportsDynamicRules()) {
                    continue;
                }

                if (!groupHeaderAdded) {
                    rowIndex++;
                    groupHeaderAdded = true;
                }

                if (entry == target) {
                    return "#VisibilityStaticList[" + rowIndex + "]";
                }

                rowIndex++;
            }
        }

        return null;
    }

    private boolean isThresholdRule(
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger
    ) {
        return entry.thresholdRule() == trigger;
    }

    @Nonnull
    private String selectorIconRef(@Nonnull String key) {
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "health" -> "healthIcon";
            case "stamina" -> "staminaIcon";
            case "mana" -> "manaIcon";
            case "oxygen" -> "oxygenIcon";
            case "hotbar" -> "hotbarIcon";
            case "reticle" -> "reticleIcon";
            case "compass" -> "compassIcon";
            case "statusicons" -> "statusiconsIcon";
            default -> "staticIcon";
        };
    }
}