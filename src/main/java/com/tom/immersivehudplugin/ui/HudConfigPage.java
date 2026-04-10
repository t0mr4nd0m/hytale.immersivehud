package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public final class HudConfigPage extends InteractiveCustomUIPage<HudConfigPage.PageEventData> {

    private static final String PAGE_UI = "Pages/ImmersiveHud/HudConfigPage.ui";

    private static final String PROFILES_UI = "Pages/ImmersiveHud/Views/HudConfigProfilesView.ui";
    private static final String PROFILE_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigProfileRow.ui";

    private static final String VISIBILITY_UI = "Pages/ImmersiveHud/Views/HudConfigVisibilityView.ui";
    private static final String VISIBILITY_SECTION_UI = "Pages/ImmersiveHud/Views/HudConfigVisibilitySection.ui";
    private static final String VISIBILITY_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigVisibilityRow.ui";

    private static final String DYNAMIC_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRulesView.ui";
    private static final String DYNAMIC_SECTION_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRulesSection.ui";
    private static final String DYNAMIC_RULE_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRuleRow.ui";
    private static final String DYNAMIC_MORE_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRuleMoreRow.ui";

    private final HudConfigUiService uiService;
    private final PlayerRef playerRef;
    private final HudConfigPresenter presenter = new HudConfigPresenter();

    public HudConfigPage(
            @Nonnull HudConfigUiService uiService,
            @Nonnull PlayerRef playerRef
    ) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.uiService = uiService;
        this.playerRef = playerRef;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull Store<EntityStore> store
    ) {
        commands.append(PAGE_UI);
        bindChromeEvents(events);
        render(commands, events);
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PageEventData data
    ) {
        super.handleDataEvent(ref, store, data);

        HudConfigUiSession session = uiService.getSession(playerRef);
        if (session == null) {
            uiService.closePage(ref, store);
            return;
        }

        String action = data.getAction();
        if (action == null) {
            sendUpdate();
            return;
        }

        switch (action) {

            case "VIEW_PROFILES" -> {
                session.showProfilesView();
                rebuildPageContent();
            }

            case "SELECT_PROFILE" -> {
                Profile profile = Profile.fromString(data.getValue());
                if (profile != null && profile != Profile.CUSTOM) {
                    session.selectProfile(profile);
                    rebuildPageContent();
                }
            }

            case "VIEW_VISIBILITY" -> {
                session.showVisibilityView();
                rebuildPageContent();
            }

            case "VIEW_DYNAMIC_RULES" -> {
                session.showDynamicRulesView();
                rebuildPageContent();
            }

            case "TOGGLE_VISIBILITY" -> {
                if (data.getValue() != null && !data.getValue().isBlank()) {
                    HudComponentRegistry.HudEntry entry = HudComponentRegistry.find(data.getValue());
                    if (entry == null) {
                        return;
                    }

                    session.toggleVisibility(data.getValue());

                    UICommandBuilder commands = new UICommandBuilder();
                    updateVisibilityRow(commands, session, data.getValue());
                    updateVisibilitySection(commands, session, entry.group());
                    sendUpdate(commands, new UIEventBuilder(), false);
                }
            }

            case "VIS_TOGGLE_GROUP" -> {
                HudComponentRegistry.Group group = parseVisibilityGroup(data.getValue());
                if (group != null) {
                    session.toggleVisibilityGroup(group);
                    rebuildPageContent();
                }
            }

            case "TOGGLE_RULE" -> {
                DynamicHudTriggers rule = DynamicHudTriggers.fromString(data.getValue());
                HudComponentRegistry.HudEntry entry = HudComponentRegistry.find(data.getComponent());

                if (rule != null && entry != null) {
                    session.toggleRule(entry, rule);

                    UICommandBuilder commands = new UICommandBuilder();
                    updateDynamicRuleRow(commands, session, entry, rule);
                    updateDynamicThresholdControls(commands, session, entry);
                    sendUpdate(commands, new UIEventBuilder(), false);
                }
            }

            case "DYN_SET_THRESHOLD" -> {
                HudComponentRegistry.HudEntry entry = HudComponentRegistry.find(data.getComponent());
                if (entry == null || !entry.supportsThreshold() || !session.isDynamicThresholdEnabled(entry)) {
                    return;
                }

                float threshold = Math.max(0f, Math.min(100f, data.getDynamicThreshold()));
                session.setDynamicThreshold(entry, threshold);

                UICommandBuilder commands = new UICommandBuilder();
                updateDynamicThresholdControls(commands, session, entry);
                sendUpdate(commands, new UIEventBuilder(), false);
            }

            case "DYN_REVEAL_MORE" -> {
                String componentKey = data.getComponent();
                HudComponentRegistry.HudEntry entry = HudComponentRegistry.find(componentKey);

                if (entry != null) {
                    session.revealMoreTriggers(componentKey);

                    UICommandBuilder commands = new UICommandBuilder();
                    UIEventBuilder events = new UIEventBuilder();

                    updateDynamicExtraTriggers(commands, events, session, entry);
                    sendUpdate(commands, events, false);
                }
            }

            case "APPLY" -> {
                uiService.apply(playerRef);
                uiService.closePage(ref, store);
            }

            case "CANCEL" -> {
                uiService.discard(playerRef);
                uiService.closePage(ref, store);
            }
        }
    }

    private void rebuildPageContent() {
        HudConfigUiSession session = uiService.getSession(playerRef);
        if (session == null) {
            sendUpdate();
            return;
        }

        UICommandBuilder commands = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();

        commands.clear("#ContentHost");
        bindChromeEvents(events);
        render(commands, events);

        sendUpdate(commands, events, false);
    }

    private void render(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events
    ) {
        HudConfigUiSession session = uiService.getSession(playerRef);

        if (session == null) {
            return;
        }

        renderChrome(commands, session);

        switch (session.getCurrentView()) {
            case PROFILES -> renderProfilesView(commands, events, session);
            case VISIBILITY -> renderVisibilityView(commands, events, session);
            case DYNAMIC_RULES -> renderDynamicRulesView(commands, events, session);
        }
    }

    private void renderChrome(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session
    ) {
        HudConfigView currentView = session.getCurrentView();

        boolean profilesSelected = currentView == HudConfigView.PROFILES;
        boolean visibilityActive = currentView == HudConfigView.VISIBILITY;
        boolean dynamicActive = currentView == HudConfigView.DYNAMIC_RULES;

        commands.set("#ViewProfilesBtnContainer.Visible", !profilesSelected);
        commands.set("#ViewProfilesBtnSelectedContainer.Visible", profilesSelected);

        commands.set("#ViewVisibilityBtnContainer.Visible", !visibilityActive);
        commands.set("#ViewVisibilityBtnSelectedContainer.Visible", visibilityActive);

        commands.set("#ViewDynamicRulesBtnContainer.Visible", !dynamicActive);
        commands.set("#ViewDynamicRulesBtnSelectedContainer.Visible", dynamicActive);

        commands.set("#ApplyButton.Text", "APPLY");
        commands.set("#CancelButton.Text", "CANCEL");
    }

    private void renderProfilesView(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.append("#ContentHost", PROFILES_UI);
        commands.clear("#ProfilesList");

        commands.set(
                "#ViewHelpText.TextSpans",
                Message.raw(session.getCurrentView().helpText())
        );

        List<Profile> profiles = Arrays.stream(Profile.values())
                .filter(profile -> profile != Profile.CUSTOM)
                .toList();

        Profile currentProfile = presenter.resolveCurrentProfile(
                session.getDraftHudComponents(),
                session.getDraftDynamicHud()
        );

        int rowIndex = 0;
        boolean isSelected;

        for (Profile profile : profiles) {
            isSelected = currentProfile == profile;

            commands.append("#ProfilesList", PROFILE_ROW_UI);

            String rowRootSelector = "#ProfilesList[" + rowIndex + "]";
            String labelSelector = rowRootSelector + " #ProfileLabel";
            String labelSelectedSelector = rowRootSelector + " #ProfileSelectedLabel";
            String descriptionSelector = rowRootSelector + " #ProfileDescription";
            String selectProfileButtonSelector = rowRootSelector + " #SelectProfileButton";
            String selectedProfile = rowRootSelector + " #SelectedProfile";

            commands.set(labelSelector + ".TextSpans", Message.raw(profile.label().toUpperCase() + " PROFILE"));
            commands.set(labelSelectedSelector + ".TextSpans", Message.raw(profile.label().toUpperCase() + " PROFILE <APPLIED>"));
            commands.set(labelSelector + ".Visible", !isSelected);
            commands.set(labelSelectedSelector + ".Visible", isSelected);
            commands.set(descriptionSelector + ".TextSpans", Message.raw(profile.description()));
            commands.set(selectProfileButtonSelector + ".Visible", !isSelected);
            commands.set(selectedProfile + ".Visible", isSelected);

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selectProfileButtonSelector,
                    PageEventData.action("SELECT_PROFILE").append("Value", profile.name()),
                    false
            );

            rowIndex++;
        }

        isSelected = currentProfile == Profile.CUSTOM;

        if (isSelected) {
            commands.append("#ProfilesList", PROFILE_ROW_UI);

            String rowRootSelector = "#ProfilesList[" + rowIndex + "]";
            String labelSelector = rowRootSelector + " #ProfileLabel";
            String labelSelectedSelector = rowRootSelector + " #ProfileSelectedLabel";
            String descriptionSelector = rowRootSelector + " #ProfileDescription";
            String selectedProfile = rowRootSelector + " #SelectedProfile";

            commands.set(labelSelector + ".TextSpans", Message.raw(Profile.CUSTOM.label().toUpperCase()));
            commands.set(labelSelectedSelector + ".TextSpans", Message.raw(Profile.CUSTOM.label().toUpperCase() + " PROFILE <APPLIED>"));
            commands.set(descriptionSelector + ".TextSpans", Message.raw(Profile.CUSTOM.description()));

            commands.set(labelSelector + ".Visible", false);
            commands.set(labelSelectedSelector + ".Visible", true);
            commands.set(selectedProfile + ".Visible", true);
        }
    }

    @Nullable
    private HudComponentRegistry.Group parseVisibilityGroup(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return HudComponentRegistry.Group.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void updateVisibilitySection(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponentRegistry.Group group
    ) {
        Integer rowIndex = session.getVisibilitySectionRowIndex(group);
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

    private void updateVisibilityRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull String componentKey
    ) {
        Integer rowIndex = session.getVisibilityRowIndex(componentKey);
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

    private void renderVisibilityView(
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

        session.clearVisibilityRowIndexes();
        session.clearVisibilitySectionRowIndexes();

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

            session.putVisibilitySectionRowIndex(group, rowIndex);

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
                    PageEventData.action("VIS_TOGGLE_GROUP").append("Value", group.name()),
                    false
            );

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    openButtonSelector,
                    PageEventData.action("VIS_TOGGLE_GROUP").append("Value", group.name()),
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
                session.putVisibilityRowIndex(entry.key(), entryRowIndex);

                String rowRootSelector = "#VisibilityList[" + entryRowIndex + "]";
                String componentSelector = rowRootSelector + " #VisibilityComponentLabel";
                String checkBoxSelector = rowRootSelector + " #VisibilityToggleCheckBox";

                commands.set(componentSelector + ".TextSpans", Message.raw(entry.label().toUpperCase()));
                commands.set(checkBoxSelector + ".Value", !hidden);

                events.addEventBinding(
                        CustomUIEventBindingType.ValueChanged,
                        checkBoxSelector,
                        PageEventData.action("TOGGLE_VISIBILITY").append("Value", entry.key()),
                        false
                );

                rowIndex++;
            }
        }
    }

    private void updateDynamicRuleRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponentRegistry.HudEntry entry,
            @Nonnull DynamicHudTriggers trigger
    ) {
        Integer componentIndex = session.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) {
            return;
        }

        boolean enabled = session.isRuleEnabled(entry, trigger);

        Integer baseRowIndex = session.getDynamicRuleRowIndex(entry.key(), "base", trigger);
        if (baseRowIndex != null) {
            String rulesListSelector = "#DynamicComponentsList[" + componentIndex + "] #DynamicRulesList";
            String rowRootSelector = rulesListSelector + "[" + baseRowIndex + "]";
            String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";
            commands.set(checkBoxSelector + ".Value", enabled);
            return;
        }

        Integer extraRowIndex = session.getDynamicRuleRowIndex(entry.key(), "extra", trigger);
        if (extraRowIndex != null) {
            String extraListSelector = "#DynamicComponentsList[" + componentIndex + "] #DynamicExtraTriggersList";
            String rowRootSelector = extraListSelector + "[" + extraRowIndex + "]";
            String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";
            commands.set(checkBoxSelector + ".Value", enabled);
        }
    }

    private void renderDynamicRulesView(
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

        session.clearDynamicRuleRowIndexes();
        session.clearDynamicComponentRowIndexes();

        int componentIndex = 0;

        for (HudComponentRegistry.HudEntry entry : session.getDynamicEntries()) {
            commands.append("#DynamicComponentsList", DYNAMIC_SECTION_UI);
            session.putDynamicComponentRowIndex(entry.key(), componentIndex);

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

    private void renderDynamicRulesList(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponentRegistry.HudEntry entry,
            @Nonnull String rulesListSelector
    ) {
        int rowIndex = 0;

        for (DynamicHudTriggers trigger : session.getBaseRulesInDisplayOrder(entry)) {
            rowIndex = renderDynamicRuleRow(commands, events, session, entry, rulesListSelector, "base", rowIndex, trigger);
        }
    }

    private void renderDynamicExtraTriggers(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponentRegistry.HudEntry entry,
            @Nonnull String componentRoot
    ) {
        List<DynamicHudTriggers> extraRules = session.getExtraRulesInDisplayOrder(entry);
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
                    PageEventData.action("DYN_REVEAL_MORE")
                            .append("Component", entry.key()),
                    false
            );
            return;
        }

        for (DynamicHudTriggers trigger : extraRules) {
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
            @Nonnull HudComponentRegistry.HudEntry entry,
            @Nonnull String hostSelector,
            @Nonnull String hostKey,
            int rowIndex,
            @Nonnull DynamicHudTriggers trigger
    ) {
        boolean enabled = session.isRuleEnabled(entry, trigger);

        commands.append(hostSelector, DYNAMIC_RULE_ROW_UI);
        session.putDynamicRuleRowIndex(entry.key(), hostKey, trigger, rowIndex);

        String rowRootSelector = hostSelector + "[" + rowIndex + "]";
        String labelSelector = rowRootSelector + " #DynamicRuleLabel";
        String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";

        commands.set(
                labelSelector + ".TextSpans",
                Message.raw(DynamicHudTriggers.prettyName(trigger).toUpperCase())
        );
        commands.set(checkBoxSelector + ".Value", enabled);

        events.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                checkBoxSelector,
                PageEventData.action("TOGGLE_RULE")
                        .append("Component", entry.key())
                        .append("Value", trigger.name()),
                false
        );

        return rowIndex + 1;
    }

    private void updateDynamicExtraTriggers(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponentRegistry.HudEntry entry
    ) {
        Integer componentIndex = session.getDynamicComponentRowIndex(entry.key());
        if (componentIndex == null) {
            return;
        }

        String componentRoot = "#DynamicComponentsList[" + componentIndex + "]";
        renderDynamicExtraTriggers(commands, events, session, entry, componentRoot);
    }

    private void updateDynamicThresholdControls(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponentRegistry.HudEntry entry
    ) {
        Integer componentIndex = session.getDynamicComponentRowIndex(entry.key());
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

    private void renderDynamicThresholdControls(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session,
            @Nonnull HudComponentRegistry.HudEntry entry,
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

    private void bindChromeEvents(@Nonnull UIEventBuilder events) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewProfilesBtn",
                PageEventData.action("VIEW_PROFILES"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewProfilesBtnSelected",
                PageEventData.action("VIEW_PROFILES"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewVisibilityBtn",
                PageEventData.action("VIEW_VISIBILITY"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewVisibilityBtnSelected",
                PageEventData.action("VIEW_VISIBILITY"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewDynamicRulesBtn",
                PageEventData.action("VIEW_DYNAMIC_RULES"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ViewDynamicRulesBtnSelected",
                PageEventData.action("VIEW_DYNAMIC_RULES"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ApplyButton",
                PageEventData.action("APPLY"),
                false
        );
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelButton",
                PageEventData.action("CANCEL"),
                false
        );
    }

    public static final class PageEventData {

        public static final BuilderCodec<PageEventData> CODEC =
                BuilderCodec.builder(PageEventData.class, PageEventData::new)
                        .append(new KeyedCodec<>("Action", Codec.STRING),
                                (d, v) -> d.action = v, d -> d.action)
                        .add()
                        .append(new KeyedCodec<>("Value", Codec.STRING),
                                (d, v) -> d.value = v, d -> d.value)
                        .add()
                        .append(new KeyedCodec<>("Component", Codec.STRING),
                                (d, v) -> d.component = v, d -> d.component)
                        .add()
                        .append(new KeyedCodec<>("@DynamicThreshold", Codec.FLOAT),
                                (d, v) -> d.dynamicThreshold = v, d -> d.dynamicThreshold)
                        .add()
                        .build();

        private String action = "";
        private String value = "";
        private String component = "";
        private float dynamicThreshold;

        public String getAction() {
            return action;
        }

        public String getValue() {
            return value;
        }

        public String getComponent() {
            return component;
        }

        public float getDynamicThreshold() {
            return dynamicThreshold;
        }

        public static EventData action(@Nonnull String action) {
            return EventData.of("Action", action);
        }
    }
}