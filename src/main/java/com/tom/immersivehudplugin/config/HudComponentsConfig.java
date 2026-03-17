package com.tom.immersivehudplugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public final class HudComponentsConfig {

    public static final BuilderCodec<HudComponentsConfig> CODEC =
            BuilderCodec.builder(HudComponentsConfig.class, HudComponentsConfig::new)

                    // Core dynamic/static-managed HUDs
                    .append(new KeyedCodec<>("HideCompassHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideCompassHud = v,
                            cfg -> cfg.hideCompassHud)
                    .add()

                    .append(new KeyedCodec<>("HideReticleHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideReticleHud = v,
                            cfg -> cfg.hideReticleHud)
                    .add()

                    .append(new KeyedCodec<>("HideHealthHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideHealthHud = v,
                            cfg -> cfg.hideHealthHud)
                    .add()

                    .append(new KeyedCodec<>("HideStaminaHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideStaminaHud = v,
                            cfg -> cfg.hideStaminaHud)
                    .add()

                    .append(new KeyedCodec<>("HideManaHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideManaHud = v,
                            cfg -> cfg.hideManaHud)
                    .add()

                    .append(new KeyedCodec<>("HideOxygenHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideOxygenHud = v,
                            cfg -> cfg.hideOxygenHud)
                    .add()

                    .append(new KeyedCodec<>("HideHotbarHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideHotbarHud = v,
                            cfg -> cfg.hideHotbarHud)
                    .add()

                    // Player
                    .append(new KeyedCodec<>("HideInputBindingsHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideInputBindingsHud = v,
                            cfg -> cfg.hideInputBindingsHud)
                    .add()

                    .append(new KeyedCodec<>("HideStatusIconsHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideStatusIconsHud = v,
                            cfg -> cfg.hideStatusIconsHud)
                    .add()

                    .append(new KeyedCodec<>("HideAmmoIndicatorHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideAmmoIndicatorHud = v,
                            cfg -> cfg.hideAmmoIndicatorHud)
                    .add()

                    .append(new KeyedCodec<>("HideNotificationsHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideNotificationsHud = v,
                            cfg -> cfg.hideNotificationsHud)
                    .add()

                    .append(new KeyedCodec<>("HideUtilitySlotSelectorHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideUtilitySlotSelectorHud = v,
                            cfg -> cfg.hideUtilitySlotSelectorHud)
                    .add()

                    .append(new KeyedCodec<>("HideSpeedometerHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideSpeedometerHud = v,
                            cfg -> cfg.hideSpeedometerHud)
                    .add()

                    // Social
                    .append(new KeyedCodec<>("HideChatHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideChatHud = v,
                            cfg -> cfg.hideChatHud)
                    .add()

                    .append(new KeyedCodec<>("HideRequestsHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideRequestsHud = v,
                            cfg -> cfg.hideRequestsHud)
                    .add()

                    .append(new KeyedCodec<>("HideKillFeedHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideKillFeedHud = v,
                            cfg -> cfg.hideKillFeedHud)
                    .add()

                    .append(new KeyedCodec<>("HidePlayerListHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hidePlayerListHud = v,
                            cfg -> cfg.hidePlayerListHud)
                    .add()

                    // Game
                    .append(new KeyedCodec<>("HideSleepHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideSleepHud = v,
                            cfg -> cfg.hideSleepHud)
                    .add()

                    .append(new KeyedCodec<>("HideEventTitleHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideEventTitleHud = v,
                            cfg -> cfg.hideEventTitleHud)
                    .add()

                    .append(new KeyedCodec<>("HideObjectivePanelHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideObjectivePanelHud = v,
                            cfg -> cfg.hideObjectivePanelHud)
                    .add()

                    .append(new KeyedCodec<>("HidePortalPanelHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hidePortalPanelHud = v,
                            cfg -> cfg.hidePortalPanelHud)
                    .add()

                    // Creative
                    .append(new KeyedCodec<>("HideBuilderToolsLegendHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideBuilderToolsLegendHud = v,
                            cfg -> cfg.hideBuilderToolsLegendHud)
                    .add()

                    .append(new KeyedCodec<>("HideBuilderToolsMaterialSlotSelectorHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideBuilderToolsMaterialSlotSelectorHud = v,
                            cfg -> cfg.hideBuilderToolsMaterialSlotSelectorHud)
                    .add()

                    .append(new KeyedCodec<>("HideBlockVariantSelectorHud", Codec.BOOLEAN),
                            (cfg, v) -> cfg.hideBlockVariantSelectorHud = v,
                            cfg -> cfg.hideBlockVariantSelectorHud)
                    .add()

                    .build();

    // Defaults: core managed HUDs
    private boolean hideCompassHud = true;
    private boolean hideReticleHud = true;
    private boolean hideHealthHud = true;
    private boolean hideStaminaHud = true;
    private boolean hideManaHud = true;
    private boolean hideHotbarHud = true;
    private boolean hideOxygenHud = false;

    // Defaults: candidates
    private boolean hideStatusIconsHud = false;
    private boolean hideNotificationsHud = true;
    private boolean hideInputBindingsHud = true;
    private boolean hideSpeedometerHud = true;
    private boolean hideAmmoIndicatorHud = false;

    // Defaults: others
    private boolean hideChatHud = false;
    private boolean hidePlayerListHud = false;
    private boolean hideRequestsHud = false;
    private boolean hideKillFeedHud = false;
    private boolean hideSleepHud = false;
    private boolean hideEventTitleHud = false;
    private boolean hideObjectivePanelHud = false;
    private boolean hidePortalPanelHud = false;
    private boolean hideBuilderToolsLegendHud = false;
    private boolean hideUtilitySlotSelectorHud = false;
    private boolean hideBlockVariantSelectorHud = false;
    private boolean hideBuilderToolsMaterialSlotSelectorHud = false;

    public boolean isHideHotbarHud() { return hideHotbarHud; }
    public void setHideHotbarHud(boolean v) { hideHotbarHud = v; }

    public boolean isHideCompassHud() { return hideCompassHud; }
    public void setHideCompassHud(boolean v) { hideCompassHud = v; }

    public boolean isHideReticleHud() { return hideReticleHud; }
    public void setHideReticleHud(boolean v) { hideReticleHud = v; }

    public boolean isHideStaminaHud() { return hideStaminaHud; }
    public void setHideStaminaHud(boolean v) { hideStaminaHud = v; }

    public boolean isHideHealthHud() { return hideHealthHud; }
    public void setHideHealthHud(boolean v) { hideHealthHud = v; }

    public boolean isHideManaHud() { return hideManaHud; }
    public void setHideManaHud(boolean v) { hideManaHud = v; }

    public boolean isHideOxygenHud() { return hideOxygenHud; }
    public void setHideOxygenHud(boolean v) { hideOxygenHud = v; }

    public boolean isHideInputBindingsHud() { return hideInputBindingsHud; }
    public void setHideInputBindingsHud(boolean v) { hideInputBindingsHud = v; }

    public boolean isHideNotificationsHud() { return hideNotificationsHud; }
    public void setHideNotificationsHud(boolean v) { hideNotificationsHud = v; }

    public boolean isHideSpeedometerHud() { return hideSpeedometerHud; }
    public void setHideSpeedometerHud(boolean v) { hideSpeedometerHud = v; }

    public boolean isHideStatusIconsHud() { return hideStatusIconsHud; }
    public void setHideStatusIconsHud(boolean v) { hideStatusIconsHud = v; }

    public boolean isHideChatHud() { return hideChatHud; }
    public void setHideChatHud(boolean v) { hideChatHud = v; }

    public boolean isHideRequestsHud() { return hideRequestsHud; }
    public void setHideRequestsHud(boolean v) { hideRequestsHud = v; }

    public boolean isHideKillFeedHud() { return hideKillFeedHud; }
    public void setHideKillFeedHud(boolean v) { hideKillFeedHud = v; }

    public boolean isHidePlayerListHud() { return hidePlayerListHud; }
    public void setHidePlayerListHud(boolean v) { hidePlayerListHud = v; }

    public boolean isHideEventTitleHud() { return hideEventTitleHud; }
    public void setHideEventTitleHud(boolean v) { hideEventTitleHud = v; }

    public boolean isHideObjectivePanelHud() { return hideObjectivePanelHud; }
    public void setHideObjectivePanelHud(boolean v) { hideObjectivePanelHud = v; }

    public boolean isHidePortalPanelHud() { return hidePortalPanelHud; }
    public void setHidePortalPanelHud(boolean v) { hidePortalPanelHud = v; }

    public boolean isHideBuilderToolsLegendHud() { return hideBuilderToolsLegendHud; }
    public void setHideBuilderToolsLegendHud(boolean v) { hideBuilderToolsLegendHud = v; }

    public boolean isHideUtilitySlotSelectorHud() { return hideUtilitySlotSelectorHud; }
    public void setHideUtilitySlotSelectorHud(boolean v) { hideUtilitySlotSelectorHud = v; }

    public boolean isHideBlockVariantSelectorHud() { return hideBlockVariantSelectorHud; }
    public void setHideBlockVariantSelectorHud(boolean v) { hideBlockVariantSelectorHud = v; }

    public boolean isHideBuilderToolsMaterialSlotSelectorHud() { return hideBuilderToolsMaterialSlotSelectorHud; }
    public void setHideBuilderToolsMaterialSlotSelectorHud(boolean v) { hideBuilderToolsMaterialSlotSelectorHud = v; }

    public boolean isHideAmmoIndicatorHud() { return hideAmmoIndicatorHud; }
    public void setHideAmmoIndicatorHud(boolean v) { hideAmmoIndicatorHud = v; }

    public boolean isHideSleepHud() { return hideSleepHud; }
    public void setHideSleepHud(boolean v) { hideSleepHud = v; }

    public boolean sanitize() {
        return false;
    }

    public HudComponentsConfig copy() {
        HudComponentsConfig c = new HudComponentsConfig();

        c.setHideHotbarHud(isHideHotbarHud());
        c.setHideStatusIconsHud(isHideStatusIconsHud());
        c.setHideReticleHud(isHideReticleHud());
        c.setHideChatHud(isHideChatHud());
        c.setHideRequestsHud(isHideRequestsHud());
        c.setHideNotificationsHud(isHideNotificationsHud());
        c.setHideKillFeedHud(isHideKillFeedHud());
        c.setHideInputBindingsHud(isHideInputBindingsHud());
        c.setHidePlayerListHud(isHidePlayerListHud());
        c.setHideEventTitleHud(isHideEventTitleHud());
        c.setHideCompassHud(isHideCompassHud());
        c.setHideObjectivePanelHud(isHideObjectivePanelHud());
        c.setHidePortalPanelHud(isHidePortalPanelHud());
        c.setHideBuilderToolsLegendHud(isHideBuilderToolsLegendHud());
        c.setHideSpeedometerHud(isHideSpeedometerHud());
        c.setHideUtilitySlotSelectorHud(isHideUtilitySlotSelectorHud());
        c.setHideBlockVariantSelectorHud(isHideBlockVariantSelectorHud());
        c.setHideBuilderToolsMaterialSlotSelectorHud(isHideBuilderToolsMaterialSlotSelectorHud());
        c.setHideStaminaHud(isHideStaminaHud());
        c.setHideAmmoIndicatorHud(isHideAmmoIndicatorHud());
        c.setHideHealthHud(isHideHealthHud());
        c.setHideManaHud(isHideManaHud());
        c.setHideOxygenHud(isHideOxygenHud());
        c.setHideSleepHud(isHideSleepHud());

        return c;
    }

}