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

public final class HudConfigVisibilityRenderer {

    private static final String VISIBILITY_UI = "Views/VisibilityView.ui";
    private static final String COMPONENT_BUTTON_UI = "Views/VisibilityComponentButton.ui";
    private static final String TRIGGER_ROW_UI = "Views/VisibilityTriggerRow.ui";
    private static final String HEADER_UI = "Views/VisibilityStaticHeader.ui";
    private static final String STATIC_ROW_UI = "Views/VisibilityStaticRow.ui";

    private static final String HIDDEN_LABEL = "immersivehud.gui.component.state.hidden";
    private static final String VISIBLE_LABEL = "immersivehud.gui.component.state.visible";
    private static final String TRIGGERS_LABEL = "immersivehud.gui.triggers";

    private static final Value<String> STATIC_BUTTON_ICON = Value.ref(COMPONENT_BUTTON_UI, "staticIcon");
    private static final String STATIC_BUTTON_TOOLTIP = "immersivehud.gui.visibility.static.components.button.tooltip";

    public void renderVisibilityView(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.append("#ContentHost", VISIBILITY_UI);

        renderComponentSelector(commands, events, session);
        renderDetail(commands, events, session);
    }

    private void renderComponentSelector(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.clear("#ComponentsButtons");

        String selectedKey = session.getSelectedVisibilityComponentKey();
        int rowIndex = 0;

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            String key = entry.key();
            boolean selected = key.equalsIgnoreCase(selectedKey);

            commands.append("#ComponentsButtons", COMPONENT_BUTTON_UI);

            String buttonSelector = "#ComponentsButtons[" + rowIndex + "]";
            String buttonIconSelector = buttonSelector + " #ComponentButtonIcon";

            Value<String> componentIcon = Value.ref(COMPONENT_BUTTON_UI, key+"Icon");
            commands.set(buttonIconSelector + ".Background", componentIcon);
            commands.set(buttonSelector + ".TooltipText", entry.label());

            if (!selected) {
                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        buttonSelector,
                        IHudConfigPage.PageEventData.action("VIS_SELECT_COMPONENT")
                                .append("Value", key),
                        false
                );
            }

            rowIndex++;
        }

        boolean staticSelected = "static".equalsIgnoreCase(selectedKey);

        commands.append("#ComponentsButtons", COMPONENT_BUTTON_UI);

        String buttonSelector = "#ComponentsButtons[" + rowIndex + "]";
        String buttonIconSelector = buttonSelector + " #ComponentButtonIcon";

        commands.set(buttonIconSelector + ".Background", STATIC_BUTTON_ICON);
        commands.set(buttonSelector + ".TooltipText", Message.translation(STATIC_BUTTON_TOOLTIP));

        if  (!staticSelected) {
            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    buttonSelector,
                    IHudConfigPage.PageEventData.action("VIS_SELECT_COMPONENT")
                            .append("Value", "static"),
                    false
            );
        }
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
            commands.set("#DynamicComponentHost.Visible", false);
            commands.set("#StaticComponentsHost.Visible", false);
            return;
        }

        commands.set("#DynamicComponentHost.Visible", true);
        commands.set("#StaticComponentsHost.Visible", false);

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

    private void updateTriggersCounter(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        int total = entry.allowedRules().size();

        int active = 0;
        for (HudTrigger trigger : entry.allowedRules()) {
            if (session.isRuleEnabled(entry, trigger)) {
                active++;
            }
        }

        commands.set(
                "#TriggersHeaderLabel.Text",
                Message.translation(TRIGGERS_LABEL).param("active", active).param("total", total)
        );
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
                    IHudConfigPage.PageEventData.action("VIS_SET_THRESHOLD")
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
                IHudConfigPage.PageEventData.action("VIS_TOGGLE_RULE")
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
                "#DynamicComponentTitle.TextSpans",
                Message.raw(entry.label())
        );

        commands.set(
                "#DynamicComponentVisiblityStateLabel.Text",
                Message.translation(componentHidden ? HIDDEN_LABEL : VISIBLE_LABEL)
        );

        commands.set("#VisibilityTriggersList.Visible", componentHidden);
        commands.set("#VisibilityNoActiveTriggersMessage.Visible", false);
        commands.set("#VisibilityTriggersMessage.Visible", !componentHidden);

        updateTriggersCounter(commands, session, entry);
        updateTriggerFilterButton(commands, session);
    }

    private void bindDynamicDetailEvents(
            @Nonnull UIEventBuilder events,
            @Nonnull HudComponent entry
    ) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DynamicComponentVisibilityStateButton",
                IHudConfigPage.PageEventData.action("VIS_TOGGLE_VISIBILITY")
                        .append("Component", entry.key()),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TriggerClearButton",
                IHudConfigPage.PageEventData.action("VIS_CLEAR_TRIGGERS")
                        .append("Component", entry.key()),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TriggerFilterButton",
                IHudConfigPage.PageEventData.action("VIS_TOGGLE_TRIGGER_FILTER"),
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

        commands.set("#TriggerFilterButtonIcon.Background", Value.ref(VISIBILITY_UI, iconRef));
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
        commands.set("#DynamicComponentHost.Visible", false);
        commands.set("#StaticComponentsHost.Visible", true);

        commands.clear("#StaticComponentsList");

        int rowIndex = 0;

        for (HudComponentRegistry.Group group : HudComponentRegistry.GROUP_ORDER) {
            boolean groupHeaderAdded = false;

            for (HudComponent entry : HudComponentRegistry.entriesOf(group)) {
                if (entry.supportsDynamicRules()) {
                    continue;
                }

                if (!groupHeaderAdded) {
                    commands.append("#StaticComponentsList", HEADER_UI);

                    String headerRoot = "#StaticComponentsList[" + rowIndex + "]";
                    commands.set(
                            headerRoot + " #SectionTitle.TextSpans",
                            Message.raw(group.label())
                    );

                    groupHeaderAdded = true;
                    rowIndex++;
                }

                commands.append("#StaticComponentsList", STATIC_ROW_UI);

                String rowRoot = "#StaticComponentsList[" + rowIndex + "]";
                String labelSelector = rowRoot + " #StaticComponentLabel";
                String stateButtonSelector = rowRoot + " #StaticComponentVisibilityButton";
                String stateLabelSelector = rowRoot + " #StaticComponentVisibilityStateLabel";

                commands.set(
                        labelSelector + ".TextSpans",
                        Message.raw(entry.label())
                );
                commands.set(
                        stateLabelSelector + ".Text",
                        Message.translation(session.isHidden(entry) ? HIDDEN_LABEL : VISIBLE_LABEL)
                );

                events.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        stateButtonSelector,
                        IHudConfigPage.PageEventData.action("VIS_TOGGLE_STATIC_VISIBILITY")
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
                Message.translation(session.isHidden(entry) ? HIDDEN_LABEL : VISIBLE_LABEL)
        );
    }

    @Nullable
    private String findStaticRowRootSelector(@Nonnull HudComponent component) {
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

                if (entry == component) {
                    return "#StaticComponentsList[" + rowIndex + "]";
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
}