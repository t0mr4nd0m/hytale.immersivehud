package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;

import javax.annotation.Nonnull;

final class VisibilityComponentSelectorRenderer {

    private static final String COMPONENT_BUTTON_UI = "Views/VisibilityComponentButton.ui";
    private static final String COMPONENTS_BUTTONS = "#ComponentsButtons";
    private static final String STATIC = "static";

    private static final Value<String> STATIC_BUTTON_ICON = Value.ref(COMPONENT_BUTTON_UI, "staticIcon");
    private static final String STATIC_BUTTON_TOOLTIP = "immersivehud.gui.visibility.static.components.button.tooltip";

    void render(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.clear(COMPONENTS_BUTTONS);

        String selectedKey = session.getSelectedVisibilityComponentKey();
        int rowIndex = 0;

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            renderDynamicComponentButton(commands, events, entry, selectedKey, rowIndex);
            rowIndex++;
        }

        renderStaticButton(commands, events, selectedKey, rowIndex);
    }

    private void renderDynamicComponentButton(
            UICommandBuilder commands,
            UIEventBuilder events,
            HudComponent entry,
            String selectedKey,
            int rowIndex
    ) {
        String key = entry.key();
        boolean selected = key.equalsIgnoreCase(selectedKey);

        commands.append(COMPONENTS_BUTTONS, COMPONENT_BUTTON_UI);

        String buttonSelector = COMPONENTS_BUTTONS + "[" + rowIndex + "]";
        String iconSelector = buttonSelector + " #ComponentButtonIcon";

        commands.set(iconSelector + ".Background", Value.ref(COMPONENT_BUTTON_UI, key + "Icon"));
        commands.set(buttonSelector + ".TooltipText", entry.label());

        if (!selected) {
            bindSelectComponent(events, buttonSelector, key);
        }
    }

    private void renderStaticButton(
            UICommandBuilder commands,
            UIEventBuilder events,
            String selectedKey,
            int rowIndex
    ) {
        boolean selected = STATIC.equalsIgnoreCase(selectedKey);

        commands.append(COMPONENTS_BUTTONS, COMPONENT_BUTTON_UI);

        String buttonSelector = COMPONENTS_BUTTONS + "[" + rowIndex + "]";
        String iconSelector = buttonSelector + " #ComponentButtonIcon";

        commands.set(iconSelector + ".Background", STATIC_BUTTON_ICON);
        commands.set(buttonSelector + ".TooltipText", Message.translation(STATIC_BUTTON_TOOLTIP));

        if (!selected) {
            bindSelectComponent(events, buttonSelector, STATIC);
        }
    }

    private void bindSelectComponent(
            UIEventBuilder events,
            String selector,
            String value
    ) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                IHudConfigPage.PageEventData.action("VIS_SELECT_COMPONENT")
                        .append("Value", value),
                false
        );
    }
}