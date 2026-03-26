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
import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggerCategory;
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
    private static final String VISIBILITY_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigVisibilityRow.ui";
    private static final String VISIBILITY_SECTION_UI = "Pages/ImmersiveHud/Views/HudConfigVisibilitySection.ui";

    private static final String DYNAMIC_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRulesView.ui";
    private static final String DYNAMIC_RULE_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRuleRow.ui";
    private static final String DYNAMIC_RULE_SECTION_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRuleSection.ui";
    private static final String DYNAMIC_RULE_TAG_UI = "Pages/ImmersiveHud/Views/HudConfigDynamicRuleTag.ui";

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final ImmersiveHudPlugin plugin;
    private final HudConfigUiService uiService;
    private final PlayerRef playerRef;

    public HudConfigPage(
            @Nonnull ImmersiveHudPlugin plugin,
            @Nonnull HudConfigUiService uiService,
            @Nonnull PlayerRef playerRef
    ) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.plugin = plugin;
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

            case "DYN_TOGGLE_CATEGORY" -> {
                DynamicHudTriggerCategory category = parseDynamicCategory(data.getValue());
                if (category != null) {
                    session.toggleDynamicCategory(category);
                    rebuildPageContent();
                }
            }

            case "DYN_PREV_COMPONENT" -> {
                session.previousDynamicEntry();
                rebuildPageContent();
            }

            case "DYN_NEXT_COMPONENT" -> {
                session.nextDynamicEntry();
                rebuildPageContent();
            }

            case "TOGGLE_RULE" -> {
                DynamicHudTriggers rule = DynamicHudTriggers.fromString(data.getValue());
                if (rule != null) {
                    session.toggleRule(rule);

                    UICommandBuilder commands = new UICommandBuilder();
                    updateDynamicRuleRow(commands, session, rule);
                    updateDynamicCategorySection(commands, session, rule.category());
                    renderDynamicCategoryTags(commands, session, rule.category());
                    sendUpdate(commands, new UIEventBuilder(), false);
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

        if (session == null) { return; }

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

        commands.set("#ViewHelpText.TextSpans",
                Message.raw(session.getCurrentView().helpText()));

        List<Profile> profiles = Arrays.stream(Profile.values())
                .filter(profile -> profile != Profile.CUSTOM)
                .toList();

        int rowIndex = 0;
        boolean isSelected;

        for (Profile profile : profiles) {

            isSelected = session.getSelectedProfile() == profile;

            commands.append("#ProfilesList", PROFILE_ROW_UI);

            String rowRootSelector = "#ProfilesList[" + rowIndex + "]";
            String labelSelector = rowRootSelector + " #ProfileLabel";
            String labelSelectedSelector = rowRootSelector + " #ProfileSelectedLabel";
            String descriptionSelector = rowRootSelector + " #ProfileDescription";
            String applyButtonSelector = rowRootSelector + " #ProfileApplyButton";

            commands.set(labelSelector + ".TextSpans", Message.raw(profile.label().toUpperCase() + " PROFILE"));
            commands.set(labelSelectedSelector + ".TextSpans", Message.raw(profile.label().toUpperCase() + " PROFILE APPLIED"));
            commands.set(labelSelector + ".Visible", !isSelected);
            commands.set(labelSelectedSelector + ".Visible", isSelected);
            commands.set(descriptionSelector + ".TextSpans", Message.raw(profile.description()));
            commands.set(applyButtonSelector + ".Visible", !isSelected);

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    applyButtonSelector,
                    PageEventData.action("SELECT_PROFILE").append("Value", profile.name()),
                    false
            );

            rowIndex++;
        }

        isSelected = session.getSelectedProfile() == Profile.CUSTOM;

        if  (isSelected) {
            commands.append("#ProfilesList", PROFILE_ROW_UI);

            String rowRootSelector = "#ProfilesList[" + rowIndex + "]";
            String labelSelector = rowRootSelector + " #ProfileLabel";
            String labelSelectedSelector = rowRootSelector + " #ProfileSelectedLabel";
            String descriptionSelector = rowRootSelector + " #ProfileDescription";
            String applyButtonSelector = rowRootSelector + " #ProfileApplyButton";

            commands.set(labelSelector + ".TextSpans", Message.raw(Profile.CUSTOM.label().toUpperCase()));
            commands.set(labelSelectedSelector + ".TextSpans", Message.raw(Profile.CUSTOM.label().toUpperCase() + " PROFILE APPLIED"));
            commands.set(descriptionSelector + ".TextSpans", Message.raw(Profile.CUSTOM.description()));

            commands.set(applyButtonSelector + ".Visible", false);
            commands.set(labelSelector + ".Visible", false);
            commands.set(labelSelectedSelector + ".Visible", true);
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

        String groupCounter = session.getVisibilityGroupCounterLabel(group);

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
            String closedSymbolSelector = sectionRootSelector + " #VisibilitySectionSymbolClosed";

            String openContentSelector = sectionRootSelector + " #VisibilitySectionContentOpen";
            String openGroupSelector = sectionRootSelector + " #VisibilitySectionGroupOpen";
            String openCounterSelector = sectionRootSelector + " #VisibilitySectionCounterOpen";
            String openSymbolSelector = sectionRootSelector + " #VisibilitySectionSymbolOpen";

            session.putVisibilitySectionRowIndex(group, rowIndex);

            boolean expanded = group == expandedGroup;

            String groupTitle = group.label().toUpperCase();
            String groupCounter = session.getVisibilityGroupCounterLabel(group);

            commands.set(closedGroupSelector + ".TextSpans", Message.raw(groupTitle));
            commands.set(openGroupSelector + ".TextSpans", Message.raw(groupTitle));

            commands.set(closedCounterSelector + ".TextSpans", Message.raw(groupCounter));
            commands.set(openCounterSelector + ".TextSpans", Message.raw(groupCounter));

            commands.set(closedSymbolSelector + ".TextSpans", Message.raw(">"));
            commands.set(openSymbolSelector + ".TextSpans", Message.raw("v"));

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

    private void updateDynamicCategorySection(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull DynamicHudTriggerCategory category
    ) {
        Integer rowIndex = session.getDynamicCategoryRowIndex(category);
        if (rowIndex == null) {
            return;
        }

        String sectionRootSelector = "#DynamicRulesList[" + rowIndex + "]";
        String closedCounterSelector = sectionRootSelector + " #DynamicRuleSectionCounterClosed";
        String openCounterSelector = sectionRootSelector + " #DynamicRuleSectionCounterOpen";

        String categoryCounter = session.getDynamicCategoryCounterLabel(category);

        commands.set(closedCounterSelector + ".TextSpans", Message.raw(categoryCounter));
        commands.set(openCounterSelector + ".TextSpans", Message.raw(categoryCounter));
    }

    private void updateDynamicRuleRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull DynamicHudTriggers trigger
    ) {
        Integer rowIndex = session.getDynamicRuleRowIndex(trigger);
        if (rowIndex == null) {
            return;
        }

        boolean enabled = session.isRuleEnabled(trigger);

        String rowRootSelector = "#DynamicRulesList[" + rowIndex + "]";
        String checkBoxSelector = rowRootSelector + " #DynamicRuleCheckBox";

        commands.set(checkBoxSelector + ".Value", enabled);
    }

    @Nullable
    private DynamicHudTriggerCategory parseDynamicCategory(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return DynamicHudTriggerCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void renderDynamicCategoryTags(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session,
            @Nonnull DynamicHudTriggerCategory category
    ) {
        Integer rowIndex = session.getDynamicCategoryRowIndex(category);
        if (rowIndex == null) {
            return;
        }

        String tagsHostSelector = "#DynamicRulesList[" + rowIndex + "] #DynamicRuleSectionTagsHost";
        commands.clear(tagsHostSelector);

        List<DynamicHudTriggers> selectedRules = session.getCurrentSelectedRulesInDisplayOrder().stream()
                .filter(trigger -> trigger.category() == category)
                .toList();

        if (selectedRules.isEmpty()) {
            commands.append(tagsHostSelector, DYNAMIC_RULE_TAG_UI);
            return;
        }

        for (int i = 0; i < selectedRules.size(); i++) {
            DynamicHudTriggers trigger = selectedRules.get(i);

            commands.append(tagsHostSelector, DYNAMIC_RULE_TAG_UI);

            String rowRootSelector = tagsHostSelector + "[" + i + "]";
            String labelSelector = rowRootSelector + " #DynamicRuleTagLabel";

            commands.set(
                    labelSelector + ".TextSpans",
                    Message.raw(DynamicHudTriggers.prettyName(trigger).toUpperCase())
            );
        }
    }

    private void renderDynamicRulesView(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.append("#ContentHost", DYNAMIC_UI);
        commands.clear("#DynamicRulesList");

        commands.set(
                "#ViewHelpText.TextSpans",
                Message.raw(session.getCurrentView().helpText())
        );

        session.clearDynamicRuleRowIndexes();
        session.clearDynamicCategoryRowIndexes();

        HudComponentRegistry.HudEntry entry = session.currentDynamicEntry();
        commands.set("#DynamicComponentValueLabel.TextSpans", Message.raw(entry.label().toUpperCase()));

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DynamicPrevComponentButton",
                PageEventData.action("DYN_PREV_COMPONENT"),
                false
        );

        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#DynamicNextComponentButton",
                PageEventData.action("DYN_NEXT_COMPONENT"),
                false
        );

        int rowIndex = 0;
        DynamicHudTriggerCategory expandedCategory = session.getExpandedDynamicCategory();

        for (DynamicHudTriggerCategory category : DynamicHudTriggers.displayCategoryOrder()) {
            List<DynamicHudTriggers> triggers = Arrays.stream(DynamicHudTriggers.values())
                    .filter(t -> t.category() == category)
                    .toList();

            if (triggers.isEmpty()) {
                continue;
            }

            commands.append("#DynamicRulesList", DYNAMIC_RULE_SECTION_UI);

            String sectionRootSelector = "#DynamicRulesList[" + rowIndex + "]";
            String closedButtonSelector = sectionRootSelector + " #DynamicRuleSectionButtonClosed";
            String openButtonSelector = sectionRootSelector + " #DynamicRuleSectionButtonOpen";

            String closedContentSelector = sectionRootSelector + " #DynamicRuleSectionContentClosed";
            String closedGroupSelector = sectionRootSelector + " #DynamicRuleSectionGroupClosed";
            String closedSymbolSelector = sectionRootSelector + " #DynamicRuleSectionSymbolClosed";
            String closedCounterSelector = sectionRootSelector + " #DynamicRuleSectionCounterClosed";

            String openContentSelector = sectionRootSelector + " #DynamicRuleSectionContentOpen";
            String openGroupSelector = sectionRootSelector + " #DynamicRuleSectionGroupOpen";
            String openCounterSelector = sectionRootSelector + " #DynamicRuleSectionCounterOpen";
            String openSymbolSelector = sectionRootSelector + " #DynamicRuleSectionSymbolOpen";

            session.putDynamicCategoryRowIndex(category, rowIndex);

            boolean expanded = category == expandedCategory;

            String categoryTitle = category.label().toUpperCase() + " TRIGGERS";
            String categoryCounter = session.getDynamicCategoryCounterLabel(category);

            commands.set(closedGroupSelector + ".TextSpans", Message.raw(categoryTitle));
            commands.set(openGroupSelector + ".TextSpans", Message.raw(categoryTitle));

            commands.set(closedCounterSelector + ".TextSpans", Message.raw(categoryCounter));
            commands.set(openCounterSelector + ".TextSpans", Message.raw(categoryCounter));

            commands.set(closedSymbolSelector + ".TextSpans", Message.raw(">"));
            commands.set(openSymbolSelector + ".TextSpans", Message.raw("v"));

            commands.set(closedButtonSelector + ".Visible", !expanded);
            commands.set(openButtonSelector + ".Visible", expanded);

            commands.set(closedContentSelector + ".Visible", !expanded);
            commands.set(openContentSelector + ".Visible", expanded);

            renderDynamicCategoryTags(commands, session, category);

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    closedButtonSelector,
                    PageEventData.action("DYN_TOGGLE_CATEGORY").append("Value", category.name()),
                    false
            );

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    openButtonSelector,
                    PageEventData.action("DYN_TOGGLE_CATEGORY").append("Value", category.name()),
                    false
            );

            rowIndex++;

            if (!expanded) {
                continue;
            }

            for (DynamicHudTriggers trigger : triggers) {
                boolean enabled = session.isRuleEnabled(trigger);

                commands.append("#DynamicRulesList", DYNAMIC_RULE_ROW_UI);

                int triggerRowIndex = rowIndex;
                session.putDynamicRuleRowIndex(trigger, triggerRowIndex);

                String rowRootSelector = "#DynamicRulesList[" + triggerRowIndex + "]";
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
                        PageEventData.action("TOGGLE_RULE").append("Value", trigger.name()),
                        false
                );

                rowIndex++;
            }
        }
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
                        .build();

        private String action = "";
        private String value = "";

        public String getAction() {
            return action;
        }

        public String getValue() {
            return value;
        }

        public static EventData action(@Nonnull String action) {
            return EventData.of("Action", action);
        }
    }
}