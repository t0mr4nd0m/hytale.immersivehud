package com.tom.immersivehudplugin.profiles;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.EnumSet;

public final class ProfilePresets {

    private ProfilePresets() {
    }
    private static EnumSet<HudTrigger> noRules() {
        return EnumSet.noneOf(HudTrigger.class);
    }

    public static void applyTo(PlayerConfig cfg, Profile profile) {
        if (cfg == null || profile == null || profile == Profile.CUSTOM) {
            return;
        }

        applyRegistryDefaults(cfg);

        switch (profile) {
            case IMMERSIVE -> applyImmersive(cfg);
            case VANILLA -> applyVanilla(cfg);
            default -> { /* DO NOTHING */ }
        }

        cfg.sanitize();
    }

    private static void applyRegistryDefaults(PlayerConfig cfg) {
        cfg.setHudComponents(HudComponentRegistry.buildDefaultHudComponents());
        cfg.setDynamicHud(HudComponentRegistry.buildDefaultDynamicHud());
    }

    private static void applyImmersive(PlayerConfig cfg) {
        HudComponentsConfig hud = cfg.getHudComponents();
        DynamicHudConfig dyn = cfg.getDynamicHud();

        hud.setHideNotificationsHud(true);
        hud.setHideInputBindingsHud(true);

        dyn.getHealth().setRules(EnumSet.of(HudTrigger.IN_COMBAT));
        dyn.getMana().setRules(EnumSet.of(HudTrigger.IN_COMBAT));
        dyn.getStamina().setRules(noRules());
        dyn.getOxygen().setRules(noRules());

        dyn.getCompass().setRules(EnumSet.of(
                HudTrigger.PLAYER_CROUCHING,
                HudTrigger.PLAYER_MOUNTING,
                HudTrigger.PLAYER_FLYING,
                HudTrigger.PLAYER_GLIDING,
                HudTrigger.IN_COMBAT
        ));
        dyn.getReticle().setRules(EnumSet.of(
                HudTrigger.CHARGING_WEAPON,
                HudTrigger.CONSUMABLE_USE,
                HudTrigger.TARGET_ENTITY,
                HudTrigger.HOLDING_RANGED_WEAPON,
                HudTrigger.IN_COMBAT
        ));
        dyn.getHotbar().setRules(EnumSet.of(HudTrigger.IN_COMBAT));
    }

    private static void applyVanilla(PlayerConfig cfg) {
        HudComponentsConfig hud = cfg.getHudComponents();
        DynamicHudConfig dyn = cfg.getDynamicHud();

        for (HudComponent entry : HudComponentRegistry.allList()) {
            entry.staticSetter().set(hud, false);
        }

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            entry.requireDynamicRuleConfig(dyn).setRules(noRules());
        }
    }
}
