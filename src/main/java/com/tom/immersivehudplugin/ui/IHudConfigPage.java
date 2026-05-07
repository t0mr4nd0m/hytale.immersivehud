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
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.profiles.Profile;

import javax.annotation.Nonnull;

public final class IHudConfigPage extends InteractiveCustomUIPage<IHudConfigPage.PageEventData> {

    private static final String PAGE_UI = "IHudConfigPage.ui";

    private final HudConfigUiService uiService;
    private final PlayerRef playerRef;

    private final HudConfigProfilesRenderer profilesRenderer;
    private final HudConfigVisibilityRenderer visibilityRenderer;

    public IHudConfigPage(
            @Nonnull HudConfigUiService uiService,
            @Nonnull PlayerRef playerRef
    ) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PageEventData.CODEC);
        this.uiService = uiService;
        this.playerRef = playerRef;
        this.profilesRenderer = new HudConfigProfilesRenderer(new HudConfigPresenter());
        this.visibilityRenderer = new HudConfigVisibilityRenderer();
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
    public void onDismiss(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store
    ) {
        uiService.discard(playerRef);
        uiService.resumeWorld(ref, store);
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

            case "VIS_SELECT_COMPONENT" -> {
                session.selectVisibilityComponent(data.getValue());
                rebuildPageContent();
            }

            case "VIS_TOGGLE_RULE" -> {
                HudTrigger rule = HudTrigger.fromString(data.getValue());
                HudComponent entry = HudComponentRegistry.find(data.getComponent());

                if (rule == null || entry == null) {
                    return;
                }

                session.toggleRule(entry, rule);

                UICommandBuilder commands = new UICommandBuilder();
                UIEventBuilder events = new UIEventBuilder();

                if (session.shouldRebuildTriggerListAfterRuleToggle()) {
                    visibilityRenderer.updateDynamicDetail(commands, events, session, entry);
                } else {
                    visibilityRenderer.updateDynamicRuleRow(commands, session, entry, rule);
                    visibilityRenderer.updateDynamicHeader(commands, session, entry);
                }

                sendUpdate(commands, events, false);
            }

            case "VIS_SET_THRESHOLD" -> {
                HudComponent entry = HudComponentRegistry.find(data.getComponent());
                if (entry == null
                        || !entry.supportsThreshold()
                        || !session.isDynamicThresholdEnabled(entry)
                        || !session.isHidden(entry)) {
                    return;
                }

                float threshold = Math.max(0f, Math.min(100f, data.getDynamicThreshold()));
                session.setDynamicThreshold(entry, threshold);

                sendUpdate(new UICommandBuilder(), new UIEventBuilder(), false);
            }

            case "VIS_TOGGLE_VISIBILITY" -> {

                HudComponent entry = session.getSelectedVisibilityComponent();
                if (entry == null) {
                    return;
                }

                session.toggleVisibility(entry);

                UICommandBuilder commands = new UICommandBuilder();
                UIEventBuilder events = new UIEventBuilder();

                visibilityRenderer.updateDynamicDetail(commands, events, session, entry);
                sendUpdate(commands, events, false);
            }

            case "VIS_TOGGLE_STATIC_VISIBILITY" -> {
                HudComponent entry = HudComponentRegistry.find(data.getComponent());
                if (entry == null) {
                    return;
                }

                session.toggleVisibility(entry);

                UICommandBuilder commands = new UICommandBuilder();
                visibilityRenderer.updateStaticRow(commands, session, entry);
                sendUpdate(commands, new UIEventBuilder(), false);
            }

            case "VIS_TOGGLE_TRIGGER_FILTER" -> {
                HudComponent entry = session.getSelectedVisibilityComponent();
                if (entry == null) return;

                session.toggleShowOnlyCheckedTriggers();

                UICommandBuilder commands = new UICommandBuilder();
                UIEventBuilder events = new UIEventBuilder();

                visibilityRenderer.updateDynamicDetail(commands, events, session, entry);
                sendUpdate(commands, events, false);
            }

            case "VIS_CLEAR_TRIGGERS" -> {
                HudComponent entry = session.getSelectedVisibilityComponent();
                if (entry == null) {
                    return;
                }

                session.clearRules(entry);

                UICommandBuilder commands = new UICommandBuilder();
                UIEventBuilder events = new UIEventBuilder();

                visibilityRenderer.updateDynamicDetail(commands, events, session, entry);

                sendUpdate(commands, events, false);
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
        }
    }

    private void renderChrome(
            @Nonnull UICommandBuilder commands,
            @Nonnull HudConfigUiSession session
    ) {
        HudConfigView currentView = session.getCurrentView();

        boolean profilesSelected = currentView == HudConfigView.PROFILES;
        boolean visibilitySelected = currentView == HudConfigView.VISIBILITY;

        commands.set("#ViewProfilesBtn.Disabled", profilesSelected);
        commands.set("#ViewVisibilityBtn.Disabled", visibilitySelected);

        commands.set("#ApplyButton.Text", "APPLY");
        commands.set("#CancelButton.Text", "CANCEL");
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
                "#ViewVisibilityBtn",
                PageEventData.action("VIEW_VISIBILITY"),
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