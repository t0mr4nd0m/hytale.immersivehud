package com.tom.immersivehudplugin.profiles;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.EnumSet;

public final class ProfilePresets {

    private ProfilePresets() {
    }

    public static void applyTo(PlayerConfig cfg, Profile profile) {
        if (cfg == null || profile == null) {
            return;
        }

        applyRegistryDefaults(cfg);

        switch (profile) {
            case IMMERSIVE -> applyImmersive(cfg);
            case DISABLED -> applyDisabled(cfg);
            case DEFAULT -> { /* DO NOTHING */ }
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

        dyn.getHealth().setRules(EnumSet.noneOf(HudTrigger.class));
        dyn.getStamina().setRules(EnumSet.noneOf(HudTrigger.class));
        dyn.getMana().setRules(EnumSet.noneOf(HudTrigger.class));
        dyn.getOxygen().setRules(EnumSet.noneOf(HudTrigger.class));

        dyn.getCompass().setRules(EnumSet.of(
                HudTrigger.PLAYER_CROUCHING,
                HudTrigger.PLAYER_MOUNTING,
                HudTrigger.PLAYER_FLYING,
                HudTrigger.PLAYER_GLIDING
        ));
        dyn.getReticle().setRules(EnumSet.of(
                HudTrigger.CHARGING_WEAPON,
                HudTrigger.CONSUMABLE_USE,
                HudTrigger.TARGET_ENTITY,
                HudTrigger.HOLDING_RANGED_WEAPON
        ));
        dyn.getHotbar().setRules(EnumSet.noneOf(HudTrigger.class));
    }

    private static void applyDisabled(PlayerConfig cfg) {
        HudComponentsConfig hud = cfg.getHudComponents();
        DynamicHudConfig dyn = cfg.getDynamicHud();

        for (HudComponent entry : HudComponentRegistry.allList()) {
            entry.staticSetter().set(hud, false);
        }

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            DynamicHudRuleConfig ruleCfg = entry.dynamicGetter() != null ? entry.dynamicGetter().apply(dyn) : null;
            if (ruleCfg != null) {
                ruleCfg.setRules(EnumSet.noneOf(HudTrigger.class));
            }
        }
    }
}