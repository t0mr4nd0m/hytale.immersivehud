package com.tom.immersivehudplugin.profiles;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudComponentRegistry.HudEntry;
import com.tom.immersivehudplugin.registry.HudDefaults;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

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
        cfg.setHudComponents(HudDefaults.buildDefaultHudComponents());
        cfg.setDynamicHud(HudDefaults.buildDefaultDynamicHud());
    }

    private static void applyImmersive(PlayerConfig cfg) {
        HudComponentsConfig hud = cfg.getHudComponents();
        DynamicHudConfig dyn = cfg.getDynamicHud();

        hud.setHideRequestsHud(true);
        hud.setHideKillFeedHud(true);
        hud.setHidePlayerListHud(true);
        hud.setHideNotificationsHud(true);
        hud.setHideInputBindingsHud(true);

        dyn.getHealth().setRules(EnumSet.noneOf(DynamicHudTriggers.class));
        dyn.getStamina().setRules(EnumSet.noneOf(DynamicHudTriggers.class));
        dyn.getMana().setRules(EnumSet.noneOf(DynamicHudTriggers.class));
        dyn.getOxygen().setRules(EnumSet.noneOf(DynamicHudTriggers.class));

        dyn.getCompass().setRules(EnumSet.of(
                DynamicHudTriggers.PLAYER_RUNNING,
                DynamicHudTriggers.PLAYER_MOUNTING,
                DynamicHudTriggers.PLAYER_SWIMMING,
                DynamicHudTriggers.PLAYER_FLYING,
                DynamicHudTriggers.PLAYER_GLIDING
        ));
        dyn.getReticle().setRules(EnumSet.of(
                DynamicHudTriggers.CHARGING_WEAPON,
                DynamicHudTriggers.CONSUMABLE_USE,
                DynamicHudTriggers.TARGET_ENTITY,
                DynamicHudTriggers.HOLDING_RANGED_WEAPON
        ));
        dyn.getHotbar().setRules(EnumSet.noneOf(DynamicHudTriggers.class));
    }

    private static void applyDisabled(PlayerConfig cfg) {
        HudComponentsConfig hud = cfg.getHudComponents();
        DynamicHudConfig dyn = cfg.getDynamicHud();

        for (HudEntry entry : HudComponentRegistry.allList()) {
            entry.staticSetter().set(hud, false);
        }

        for (HudEntry entry : HudComponentRegistry.dynamicList()) {
            DynamicHudRuleConfig ruleCfg = entry.dynamicGetter() != null ? entry.dynamicGetter().apply(dyn) : null;
            if (ruleCfg != null) {
                ruleCfg.setRules(EnumSet.noneOf(DynamicHudTriggers.class));
            }
        }
    }
}