package com.tom.immersivehudplugin.profiles;

import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;

public final class ProfilePresets {

    private ProfilePresets() {
    }

    public static void apply(
            ImmersiveHudPlugin plugin,
            PlayerConfig playerCfg,
            Profile profile
    ) {
        if (plugin == null || playerCfg == null || profile == null) {
            return;
        }

        switch (profile) {
            case DEFAULT -> applyDefault(plugin, playerCfg);
            case IMMERSIVE -> applyImmersive(playerCfg);
            case DISABLED -> applyDisabled(playerCfg);
        }

        playerCfg.sanitize();
    }

    private static void applyImmersive(PlayerConfig cfg) {
        HudComponentsConfig hc = cfg.getHudComponents();
        DynamicHudConfig dh = cfg.getDynamicHud();

        if (hc == null || dh == null) {
            return;
        }

        hideAllHud(hc);

        // Explicit immersive exceptions
        hc.setHideStatusIconsHud(false);
        hc.setHideSleepHud(false);
        hc.setHideEventTitleHud(false);
        hc.setHideObjectivePanelHud(false);
        hc.setHidePortalPanelHud(false);
        hc.setHideChatHud(false);

        applyImmersiveDynamic(dh);
    }

    private static void applyDefault(
            ImmersiveHudPlugin plugin,
            PlayerConfig cfg
    ) {
        var globalCfg = plugin.getImmersiveHudGlobalConfig();
        if (globalCfg == null) {
            return;
        }

        if (globalCfg.getDefaultHudComponents() != null) {
            cfg.setHudComponents(globalCfg.getDefaultHudComponents().copy());
        }
        if (globalCfg.getDefaultDynamicHud() != null) {
            cfg.setDynamicHud(globalCfg.getDefaultDynamicHud().copy());
        }
    }

    private static void applyDisabled(PlayerConfig cfg) {
        HudComponentsConfig hc = cfg.getHudComponents();
        DynamicHudConfig dh = cfg.getDynamicHud();

        if (hc == null || dh == null) {
            return;
        }

        showAllHud(hc);
        clearAllDynamicRules(dh);
    }

    private static void applyImmersiveDynamic(DynamicHudConfig dh) {
        if (dh == null) {
            return;
        }

        setRules(dh.getHotbar(),
                DynamicHudTriggers.ALWAYS_HIDDEN);

        setRules(dh.getReticle(),
                DynamicHudTriggers.HOLDING_RANGED_WEAPON,
                DynamicHudTriggers.CHARGING_WEAPON,
                DynamicHudTriggers.CONSUMABLE_USE);

        setRules(dh.getCompass(),
                DynamicHudTriggers.PLAYER_WALKING,
                DynamicHudTriggers.PLAYER_RUNNING,
                DynamicHudTriggers.PLAYER_SWIMMING,
                DynamicHudTriggers.PLAYER_MOUNTING);

        setRules(dh.getHealth(),
                DynamicHudTriggers.ALWAYS_HIDDEN);

        setRules(dh.getStamina(),
                DynamicHudTriggers.ALWAYS_HIDDEN);

        setRules(dh.getMana(),
                DynamicHudTriggers.ALWAYS_HIDDEN);
    }

    private static void clearAllDynamicRules(DynamicHudConfig dh) {
        if (dh == null) {
            return;
        }

        clearRules(dh.getHotbar());
        clearRules(dh.getReticle());
        clearRules(dh.getCompass());
        clearRules(dh.getHealth());
        clearRules(dh.getStamina());
        clearRules(dh.getMana());
    }

    private static void hideAllHud(HudComponentsConfig hc) {
        for (var entry : HudComponentRegistry.all().values()) {
            entry.staticSetter().set(hc, true);
        }
    }

    private static void showAllHud(HudComponentsConfig hc) {
        for (var entry : HudComponentRegistry.all().values()) {
            entry.staticSetter().set(hc, false);
        }
    }

    private static void setRules(DynamicHudRuleConfig rules, DynamicHudTriggers... triggers) {
        if (rules == null) {
            return;
        }

        EnumSet<DynamicHudTriggers> set = EnumSet.noneOf(DynamicHudTriggers.class);
        if (triggers != null) {
            for (DynamicHudTriggers trigger : triggers) {
                if (trigger != null) {
                    set.add(trigger);
                }
            }
        }

        rules.setRules(set);
    }

    private static void clearRules(DynamicHudRuleConfig rules) {
        if (rules == null) {
            return;
        }

        rules.setRules(EnumSet.noneOf(DynamicHudTriggers.class));
    }
}