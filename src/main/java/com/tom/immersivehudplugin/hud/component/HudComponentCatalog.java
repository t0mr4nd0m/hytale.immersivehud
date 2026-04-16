package com.tom.immersivehudplugin.hud.component;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.AmmoIndicator;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.BlockVariantSelector;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.BuilderToolsLegend;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.BuilderToolsMaterialSlotSelector;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Chat;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Compass;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.EventTitle;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Health;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Hotbar;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.InputBindings;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.KillFeed;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Mana;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Notifications;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.ObjectivePanel;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Oxygen;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.PlayerList;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.PortalPanel;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Requests;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Reticle;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Sleep;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Speedometer;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.Stamina;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.StatusIcons;
import static com.hypixel.hytale.protocol.packets.interface_.HudComponent.UtilitySlotSelector;

final class HudComponentCatalog {
    private HudComponentCatalog() {
    }

    static List<HudComponent> createAll() {
        return List.of(
                dynamicComponent(
                        "health",
                        "Health",
                        HudComponentRegistry.Group.BARS,
                        Health,
                        HudComponentsConfig::isHideHealthHud,
                        HudComponentsConfig::setHideHealthHud,
                        DynamicHudConfig::getHealth,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.HEALTH_NOT_FULL
                        ),
                        EnumSet.of(HudTrigger.HEALTH_NOT_FULL),
                        100f
                ),
                dynamicComponent(
                        "stamina",
                        "Stamina",
                        HudComponentRegistry.Group.BARS,
                        Stamina,
                        HudComponentsConfig::isHideStaminaHud,
                        HudComponentsConfig::setHideStaminaHud,
                        DynamicHudConfig::getStamina,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.STAMINA_NOT_FULL
                        ),
                        EnumSet.of(HudTrigger.STAMINA_NOT_FULL),
                        100f
                ),
                dynamicComponent(
                        "mana",
                        "Mana",
                        HudComponentRegistry.Group.BARS,
                        Mana,
                        HudComponentsConfig::isHideManaHud,
                        HudComponentsConfig::setHideManaHud,
                        DynamicHudConfig::getMana,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.MANA_NOT_FULL
                        ),
                        EnumSet.of(HudTrigger.MANA_NOT_FULL),
                        100f
                ),
                dynamicComponent(
                        "oxygen",
                        "Oxygen",
                        HudComponentRegistry.Group.BARS,
                        Oxygen,
                        HudComponentsConfig::isHideOxygenHud,
                        HudComponentsConfig::setHideOxygenHud,
                        DynamicHudConfig::getOxygen,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.OXYGEN_NOT_FULL
                        ),
                        EnumSet.of(HudTrigger.OXYGEN_NOT_FULL),
                        100f
                ),
                dynamicComponent(
                        "compass",
                        "Compass",
                        HudComponentRegistry.Group.CORE,
                        Compass,
                        HudComponentsConfig::isHideCompassHud,
                        HudComponentsConfig::setHideCompassHud,
                        DynamicHudConfig::getCompass,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.PLAYER_MOVING,
                                HudTrigger.PLAYER_WALKING,
                                HudTrigger.PLAYER_RUNNING,
                                HudTrigger.PLAYER_SPRINTING,
                                HudTrigger.PLAYER_MOUNTING,
                                HudTrigger.PLAYER_SWIMMING,
                                HudTrigger.PLAYER_FLYING,
                                HudTrigger.PLAYER_GLIDING,
                                HudTrigger.PLAYER_JUMPING,
                                HudTrigger.PLAYER_CROUCHING,
                                HudTrigger.PLAYER_CLIMBING,
                                HudTrigger.PLAYER_FALLING,
                                HudTrigger.PLAYER_ROLLING,
                                HudTrigger.PLAYER_IDLE,
                                HudTrigger.PLAYER_SITTING,
                                HudTrigger.PLAYER_SLEEPING,
                                HudTrigger.PLAYER_IN_FLUID,
                                HudTrigger.PLAYER_ON_GROUND
                        ),
                        EnumSet.of(HudTrigger.PLAYER_MOVING),
                        null
                ),
                dynamicComponent(
                        "hotbar",
                        "Hotbar",
                        HudComponentRegistry.Group.CORE,
                        Hotbar,
                        HudComponentsConfig::isHideHotbarHud,
                        HudComponentsConfig::setHideHotbarHud,
                        DynamicHudConfig::getHotbar,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.CONSUMABLE_USE,
                                HudTrigger.HOLDING_MELEE_WEAPON,
                                HudTrigger.HOLDING_RANGED_WEAPON
                        ),
                        EnumSet.of(HudTrigger.HOTBAR_INPUT),
                        null
                ),
                dynamicComponent(
                        "reticle",
                        "Reticle",
                        HudComponentRegistry.Group.CORE,
                        Reticle,
                        HudComponentsConfig::isHideReticleHud,
                        HudComponentsConfig::setHideReticleHud,
                        DynamicHudConfig::getReticle,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.CHARGING_WEAPON,
                                HudTrigger.CONSUMABLE_USE,
                                HudTrigger.TARGET_ENTITY,
                                HudTrigger.INTERACTABLE_BLOCK,
                                HudTrigger.HOLDING_RANGED_WEAPON,
                                HudTrigger.HOLDING_MELEE_WEAPON,
                                HudTrigger.BLOCKING_ATTACK
                        ),
                        EnumSet.of(
                                HudTrigger.CHARGING_WEAPON,
                                HudTrigger.CONSUMABLE_USE,
                                HudTrigger.TARGET_ENTITY,
                                HudTrigger.INTERACTABLE_BLOCK,
                                HudTrigger.HOLDING_RANGED_WEAPON
                        ),
                        null
                ),
                dynamicComponent(
                        "statusicons",
                        "Status Icons",
                        HudComponentRegistry.Group.UI,
                        StatusIcons,
                        HudComponentsConfig::isHideStatusIconsHud,
                        HudComponentsConfig::setHideStatusIconsHud,
                        DynamicHudConfig::getStatusIcons,
                        EnumSet.of(HudTrigger.HOTBAR_INPUT),
                        EnumSet.of(HudTrigger.HOTBAR_INPUT),
                        null
                ),
                staticComponent(
                        "inputbindings",
                        "Input Bindings",
                        HudComponentRegistry.Group.UI,
                        InputBindings,
                        HudComponentsConfig::isHideInputBindingsHud,
                        HudComponentsConfig::setHideInputBindingsHud,
                        true
                ),
                staticComponent(
                        "ammo",
                        "Ammo Indicator",
                        HudComponentRegistry.Group.UI,
                        AmmoIndicator,
                        HudComponentsConfig::isHideAmmoIndicatorHud,
                        HudComponentsConfig::setHideAmmoIndicatorHud,
                        false
                ),
                staticComponent(
                        "notifications",
                        "Notifications",
                        HudComponentRegistry.Group.UI,
                        Notifications,
                        HudComponentsConfig::isHideNotificationsHud,
                        HudComponentsConfig::setHideNotificationsHud,
                        true
                ),
                staticComponent(
                        "speedometer",
                        "Speedometer",
                        HudComponentRegistry.Group.UI,
                        Speedometer,
                        HudComponentsConfig::isHideSpeedometerHud,
                        HudComponentsConfig::setHideSpeedometerHud,
                        false
                ),
                staticComponent(
                        "utilityslotselector",
                        "Utility Slot Selector",
                        HudComponentRegistry.Group.UI,
                        UtilitySlotSelector,
                        HudComponentsConfig::isHideUtilitySlotSelectorHud,
                        HudComponentsConfig::setHideUtilitySlotSelectorHud,
                        false
                ),
                staticComponent(
                        "chat",
                        "Chat",
                        HudComponentRegistry.Group.SOCIAL,
                        Chat,
                        HudComponentsConfig::isHideChatHud,
                        HudComponentsConfig::setHideChatHud,
                        false
                ),
                staticComponent(
                        "requests",
                        "Requests",
                        HudComponentRegistry.Group.SOCIAL,
                        Requests,
                        HudComponentsConfig::isHideRequestsHud,
                        HudComponentsConfig::setHideRequestsHud,
                        false
                ),
                staticComponent(
                        "killfeed",
                        "Kill Feed",
                        HudComponentRegistry.Group.SOCIAL,
                        KillFeed,
                        HudComponentsConfig::isHideKillFeedHud,
                        HudComponentsConfig::setHideKillFeedHud,
                        false
                ),
                staticComponent(
                        "playerlist",
                        "Player List",
                        HudComponentRegistry.Group.SOCIAL,
                        PlayerList,
                        HudComponentsConfig::isHidePlayerListHud,
                        HudComponentsConfig::setHidePlayerListHud,
                        false
                ),
                staticComponent(
                        "eventtitle",
                        "Event Title",
                        HudComponentRegistry.Group.PANELS,
                        EventTitle,
                        HudComponentsConfig::isHideEventTitleHud,
                        HudComponentsConfig::setHideEventTitleHud,
                        false
                ),
                staticComponent(
                        "objectivepanel",
                        "Objective Panel",
                        HudComponentRegistry.Group.PANELS,
                        ObjectivePanel,
                        HudComponentsConfig::isHideObjectivePanelHud,
                        HudComponentsConfig::setHideObjectivePanelHud,
                        false
                ),
                staticComponent(
                        "portalpanel",
                        "Portal Panel",
                        HudComponentRegistry.Group.PANELS,
                        PortalPanel,
                        HudComponentsConfig::isHidePortalPanelHud,
                        HudComponentsConfig::setHidePortalPanelHud,
                        false
                ),
                staticComponent(
                        "sleep",
                        "Sleep",
                        HudComponentRegistry.Group.PANELS,
                        Sleep,
                        HudComponentsConfig::isHideSleepHud,
                        HudComponentsConfig::setHideSleepHud,
                        false
                ),
                staticComponent(
                        "blockvariantselector",
                        "Block Variant Selector",
                        HudComponentRegistry.Group.BUILDER,
                        BlockVariantSelector,
                        HudComponentsConfig::isHideBlockVariantSelectorHud,
                        HudComponentsConfig::setHideBlockVariantSelectorHud,
                        false
                ),
                staticComponent(
                        "buildertoolslegend",
                        "Builder Tools Legend",
                        HudComponentRegistry.Group.BUILDER,
                        BuilderToolsLegend,
                        HudComponentsConfig::isHideBuilderToolsLegendHud,
                        HudComponentsConfig::setHideBuilderToolsLegendHud,
                        false
                ),
                staticComponent(
                        "buildertoolsmaterialslotselector",
                        "Builder Tools Material Slot Selector",
                        HudComponentRegistry.Group.BUILDER,
                        BuilderToolsMaterialSlotSelector,
                        HudComponentsConfig::isHideBuilderToolsMaterialSlotSelectorHud,
                        HudComponentsConfig::setHideBuilderToolsMaterialSlotSelectorHud,
                        false
                )
        );
    }

    private static HudComponent dynamicComponent(
            String key,
            String label,
            HudComponentRegistry.Group group,
            com.hypixel.hytale.protocol.packets.interface_.HudComponent hudComponent,
            HudComponentRegistry.BoolGetter<HudComponentsConfig> staticGetter,
            HudComponentRegistry.BoolSetter<HudComponentsConfig> staticSetter,
            Function<DynamicHudConfig, DynamicHudRuleConfig> dynamicGetter,
            EnumSet<HudTrigger> allowedRules,
            EnumSet<HudTrigger> defaultRules,
            @Nullable Float defaultThreshold
    ) {
        return new HudComponent(
                key,
                label,
                group,
                hudComponent,
                staticGetter,
                staticSetter,
                dynamicGetter,
                true,
                allowedRules,
                defaultRules,
                defaultThreshold
        );
    }

    private static HudComponent staticComponent(
            String key,
            String label,
            HudComponentRegistry.Group group,
            com.hypixel.hytale.protocol.packets.interface_.HudComponent hudComponent,
            HudComponentRegistry.BoolGetter<HudComponentsConfig> staticGetter,
            HudComponentRegistry.BoolSetter<HudComponentsConfig> staticSetter,
            boolean defaultHidden
    ) {
        return new HudComponent(
                key,
                label,
                group,
                hudComponent,
                staticGetter,
                staticSetter,
                null,
                defaultHidden,
                EnumSet.noneOf(HudTrigger.class),
                EnumSet.noneOf(HudTrigger.class),
                null
        );
    }
}