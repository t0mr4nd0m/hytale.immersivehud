package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nonnull;
import java.util.List;

public final class HudConfigDynamicRulesRenderer {

    private static final String DYNAMIC_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRulesView.ui";
    private static final String DYNAMIC_SECTION_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRulesSection.ui";
    private static final String DYNAMIC_RULE_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRuleRow.ui";
    private static final String DYNAMIC_MORE_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRuleMoreRow.ui";

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
            String titleSelector = componentRoot + " #DynamicComponentValueLabel";
            String visibilitySelector = componentRoot + " #DynamicComponentVisibilityLabel";
            String rulesListSelector = componentRoot + " #DynamicRulesList";

            commands.set(titleSelector + ".TextSpans", Message.raw(entry.label().toUpperCase()));
            commands.set(
                    visibilitySelector + ".TextSpans",
                    Message.raw(session.getDynamicComponentVisibilityLabel(entry))
            );

            renderDynamicRulesList(commands, events, session, entry, rulesListSelector);
            renderDynamicThresholdControls(commands, events, session, entry, componentRoot);
            renderDynamicExtraTriggers(commands, events, session, entry, componentRoot);

            componentIndex++;
        }
    }

    public void updateDynamicRuleRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger
    ) {
        Integer componentIndex = renderIndex.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) {
            return;
        }

        boolean enabled = session.isRuleEnabled(entry, trigger);

        Integer baseRowIndex = renderIndex.getDynamicRuleRowIndex(entry.key(), "base", trigger);
        if (baseRowIndex != null) {
            String rulesListSelector = "#DynamicComponentsList[" + componentIndex + "] #DynamicRulesList";
            String rowRootSelector = rulesListSelector + "[" + baseRowIndex + "]";
            String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";
            commands.set(checkBoxSelector + ".Value", enabled);
            return;
        }

        Integer extraRowIndex = renderIndex.getDynamicRuleRowIndex(entry.key(), "extra", trigger);
        if (extraRowIndex != null) {
            String extraListSelector = "#DynamicComponentsList[" + componentIndex + "] #DynamicExtraTriggersList";
            String rowRootSelector = extraListSelector + "[" + extraRowIndex + "]";
            String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";
            commands.set(checkBoxSelector + ".Value", enabled);
        }
    }

    public void updateDynamicExtraTriggers(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        Integer componentIndex = renderIndex.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) {
            return;
        }

        String componentRoot = "#DynamicComponentsList[" + componentIndex + "]";
        renderDynamicExtraTriggers(commands, events, session, entry, componentRoot);
    }

    public void updateDynamicThresholdControls(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        Integer componentIndex = renderIndex.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) {
            return;
        }

        String componentRoot = "#DynamicComponentsList[" + componentIndex + "]";
        String thresholdHostSelector = componentRoot + " #DynamicThresholdHost";
        String sliderSelector = componentRoot + " #DynamicThresholdSlider";

        boolean visible = entry.supportsThreshold();
        boolean enabled = session.isDynamicThresholdEnabled(entry);

        commands.set(thresholdHostSelector + ".Visible", visible);

        if (!visible) {
            return;
        }

        int threshold = Math.round(session.getDynamicThreshold(entry));
        commands.set(sliderSelector + ".Value", threshold);
        commands.set(thresholdHostSelector + ".Visible", enabled);
    }

    private void renderDynamicRulesList(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull String rulesListSelector
    ) {
        int rowIndex = 0;

        for (HudTrigger trigger : session.getBaseRulesInDisplayOrder(entry)) {
            rowIndex = renderDynamicRuleRow(commands, events, session, entry, rulesListSelector, "base", rowIndex, trigger);
        }
    }

    private void renderDynamicExtraTriggers(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull String componentRoot
    ) {
        List<HudTrigger> extraRules = session.getExtraRulesInDisplayOrder(entry);
        boolean revealed = session.isMoreTriggersRevealed(entry.key());

        String extraHostSelector = componentRoot + " #DynamicExtraTriggersList";
        commands.clear(extraHostSelector);

        if (extraRules.isEmpty()) {
            return;
        }

        int rowIndex = 0;

        if (!revealed) {
            commands.append(extraHostSelector, DYNAMIC_MORE_ROW_UI);

            String rowRootSelector = extraHostSelector + "[" + rowIndex + "]";
            String buttonSelector = rowRootSelector + " #DynamicMoreTriggersButton";
            String labelSelector = rowRootSelector + " #DynamicMoreTriggersLabel";

            commands.set(labelSelector + ".TextSpans", Message.raw("MORE TRIGGERS"));

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    buttonSelector,
                    HudConfigPage.PageEventData.action("DYN_REVEAL_MORE")
                            .append("Component", entry.key()),
                    false
            );
            return;
        }

        for (HudTrigger trigger : extraRules) {
            rowIndex = renderDynamicRuleRow(
                    commands,
                    events,
                    session,
                    entry,
                    extraHostSelector,
                    "extra",
                    rowIndex,
                    trigger
            );
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

        commands.append(hostSelector, DYNAMIC_RULE_ROW_UI);
        renderIndex.putDynamicRuleRowIndex(entry.key(), hostKey, trigger, rowIndex);

        String rowRootSelector = hostSelector + "[" + rowIndex + "]";
        String labelSelector = rowRootSelector + " #DynamicRuleLabel";
        String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";

        commands.set(
                labelSelector + ".TextSpans",
                Message.raw(HudTrigger.prettyName(trigger).toUpperCase())
        );
        commands.set(checkBoxSelector + ".Value", enabled);

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                checkBoxSelector,
                HudConfigPage.PageEventData.action("TOGGLE_RULE")
                        .append("Component", entry.key())
                        .append("Value", trigger.name()),
                false
        );

        return rowIndex + 1;
    }

    private void renderDynamicThresholdControls(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull String componentRoot
    ) {
        String thresholdHostSelector = componentRoot + " #DynamicThresholdHost";
        String sliderSelector = componentRoot + " #DynamicThresholdSlider";

        boolean visible = entry.supportsThreshold();
        boolean enabled = session.isDynamicThresholdEnabled(entry);

        commands.set(thresholdHostSelector + ".Visible", visible);

        if (!visible) {
            return;
        }

        int threshold = Math.round(session.getDynamicThreshold(entry));
        commands.set(sliderSelector + ".Value", threshold);
        commands.set(thresholdHostSelector + ".Visible", enabled);

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                sliderSelector,
                EventData.of("Action", "DYN_SET_THRESHOLD")
                        .append("Component", entry.key())
                        .append("@DynamicThreshold", sliderSelector + ".Value"),
                false
        );
    }
}