package com.tom.immersivehudplugin.hud.component;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.EnumSet;
import java.util.List;

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

    private HudComponentCatalog() {}

    static List<HudComponent> createAll() {

        return List.of(
                new HudComponent(
                        "health", "Health",
                        HudComponentRegistry.Group.BARS,
                        Health,
                        "HideHealth",
                        "Health",
                        HudComponentsConfig::isHideHealthHud,
                        HudComponentsConfig::setHideHealthHud,
                        DynamicHudConfig::getHealth,
                        true,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.HEALTH_NOT_FULL
                        ),
                        EnumSet.of(HudTrigger.HEALTH_NOT_FULL),
                        100f
                ),
                new HudComponent(
                        "stamina", "Stamina",
                        HudComponentRegistry.Group.BARS,
                        Stamina,
                        "HideStamina",
                        "Stamina",
                        HudComponentsConfig::isHideStaminaHud,
                        HudComponentsConfig::setHideStaminaHud,
                        DynamicHudConfig::getStamina,
                        true,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.STAMINA_NOT_FULL
                        ),
                        EnumSet.of(HudTrigger.STAMINA_NOT_FULL),
                        100f
                ),
                new HudComponent(
                        "mana", "Mana",
                        HudComponentRegistry.Group.BARS,
                        Mana,
                        "HideMana",
                        "Mana",
                        HudComponentsConfig::isHideManaHud,
                        HudComponentsConfig::setHideManaHud,
                        DynamicHudConfig::getMana,
                        true,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.MANA_NOT_FULL
                        ),
                        EnumSet.of(HudTrigger.MANA_NOT_FULL),
                        100f
                ),
                new HudComponent(
                        "oxygen", "Oxygen",
                        HudComponentRegistry.Group.BARS,
                        Oxygen,
                        "HideOxygen",
                        "Oxygen",
                        HudComponentsConfig::isHideOxygenHud,
                        HudComponentsConfig::setHideOxygenHud,
                        DynamicHudConfig::getOxygen,
                        true,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.OXYGEN_NOT_FULL
                        ),
                        EnumSet.of(HudTrigger.OXYGEN_NOT_FULL),
                        100f
                ),
                new HudComponent(
                        "compass", "Compass",
                        HudComponentRegistry.Group.CORE,
                        Compass,
                        "HideCompass",
                        "Compass",
                        HudComponentsConfig::isHideCompassHud,
                        HudComponentsConfig::setHideCompassHud,
                        DynamicHudConfig::getCompass,
                        true,
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
                new HudComponent(
                        "hotbar", "Hotbar",
                        HudComponentRegistry.Group.CORE,
                        Hotbar,
                        "HideHotbar",
                        "Hotbar",
                        HudComponentsConfig::isHideHotbarHud,
                        HudComponentsConfig::setHideHotbarHud,
                        DynamicHudConfig::getHotbar,
                        true,
                        EnumSet.of(
                                HudTrigger.HOTBAR_INPUT,
                                HudTrigger.CONSUMABLE_USE,
                                HudTrigger.HOLDING_MELEE_WEAPON,
                                HudTrigger.HOLDING_RANGED_WEAPON
                        ),
                        EnumSet.of(HudTrigger.HOTBAR_INPUT),
                        null
                ),
                new HudComponent(
                        "reticle", "Reticle",
                        HudComponentRegistry.Group.CORE,
                        Reticle,
                        "HideReticle",
                        "Reticle",
                        HudComponentsConfig::isHideReticleHud,
                        HudComponentsConfig::setHideReticleHud,
                        DynamicHudConfig::getReticle,
                        true,
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
                new HudComponent(
                        "inputbindings", "Input Bindings",
                        HudComponentRegistry.Group.UI,
                        InputBindings,
                        "HideInputBindings",
                        null,
                        HudComponentsConfig::isHideInputBindingsHud,
                        HudComponentsConfig::setHideInputBindingsHud,
                        null,
                        true,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "notifications", "Notifications",
                        HudComponentRegistry.Group.UI,
                        Notifications,
                        "HideNotifications",
                        null,
                        HudComponentsConfig::isHideNotificationsHud,
                        HudComponentsConfig::setHideNotificationsHud,
                        null,
                        true,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "statusicons", "Status Icons",
                        HudComponentRegistry.Group.UI,
                        StatusIcons,
                        "HideStatusIcons",
                        null,
                        HudComponentsConfig::isHideStatusIconsHud,
                        HudComponentsConfig::setHideStatusIconsHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "speedometer", "Speedometer",
                        HudComponentRegistry.Group.UI,
                        Speedometer,
                        "HideSpeedometer",
                        null,
                        HudComponentsConfig::isHideSpeedometerHud,
                        HudComponentsConfig::setHideSpeedometerHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "ammo", "Ammo Indicator",
                        HudComponentRegistry.Group.UI,
                        AmmoIndicator,
                        "HideAmmoIndicator",
                        null,
                        HudComponentsConfig::isHideAmmoIndicatorHud,
                        HudComponentsConfig::setHideAmmoIndicatorHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "utilityslotselector", "Utility Slot Selector",
                        HudComponentRegistry.Group.UI,
                        UtilitySlotSelector,
                        "HideUtilitySlotSelector",
                        null,
                        HudComponentsConfig::isHideUtilitySlotSelectorHud,
                        HudComponentsConfig::setHideUtilitySlotSelectorHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "chat", "Chat",
                        HudComponentRegistry.Group.SOCIAL,
                        Chat,
                        "HideChat",
                        null,
                        HudComponentsConfig::isHideChatHud,
                        HudComponentsConfig::setHideChatHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "requests", "Requests",
                        HudComponentRegistry.Group.SOCIAL,
                        Requests,
                        "HideRequests",
                        null,
                        HudComponentsConfig::isHideRequestsHud,
                        HudComponentsConfig::setHideRequestsHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "killfeed", "Kill Feed",
                        HudComponentRegistry.Group.SOCIAL,
                        KillFeed,
                        "HideKillFeed",
                        null,
                        HudComponentsConfig::isHideKillFeedHud,
                        HudComponentsConfig::setHideKillFeedHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "playerlist", "Player List",
                        HudComponentRegistry.Group.SOCIAL,
                        PlayerList,
                        "HidePlayerList",
                        null,
                        HudComponentsConfig::isHidePlayerListHud,
                        HudComponentsConfig::setHidePlayerListHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "eventtitle", "Event Title",
                        HudComponentRegistry.Group.PANELS,
                        EventTitle,
                        "HideEventTitle",
                        null,
                        HudComponentsConfig::isHideEventTitleHud,
                        HudComponentsConfig::setHideEventTitleHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "objectivepanel", "Objective Panel",
                        HudComponentRegistry.Group.PANELS,
                        ObjectivePanel,
                        "HideObjectivePanel",
                        null,
                        HudComponentsConfig::isHideObjectivePanelHud,
                        HudComponentsConfig::setHideObjectivePanelHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "portalpanel", "Portal Panel",
                        HudComponentRegistry.Group.PANELS,
                        PortalPanel,
                        "HidePortalPanel",
                        null,
                        HudComponentsConfig::isHidePortalPanelHud,
                        HudComponentsConfig::setHidePortalPanelHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "sleep", "Sleep",
                        HudComponentRegistry.Group.PANELS,
                        Sleep,
                        "HideSleep",
                        null,
                        HudComponentsConfig::isHideSleepHud,
                        HudComponentsConfig::setHideSleepHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "blockvariantselector", "Block Variant Selector",
                        HudComponentRegistry.Group.BUILDER,
                        BlockVariantSelector,
                        "HideBlockVariantSelector",
                        null,
                        HudComponentsConfig::isHideBlockVariantSelectorHud,
                        HudComponentsConfig::setHideBlockVariantSelectorHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "buildertoolslegend", "Builder Tools Legend",
                        HudComponentRegistry.Group.BUILDER,
                        BuilderToolsLegend,
                        "HideBuilderToolsLegend",
                        null,
                        HudComponentsConfig::isHideBuilderToolsLegendHud,
                        HudComponentsConfig::setHideBuilderToolsLegendHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                ),
                new HudComponent(
                        "buildertoolsmaterialslotselector", "Builder Tools Material Slot Selector",
                        HudComponentRegistry.Group.BUILDER,
                        BuilderToolsMaterialSlotSelector,
                        "HideBuilderToolsMaterialSlotSelector",
                        null,
                        HudComponentsConfig::isHideBuilderToolsMaterialSlotSelectorHud,
                        HudComponentsConfig::setHideBuilderToolsMaterialSlotSelectorHud,
                        null,
                        false,
                        EnumSet.noneOf(HudTrigger.class),
                        EnumSet.noneOf(HudTrigger.class),
                        null
                )
        );
    }
}