package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class VisibilityStaticDetailRenderer {

    private static final String HEADER_UI = "Views/VisibilityStaticHeader.ui";
    private static final String STATIC_ROW_UI = "Views/VisibilityStaticRow.ui";
    private static final String STATIC_COMPONENTS = "#StaticComponents";

    private static final String HIDDEN_LABEL = "immersivehud.gui.component.state.hidden";
    private static final String VISIBLE_LABEL = "immersivehud.gui.component.state.visible";

    void render(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.set("#DynamicComponentHost.Visible", false);
        commands.set("#StaticComponentsHost.Visible", true);

        commands.clear(STATIC_COMPONENTS);

        int rowIndex = 0;

        for (HudComponentRegistry.Group group : HudComponentRegistry.GROUP_ORDER) {
            boolean groupHeaderAdded = false;

            for (HudComponent entry : HudComponentRegistry.entriesOf(group)) {
                if (entry.supportsDynamicRules()) {
                    continue;
                }

                if (!groupHeaderAdded) {
                    commands.append(STATIC_COMPONENTS, HEADER_UI);

                    String headerRoot = STATIC_COMPONENTS + "[" + rowIndex + "]";
                    commands.set(
                            headerRoot + " #SectionTitle.TextSpans",
                            Message.raw(group.label())
                    );

                    groupHeaderAdded = true;
                    rowIndex++;
                }

                renderStaticRow(commands, events, session, entry, rowIndex);
                rowIndex++;
            }
        }
    }

    private void renderStaticRow(
            UICommandBuilder commands,
            UIEventBuilder events,
            HudConfigUiSession session,
            HudComponent entry,
            int rowIndex
    ) {
        commands.append(STATIC_COMPONENTS, STATIC_ROW_UI);

        String rowRoot = STATIC_COMPONENTS + "[" + rowIndex + "]";
        String labelSelector = rowRoot + " #StaticComponentLabel";
        String stateButtonSelector = rowRoot + " #StaticComponentVisibilityButton";
        String stateLabelSelector = rowRoot + " #StaticComponentVisibilityStateLabel";

        commands.set(labelSelector + ".TextSpans", Message.raw(entry.label()));
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
    }

    void updateRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        String rowRoot = findRowRootSelector(entry);
        if (rowRoot == null) {
            return;
        }

        commands.set(rowRoot + " #StaticComponentLabel.TextSpans", Message.raw(entry.label()));
        commands.set(
                rowRoot + " #StaticComponentVisibilityStateLabel.Text",
                Message.translation(session.isHidden(entry) ? HIDDEN_LABEL : VISIBLE_LABEL)
        );
    }

    @Nullable
    private String findRowRootSelector(@Nonnull HudComponent component) {
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
                    return STATIC_COMPONENTS + "[" + rowIndex + "]";
                }

                rowIndex++;
            }
        }

        return null;
    }
}
