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

    public static void applyTo(PlayerConfig cfg, Profile profile) {
        if (cfg == null || profile == null) {
            return;
        }

        applyRegistryDefaults(cfg);

        switch (profile) {
            case IMMERSIVE -> applyImmersive(cfg);
            case VANILLA -> applyVanilla(cfg);
            case BALANCED -> { /* DO NOTHING */ }
        }

        cfg.sanitize();
    }

    private static void applyRegistryDefaults(PlayerConfig cfg) {
        cfg.setHudComponents(HudComponentRegistry.buildDefaultHudComponents());
        cfg.setDynamicHud(HudComponentRegistry.buildDefaultDynamicHud());
    }

    private static final EnumSet<HudTrigger> NO_RULES = EnumSet.noneOf(HudTrigger.class);

    private static void applyImmersive(PlayerConfig cfg) {
        HudComponentsConfig hud = cfg.getHudComponents();
        DynamicHudConfig dyn = cfg.getDynamicHud();

        hud.setHideNotificationsHud(true);
        hud.setHideInputBindingsHud(true);

        dyn.getHealth().setRules(NO_RULES);
        dyn.getStamina().setRules(NO_RULES);
        dyn.getMana().setRules(NO_RULES);
        dyn.getOxygen().setRules(NO_RULES);

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
        dyn.getHotbar().setRules(NO_RULES);
    }

    private static void applyVanilla(PlayerConfig cfg) {
        HudComponentsConfig hud = cfg.getHudComponents();
        DynamicHudConfig dyn = cfg.getDynamicHud();

        for (HudComponent entry : HudComponentRegistry.allList()) {
            entry.staticSetter().set(hud, false);
        }

        for (HudComponent entry : HudComponentRegistry.dynamicList()) {
            entry.requireDynamicRuleConfig(dyn).setRules(NO_RULES);
        }
    }
}