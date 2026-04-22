package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HudConfigDynamicRulesRenderer {

    private static final String DYNAMIC_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRulesView.ui";
    private static final String DYNAMIC_SECTION_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRulesSection.ui";
    private static final String DYNAMIC_RULE_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRuleRow.ui";

    private final HudConfigRenderIndex renderIndex;

    public HudConfigDynamicRulesRenderer(@Nonnull HudConfigRenderIndex renderIndex) {
        this.renderIndex = renderIndex;
    }

    public void renderDynamicRulesView(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.append("#ContentHost", DYNAMIC_UI);
        commands.clear("#DynamicComponentsList");

        commands.set(
                "#ViewHelpText.TextSpans",
                Message.raw(session.getCurrentView().helpText())
        );

        renderIndex.clearDynamicRuleRowIndexes();
        renderIndex.clearDynamicComponentRowIndexes();

        int componentIndex = 0;

        for (HudComponent entry : session.getDynamicEntries()) {
            commands.append("#DynamicComponentsList", DYNAMIC_SECTION_UI);
            renderIndex.putDynamicComponentRowIndex(entry.key(), componentIndex);

            String componentRoot = "#DynamicComponentsList[" + componentIndex + "]";
            String iconSelector = componentRoot + " #DynamicComponentIcon";
            String titleSelector = componentRoot + " #DynamicComponentValueLabel";

            String visibilityToggleButtonSelector = componentRoot + " #VisibilityToggleButton";
            String statusButtonSelector = componentRoot + " #StatusButton";

            String rulesListSelector = componentRoot + " #DynamicRulesList";

            String componentStatus = session.getDynamicComponentVisibilityLabel(entry);
            boolean hasRules = !session.getDynamicRuleConfig(entry).getRules().isEmpty();
            boolean isVisible = componentStatus.equalsIgnoreCase("VISIBLE");

            String iconVisibilityToggleButton = isVisible ? "IconVisibilityOn" : "IconVisibilityOff";
            String iconStatusButton = !isVisible && hasRules ? "IconDynamic" : "IconStatic";

            commands.set(iconSelector + ".Background", Value.ref(DYNAMIC_SECTION_UI, entry.key() + "Icon"));
            commands.set(titleSelector + ".TextSpans", Message.raw(entry.label().toUpperCase()));

            commands.set(visibilityToggleButtonSelector + ".Background",
                    Value.ref(DYNAMIC_SECTION_UI, iconVisibilityToggleButton));
            commands.set(visibilityToggleButtonSelector + ".TooltipText", isVisible ? "Visible" : "Hidden");

            commands.set(statusButtonSelector + ".Background", Value.ref(DYNAMIC_SECTION_UI, iconStatusButton));
            commands.set(statusButtonSelector + ".TooltipText", !isVisible && hasRules ? "Dynamic" : "Static");

            if (!isVisible) {
                renderDynamicRulesList(commands, events, session, entry, rulesListSelector);
            }

            String editButtonSelector = componentRoot + " #DynamicComponentEditButton";

            updateDynamicComponentEditButton(commands, session, entry);

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    editButtonSelector,
                    HudConfigPage.PageEventData.action("TOGGLE_DYNAMIC_COMPONENT_EXPANDED")
                            .append("Component", entry.key()),
                    false
            );

            componentIndex++;
        }
    }

    public void updateDynamicComponentEditButton(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        Integer componentIndex = renderIndex.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) return;

        String componentRoot = "#DynamicComponentsList[" + componentIndex + "]";
        String editButtonSelector = componentRoot + " #DynamicComponentEditButton";

        boolean expanded = session.isDynamicComponentExpanded(entry);

        commands.set(
                editButtonSelector + ".Background",
                Value.ref(DYNAMIC_SECTION_UI, expanded ? "IconSaveTriggers" : "IconEditTriggers")
        );
        commands.set(editButtonSelector + ".TooltipText", expanded ? "Colapse rules" : "Edit rules");
    }

    public void updateDynamicRuleRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger
    ) {
        String rowRootSelector = findRuleRowRootSelector(entry, trigger);
        if (rowRootSelector == null) {
            return;
        }

        boolean enabled = session.isRuleEnabled(entry, trigger);
        String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";

        commands.set(checkBoxSelector + ".Value", enabled);

        if (isThresholdRule(entry, trigger)) {
            updateThresholdRow(commands, session, entry, rowRootSelector);
        }
    }

    public void updateDynamicStatus(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        Integer componentIndex = renderIndex.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) return;

        String componentRoot = "#DynamicComponentsList[" + componentIndex + "]";

        String statusButtonSelector = componentRoot + " #StatusButton";

        String componentStatus = session.getDynamicComponentVisibilityLabel(entry);
        boolean isVisible = componentStatus.equalsIgnoreCase("VISIBLE");
        boolean hasRules = !session.getDynamicRuleConfig(entry).getRules().isEmpty();
        String iconStatusButton = !isVisible && hasRules ? "IconDynamic" : "IconStatic";

        commands.set(statusButtonSelector + ".Background", Value.ref(DYNAMIC_SECTION_UI, iconStatusButton));
        commands.set(statusButtonSelector + ".TooltipText", !isVisible && hasRules ? "Dynamic" : "Static");
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

        String rowRootSelector = findRuleRowRootSelector(entry, thresholdRule);
        if (rowRootSelector == null) {
            return;
        }

        updateThresholdRow(commands, session, entry, rowRootSelector);
    }

    private void renderDynamicRulesList(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull String rulesListSelector
    ) {
        int rowIndex = 0;

        for (HudTrigger trigger : session.getVisibleRulesInDisplayOrder(entry)) {
            rowIndex = renderDynamicRuleRow(commands, events, session, entry, rulesListSelector, "base", rowIndex, trigger);
        }
    }

    private int renderDynamicRuleRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull String hostSelector,
            @Nonnull String hostKey,
            int rowIndex,
            @Nonnull HudTrigger trigger
    ) {
        boolean enabled = session.isRuleEnabled(entry, trigger);
        //boolean expanded = session.isDynamicComponentExpanded(entry);

        commands.append(hostSelector, DYNAMIC_RULE_ROW_UI);
        renderIndex.putDynamicRuleRowIndex(entry.key(), hostKey, trigger, rowIndex);

        String rowRootSelector = hostSelector + "[" + rowIndex + "]";
        String labelSelector = rowRootSelector + " #DynamicRuleLabel";
        String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";

        commands.set(labelSelector + ".TextSpans", Message.raw(HudTrigger.displayNameUpper(trigger)));
        commands.set(checkBoxSelector + ".Value", enabled);
        //commands.set(checkBoxSelector + ".Disabled", !expanded);

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                checkBoxSelector,
                HudConfigPage.PageEventData.action("TOGGLE_RULE")
                        .append("Component", entry.key())
                        .append("Value", trigger.name()),
                false
        );

        if (isThresholdRule(entry, trigger)) {
            String thresholdSliderSelector = rowRootSelector + " #DynamicRuleThresholdSlider";

            updateThresholdRow(commands, session, entry, rowRootSelector);

            events.addEventBinding(
                    CustomUIEventBindingType.ValueChanged,
                    thresholdSliderSelector,
                    EventData.of("Action", "DYN_SET_THRESHOLD")
                            .append("Component", entry.key())
                            .append("@DynamicThreshold", thresholdSliderSelector + ".Value"),
                    false
            );
        }

        return rowIndex + 1;
    }

    private void updateThresholdRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull String rowRootSelector
    ) {
        String thresholdHostSelector = rowRootSelector + " #DynamicRuleThresholdHost";
        String thresholdSliderSelector = rowRootSelector + " #DynamicRuleThresholdSlider";

        boolean enabled = session.isDynamicThresholdEnabled(entry);
        int threshold = Math.round(session.getDynamicThreshold(entry));

        commands.set(thresholdHostSelector + ".Visible", enabled);
        commands.set(thresholdSliderSelector + ".Value", threshold);
    }

    private boolean isThresholdRule(
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger
    ) {
        HudTrigger thresholdRule = entry.thresholdRule();
        return thresholdRule == trigger;
    }

    @Nullable
    private String findRuleRowRootSelector(
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger
    ) {
        Integer componentIndex = renderIndex.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) {
            return null;
        }

        Integer baseRowIndex = renderIndex.getDynamicRuleRowIndex(entry.key(), "base", trigger);
        if (baseRowIndex != null) {
            return "#DynamicComponentsList[" + componentIndex + "] #DynamicRulesList[" + baseRowIndex + "]";
        }

        Integer extraRowIndex = renderIndex.getDynamicRuleRowIndex(entry.key(), "extra", trigger);
        if (extraRowIndex != null) {
            return "#DynamicComponentsList[" + componentIndex + "] #DynamicExtraTriggersList[" + extraRowIndex + "]";
        }

        return null;
    }

    public void rerenderDynamicComponentRules(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        Integer componentIndex = renderIndex.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) {
            return;
        }

        String rulesListSelector = "#DynamicComponentsList[" + componentIndex + "] #DynamicRulesList";

        renderIndex.clearDynamicRuleRowIndexes(entry.key());
        commands.clear(rulesListSelector);

        renderDynamicRulesList(commands, events, session, entry, rulesListSelector);
        updateDynamicStatus(commands, session, entry);
    }
}