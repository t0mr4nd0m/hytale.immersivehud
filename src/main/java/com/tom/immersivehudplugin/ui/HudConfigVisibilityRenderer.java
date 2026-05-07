package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nonnull;

public final class HudConfigVisibilityRenderer {

    private static final String VISIBILITY_UI = "Views/VisibilityView.ui";

    private final VisibilityStaticDetailRenderer staticDetailRenderer =
            new VisibilityStaticDetailRenderer();

    private final VisibilityComponentSelectorRenderer componentSelectorRenderer =
            new VisibilityComponentSelectorRenderer();

    private final VisibilityDynamicDetailRenderer dynamicDetailRenderer =
            new VisibilityDynamicDetailRenderer();

    public void renderVisibilityView(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.append("#ContentHost", VISIBILITY_UI);

        componentSelectorRenderer.render(commands, events, session);
        dynamicDetailRenderer.bindEvents(events);
        renderDetail(commands, events, session);
    }

    private void renderDetail(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        if (session.isVisibilityStaticSelection()) {
            staticDetailRenderer.render(commands, events, session);
        } else {
            dynamicDetailRenderer.render(commands, events, session);
        }
    }

    public void updateDynamicHeader(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        dynamicDetailRenderer.updateHeader(commands, session, entry);
    }

    public void updateDynamicDetail(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        dynamicDetailRenderer.update(commands, events, session, entry);
    }

    public void updateDynamicRuleRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry,
            @Nonnull HudTrigger trigger
    ) {
        dynamicDetailRenderer.updateRuleRow(commands, session, entry, trigger);
    }

    public void updateStaticRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponent entry
    ) {
        staticDetailRenderer.updateRow(commands, session, entry);
    }
}