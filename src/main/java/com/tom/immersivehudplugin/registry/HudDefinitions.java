package com.tom.immersivehudplugin.registry;

import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;
import java.util.List;

final class HudDefinitions {

    private HudDefinitions() {}

    static List<HudComponentRegistry.HudEntry> createAll() {

        return List.of(

                // Bars (dynamic-capable)

                new HudComponentRegistry.HudEntry(
                        "health", "Health",
                        HudComponentRegistry.Group.BARS, HudComponent.Health,
                        "HideHealth",
                        "Health",
                        HudComponentsConfig::isHideHealthHud,
                        HudComponentsConfig::setHideHealthHud,
                        DynamicHudConfig::getHealth,
                        true,
                        EnumSet.of(DynamicHudTriggers.HEALTH_NOT_FULL),
                        100f
                ),

                new HudComponentRegistry.HudEntry(
                        "stamina", "Stamina",
                        HudComponentRegistry.Group.BARS, HudComponent.Stamina,
                        "HideStamina",
                        "Stamina",
                        HudComponentsConfig::isHideStaminaHud,
                        HudComponentsConfig::setHideStaminaHud,
                        DynamicHudConfig::getStamina,
                        true,
                        EnumSet.of(DynamicHudTriggers.STAMINA_NOT_FULL),
                        100f
                ),

                new HudComponentRegistry.HudEntry(
                        "mana", "Mana",
                        HudComponentRegistry.Group.BARS, HudComponent.Mana,
                        "HideMana",
                        "Mana",
                        HudComponentsConfig::isHideManaHud,
                        HudComponentsConfig::setHideManaHud,
                        DynamicHudConfig::getMana,
                        true,
                        EnumSet.of(DynamicHudTriggers.MANA_NOT_FULL),
                        100f
                ),

                new HudComponentRegistry.HudEntry(
                        "oxygen", "Oxygen",
                        HudComponentRegistry.Group.BARS, HudComponent.Oxygen,
                        "HideOxygen",
                        "Oxygen",
                        HudComponentsConfig::isHideOxygenHud,
                        HudComponentsConfig::setHideOxygenHud,
                        DynamicHudConfig::getOxygen,
                        true,
                        EnumSet.of(DynamicHudTriggers.OXYGEN_NOT_FULL),
                        100f
                ),

                // Core (dynamic-capable)

                new HudComponentRegistry.HudEntry(
                        "compass", "Compass",
                        HudComponentRegistry.Group.CORE, HudComponent.Compass,
                        "HideCompass",
                        "Compass",
                        HudComponentsConfig::isHideCompassHud,
                        HudComponentsConfig::setHideCompassHud,
                        DynamicHudConfig::getCompass,
                        true,
                        EnumSet.of(DynamicHudTriggers.PLAYER_MOVING),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "hotbar", "Hotbar",
                        HudComponentRegistry.Group.CORE, HudComponent.Hotbar,
                        "HideHotbar",
                        "Hotbar",
                        HudComponentsConfig::isHideHotbarHud,
                        HudComponentsConfig::setHideHotbarHud,
                        DynamicHudConfig::getHotbar,
                        true,
                        EnumSet.of(DynamicHudTriggers.HOTBAR_INPUT),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "reticle", "Reticle",
                        HudComponentRegistry.Group.CORE, HudComponent.Reticle,
                        "HideReticle",
                        "Reticle",
                        HudComponentsConfig::isHideReticleHud,
                        HudComponentsConfig::setHideReticleHud,
                        DynamicHudConfig::getReticle,
                        true,
                        EnumSet.of(
                                DynamicHudTriggers.CHARGING_WEAPON,
                                DynamicHudTriggers.CONSUMABLE_USE,
                                DynamicHudTriggers.TARGET_ENTITY,
                                DynamicHudTriggers.INTERACTABLE_BLOCK,
                                DynamicHudTriggers.HOLDING_RANGED_WEAPON
                        ),
                        null
                ),

                // Static-only

                new HudComponentRegistry.HudEntry(
                        "inputbindings", "Input Bindings",
                        HudComponentRegistry.Group.UI, HudComponent.InputBindings,
                        "HideInputBindings",
                        null,
                        HudComponentsConfig::isHideInputBindingsHud,
                        HudComponentsConfig::setHideInputBindingsHud,
                        null,
                        true,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "notifications", "Notifications",
                        HudComponentRegistry.Group.UI, HudComponent.Notifications,
                        "HideNotifications",
                        null,
                        HudComponentsConfig::isHideNotificationsHud,
                        HudComponentsConfig::setHideNotificationsHud,
                        null,
                        true,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "statusicons", "Status Icons",
                        HudComponentRegistry.Group.UI, HudComponent.StatusIcons,
                        "HideStatusIcons",
                        null,
                        HudComponentsConfig::isHideStatusIconsHud,
                        HudComponentsConfig::setHideStatusIconsHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "speedometer", "Speedometer",
                        HudComponentRegistry.Group.UI, HudComponent.Speedometer,
                        "HideSpeedometer",
                        null,
                        HudComponentsConfig::isHideSpeedometerHud,
                        HudComponentsConfig::setHideSpeedometerHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "ammo", "Ammo Indicator",
                        HudComponentRegistry.Group.UI, HudComponent.AmmoIndicator,
                        "HideAmmoIndicator",
                        null,
                        HudComponentsConfig::isHideAmmoIndicatorHud,
                        HudComponentsConfig::setHideAmmoIndicatorHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "utilityslotselector", "Utility Slot Selector",
                        HudComponentRegistry.Group.UI, HudComponent.UtilitySlotSelector,
                        "HideUtilitySlotSelector",
                        null,
                        HudComponentsConfig::isHideUtilitySlotSelectorHud,
                        HudComponentsConfig::setHideUtilitySlotSelectorHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "chat", "Chat",
                        HudComponentRegistry.Group.SOCIAL, HudComponent.Chat,
                        "HideChat",
                        null,
                        HudComponentsConfig::isHideChatHud,
                        HudComponentsConfig::setHideChatHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "requests", "Requests",
                        HudComponentRegistry.Group.SOCIAL, HudComponent.Requests,
                        "HideRequests",
                        null,
                        HudComponentsConfig::isHideRequestsHud,
                        HudComponentsConfig::setHideRequestsHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "killfeed", "Kill Feed",
                        HudComponentRegistry.Group.SOCIAL, HudComponent.KillFeed,
                        "HideKillFeed",
                        null,
                        HudComponentsConfig::isHideKillFeedHud,
                        HudComponentsConfig::setHideKillFeedHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "playerlist", "Player List",
                        HudComponentRegistry.Group.SOCIAL, HudComponent.PlayerList,
                        "HidePlayerList",
                        null,
                        HudComponentsConfig::isHidePlayerListHud,
                        HudComponentsConfig::setHidePlayerListHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "eventtitle", "Event Title",
                        HudComponentRegistry.Group.PANELS, HudComponent.EventTitle,
                        "HideEventTitle",
                        null,
                        HudComponentsConfig::isHideEventTitleHud,
                        HudComponentsConfig::setHideEventTitleHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "objectivepanel", "Objective Panel",
                        HudComponentRegistry.Group.PANELS, HudComponent.ObjectivePanel,
                        "HideObjectivePanel",
                        null,
                        HudComponentsConfig::isHideObjectivePanelHud,
                        HudComponentsConfig::setHideObjectivePanelHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "portalpanel", "Portal Panel",
                        HudComponentRegistry.Group.PANELS, HudComponent.PortalPanel,
                        "HidePortalPanel",
                        null,
                        HudComponentsConfig::isHidePortalPanelHud,
                        HudComponentsConfig::setHidePortalPanelHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "sleep", "Sleep",
                        HudComponentRegistry.Group.PANELS, HudComponent.Sleep,
                        "HideSleep",
                        null,
                        HudComponentsConfig::isHideSleepHud,
                        HudComponentsConfig::setHideSleepHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "blockvariantselector", "Block Variant Selector",
                        HudComponentRegistry.Group.BUILDER, HudComponent.BlockVariantSelector,
                        "HideBlockVariantSelector",
                        null,
                        HudComponentsConfig::isHideBlockVariantSelectorHud,
                        HudComponentsConfig::setHideBlockVariantSelectorHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "buildertoolslegend", "Builder Tools Legend",
                        HudComponentRegistry.Group.BUILDER, HudComponent.BuilderToolsLegend,
                        "HideBuilderToolsLegend",
                        null,
                        HudComponentsConfig::isHideBuilderToolsLegendHud,
                        HudComponentsConfig::setHideBuilderToolsLegendHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                ),

                new HudComponentRegistry.HudEntry(
                        "buildertoolsmaterialslotselector", "Builder Tools Material Slot Selector",
                        HudComponentRegistry.Group.BUILDER, HudComponent.BuilderToolsMaterialSlotSelector,
                        "HideBuilderToolsMaterialSlotSelector",
                        null,
                        HudComponentsConfig::isHideBuilderToolsMaterialSlotSelectorHud,
                        HudComponentsConfig::setHideBuilderToolsMaterialSlotSelectorHud,
                        null,
                        false,
                        EnumSet.noneOf(DynamicHudTriggers.class),
                        null
                )
        );
    }
}