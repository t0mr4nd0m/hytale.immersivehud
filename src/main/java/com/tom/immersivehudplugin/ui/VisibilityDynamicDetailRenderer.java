package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class VisibilityDynamicDetailRenderer {

    private static final String VISIBILITY_UI = "Views/VisibilityView.ui";
    private static final String TRIGGER_ROW_UI = "Views/VisibilityTriggerRow.ui";

    private static final String HIDDEN_LABEL = "immersivehud.gui.component.state.hidden";
    private static final String VISIBLE_LABEL = "immersivehud.gui.component.state.visible";
    private static final String TRIGGERS_LABEL = "immersivehud.gui.triggers";

    private static final String VISIBILITY_TRIGGERS = "#VisibilityTriggers";

    void render(
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

        update(commands, events, session, entry);
    }

    void update(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        updateHeader(commands, session, entry);

        commands.clear(VISIBILITY_TRIGGERS);

        if (!session.isHidden(entry)) {
            commands.set("#VisibilityNoActiveTriggersMessage.Visible", false);
            return;
        }

        renderRuleRows(commands, events, session, entry);
    }

    private void renderRuleRows(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        int rowIndex = 0;

        for (HudTrigger trigger : session.getVisibleRulesInDisplayOrder(entry)) {
            commands.append(VISIBILITY_TRIGGERS, TRIGGER_ROW_UI);

            String rowRoot = VISIBILITY_TRIGGERS + "[" + rowIndex + "]";
            renderRuleRow(commands, events, session, entry, trigger, rowRoot);

            rowIndex++;
        }

        updateNoActiveTriggersMessage(commands, session, entry, rowIndex);
    }

    private void renderRuleRow(
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

    void updateHeader(
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

        commands.set(VISIBILITY_TRIGGERS + ".Visible", componentHidden);
        commands.set("#VisibilityNoActiveTriggersMessage.Visible", false);
        commands.set("#VisibilityTriggersMessage.Visible", !componentHidden);

        updateTriggersCounter(commands, session, entry);
        updateTriggerFilterButton(commands, session);
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
                Message.translation(TRIGGERS_LABEL)
                        .param("active", active)
                        .param("total", total)
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

        commands.set(VISIBILITY_TRIGGERS + ".Visible", !showNoActiveTriggers);
        commands.set("#VisibilityNoActiveTriggersMessage.Visible", showNoActiveTriggers);
    }

    void bindEvents(@Nonnull UIEventBuilder events) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DynamicComponentVisibilityStateButton",
                IHudConfigPage.PageEventData.action("VIS_TOGGLE_VISIBILITY"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#TriggerClearButton",
                IHudConfigPage.PageEventData.action("VIS_CLEAR_TRIGGERS"),
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

    void updateRuleRow(
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
        String checkBoxSelector = rowRootSelector + " #TriggerCheckBox";

        commands.set(checkBoxSelector + ".Value", enabled);

        if (isThresholdRule(entry, trigger)) {
            updateThresholdRow(commands, session, entry, rowRootSelector);
        }
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
                return VISIBILITY_TRIGGERS + "[" + rowIndex + "]";
            }

            rowIndex++;
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