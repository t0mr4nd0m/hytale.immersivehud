package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;

import javax.annotation.Nonnull;
import java.util.List;

public final class HudConfigVisibilityRenderer {

    private static final String VISIBILITY_UI = "Pages/ImmersiveHud/Views/HudConfigVisibilityView.ui";
    private static final String VISIBILITY_SECTION_UI = "Pages/ImmersiveHud/Views/HudConfigVisibilitySection.ui";
    private static final String VISIBILITY_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigVisibilityRow.ui";

    private final HudConfigPresenter presenter;
    private final HudConfigRenderIndex renderIndex;

    public HudConfigVisibilityRenderer(
            @Nonnull HudConfigPresenter presenter,
            @Nonnull HudConfigRenderIndex renderIndex
    ) {
        this.presenter = presenter;
        this.renderIndex = renderIndex;
    }

    public void renderVisibilityView(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.append("#ContentHost", VISIBILITY_UI);
        commands.clear("#VisibilityList");

        commands.set(
                "#ViewHelpText.TextSpans",
                Message.raw(session.getCurrentView().helpText())
        );

        renderIndex.clearVisibilityRowIndexes();
        renderIndex.clearVisibilitySectionRowIndexes();

        int rowIndex = 0;
        HudComponentRegistry.Group expandedGroup = session.getExpandedVisibilityGroup();

        for (HudComponentRegistry.Group group : HudComponentRegistry.groupOrder) {
            List<HudComponentRegistry.HudEntry> entries = HudComponentRegistry.allList().stream()
                    .filter(entry -> entry.group() == group)
                    .toList();

            if (entries.isEmpty()) {
                continue;
            }

            commands.append("#VisibilityList", VISIBILITY_SECTION_UI);

            String sectionRootSelector = "#VisibilityList[" + rowIndex + "]";
            String closedButtonSelector = sectionRootSelector + " #VisibilitySectionButtonClosed";
            String openButtonSelector = sectionRootSelector + " #VisibilitySectionButtonOpen";

            String closedContentSelector = sectionRootSelector + " #VisibilitySectionContentClosed";
            String closedGroupSelector = sectionRootSelector + " #VisibilitySectionGroupClosed";
            String closedCounterSelector = sectionRootSelector + " #VisibilitySectionCounterClosed";

            String openContentSelector = sectionRootSelector + " #VisibilitySectionContentOpen";
            String openGroupSelector = sectionRootSelector + " #VisibilitySectionGroupOpen";
            String openCounterSelector = sectionRootSelector + " #VisibilitySectionCounterOpen";

            renderIndex.putVisibilitySectionRowIndex(group, rowIndex);

            boolean expanded = group == expandedGroup;

            String groupTitle = group.label().toUpperCase();
            String groupCounter = presenter.getVisibilityGroupCounterLabel(
                    group,
                    session.getDraftHudComponents()
            );

            commands.set(closedGroupSelector + ".TextSpans", Message.raw(groupTitle));
            commands.set(openGroupSelector + ".TextSpans", Message.raw(groupTitle));

            commands.set(closedCounterSelector + ".TextSpans", Message.raw(groupCounter));
            commands.set(openCounterSelector + ".TextSpans", Message.raw(groupCounter));

            commands.set(closedButtonSelector + ".Visible", !expanded);
            commands.set(openButtonSelector + ".Visible", expanded);

            commands.set(closedContentSelector + ".Visible", !expanded);
            commands.set(openContentSelector + ".Visible", expanded);

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    closedButtonSelector,
                    HudConfigPage.PageEventData.action("VIS_TOGGLE_GROUP").append("Value", group.name()),
                    false
            );

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    openButtonSelector,
                    HudConfigPage.PageEventData.action("VIS_TOGGLE_GROUP").append("Value", group.name()),
                    false
            );

            rowIndex++;

            if (!expanded) {
                continue;
            }

            for (HudComponentRegistry.HudEntry entry : entries) {
                boolean hidden = session.isHidden(entry);

                commands.append("#VisibilityList", VISIBILITY_ROW_UI);

                int entryRowIndex = rowIndex;
                renderIndex.putVisibilityRowIndex(entry.key(), entryRowIndex);

                String rowRootSelector = "#VisibilityList[" + entryRowIndex + "]";
                String componentSelector = rowRootSelector + " #VisibilityComponentLabel";
                String checkBoxSelector = rowRootSelector + " #VisibilityToggleCheckBox";

                commands.set(componentSelector + ".TextSpans", Message.raw(entry.label().toUpperCase()));
                commands.set(checkBoxSelector + ".Value", !hidden);

                events.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        checkBoxSelector,
                        HudConfigPage.PageEventData.action("TOGGLE_VISIBILITY").append("Value", entry.key()),
                        false
                );

                rowIndex++;
            }
        }
    }

    public void updateVisibilitySection(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponentRegistry.Group group
    ) {
        Integer rowIndex = renderIndex.getVisibilitySectionRowIndex(group);
        if (rowIndex == null) {
            return;
        }

        String sectionRootSelector = "#VisibilityList[" + rowIndex + "]";
        String closedCounterSelector = sectionRootSelector + " #VisibilitySectionCounterClosed";
        String openCounterSelector = sectionRootSelector + " #VisibilitySectionCounterOpen";

        String groupCounter = presenter.getVisibilityGroupCounterLabel(
                group,
                session.getDraftHudComponents()
        );

        commands.set(closedCounterSelector + ".TextSpans", Message.raw(groupCounter));
        commands.set(openCounterSelector + ".TextSpans", Message.raw(groupCounter));
    }

    public void updateVisibilityRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull String componentKey
    ) {
        Integer rowIndex = renderIndex.getVisibilityRowIndex(componentKey);
        if (rowIndex == null) {
            return;
        }

        HudComponentRegistry.HudEntry entry = HudComponentRegistry.find(componentKey);
        if (entry == null) {
            return;
        }

        boolean hidden = session.isHidden(entry);

        String rowRootSelector = "#VisibilityList[" + rowIndex + "]";
        String checkBoxSelector = rowRootSelector + " #VisibilityToggleCheckBox";

        commands.set(checkBoxSelector + ".Value", !hidden);
    }
}