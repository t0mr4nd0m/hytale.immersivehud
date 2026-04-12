package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HudConfigPage extends InteractiveCustomUIPage<HudConfigPage.PageEventData> {

    private static final String PAGE_UI = "Pages/ImmersiveHud/HudConfigPage.ui";

    private final HudConfigUiService uiService;
    private final PlayerRef playerRef;
    private final HudConfigPresenter presenter = new HudConfigPresenter();
    private final HudConfigRenderIndex renderIndex = new HudConfigRenderIndex();

    private final HudConfigVisibilityRenderer visibilityRenderer;
    private final HudConfigDynamicRulesRenderer dynamicRulesRenderer;
    private final HudConfigProfilesRenderer profilesRenderer;

    public HudConfigPage(
            @Nonnull HudConfigUiService uiService,
            @Nonnull PlayerRef playerRef
    ) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.uiService = uiService;
        this.playerRef = playerRef;
        this.visibilityRenderer = new HudConfigVisibilityRenderer(presenter, renderIndex);
        this.dynamicRulesRenderer = new HudConfigDynamicRulesRenderer(renderIndex);
        this.profilesRenderer = new HudConfigProfilesRenderer(presenter);
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
                    HudComponent entry = HudComponentRegistry.find(data.getValue());
                    if (entry == null) {
                        return;
                    }

                    session.toggleVisibility(data.getValue());

                    UICommandBuilder commands = new UICommandBuilder();
                    visibilityRenderer.updateVisibilityRow(commands, session, data.getValue());
                    visibilityRenderer.updateVisibilitySection(commands, session, entry.group());
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
                HudTrigger rule = HudTrigger.fromString(data.getValue());
                HudComponent entry = HudComponentRegistry.find(data.getComponent());

                if (rule != null && entry != null) {
                    session.toggleRule(entry, rule);

                    UICommandBuilder commands = new UICommandBuilder();
                    dynamicRulesRenderer.updateDynamicRuleRow(commands, session, entry, rule);
                    dynamicRulesRenderer.updateDynamicThresholdControls(commands, session, entry);
                    sendUpdate(commands, new UIEventBuilder(), false);
                }
            }

            case "DYN_SET_THRESHOLD" -> {
                HudComponent entry = HudComponentRegistry.find(data.getComponent());
                if (entry == null || !entry.supportsThreshold() || !session.isDynamicThresholdEnabled(entry)) {
                    return;
                }

                float threshold = Math.max(0f, Math.min(100f, data.getDynamicThreshold()));
                session.setDynamicThreshold(entry, threshold);

                UICommandBuilder commands = new UICommandBuilder();
                dynamicRulesRenderer.updateDynamicThresholdControls(commands, session, entry);
                sendUpdate(commands, new UIEventBuilder(), false);
            }

            case "DYN_REVEAL_MORE" -> {
                String componentKey = data.getComponent();
                HudComponent entry = HudComponentRegistry.find(componentKey);

                if (entry != null) {
                    session.revealMoreTriggers(componentKey);

                    UICommandBuilder commands = new UICommandBuilder();
                    UIEventBuilder events = new UIEventBuilder();

                    dynamicRulesRenderer.updateDynamicExtraTriggers(commands, events, session, entry);
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

        renderIndex.clearAll();

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
            case PROFILES -> profilesRenderer.renderProfilesView(commands, events, session);
            case VISIBILITY -> visibilityRenderer.renderVisibilityView(commands, events, session);
            case DYNAMIC_RULES -> dynamicRulesRenderer.renderDynamicRulesView(commands, events, session);
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