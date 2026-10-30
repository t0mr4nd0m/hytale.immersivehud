package com.tom.immersivehudplugin.profiles;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        Models.BUILT_IN.get(profile).applyTo(cfg);
    }

    public static Profile resolveCurrentProfile(HudComponentsConfig hud, DynamicHudConfig dynamic) {
        for (var entry : Models.BUILT_IN.entrySet()) {
            if (entry.getValue().matches(hud, dynamic)) return entry.getKey();
        }
        return Profile.CUSTOM;
    }

    // Lazy initialization avoids coupling model construction to registry initialization.
    private static final class Models {
        private static final Map<Profile, ProfileModel> BUILT_IN = buildModels();

        private static Map<Profile, ProfileModel> buildModels() {
            EnumMap<Profile, ProfileModel> models = new EnumMap<>(Profile.class);
            for (Profile profile : Profile.values()) {
                if (profile == Profile.CUSTOM) continue;
                PlayerConfig cfg = new PlayerConfig();
                switch (profile) {
                    case IMMERSIVE -> applyImmersive(cfg);
                    case VANILLA -> applyVanilla(cfg);
                    default -> { /* Balanced uses registry defaults. */ }
                }
                cfg.sanitize();
                List<ComponentState> components = HudComponentRegistry.allList().stream()
                        .map(component -> ComponentState.capture(component, cfg))
                        .toList();
                models.put(profile, new ProfileModel(components));
            }
            // EnumMap preserves the previous profile matching order.
            return Collections.unmodifiableMap(models);
        }
    }

    private record ProfileModel(List<ComponentState> components) {
        private ProfileModel {
            components = List.copyOf(components);
        }

        private boolean matches(HudComponentsConfig hud, DynamicHudConfig dynamic) {
            for (ComponentState state : components) {
                if (!state.matches(hud, dynamic)) return false;
            }
            return true;
        }

        private void applyTo(PlayerConfig cfg) {
            HudComponentsConfig hud = new HudComponentsConfig();
            DynamicHudConfig dynamic = new DynamicHudConfig();
            for (ComponentState state : components) {
                state.component().setHidden(hud, state.hidden());
                if (state.component().supportsDynamicRules()) {
                    var rules = state.component().requireDynamicRuleConfig(dynamic);
                    rules.setRules(state.rules());
                    if (state.threshold() != null) rules.setThreshold(state.threshold());
                }
            }
            cfg.setHudComponents(hud);
            cfg.setDynamicHud(dynamic);
        }
    }

    private record ComponentState(HudComponent component, boolean hidden,
                                  Set<HudTrigger> rules, @Nullable Float threshold) {
        private ComponentState {
            rules = Set.copyOf(rules);
        }

        private static ComponentState capture(HudComponent component, PlayerConfig cfg) {
            var rules = component.getDynamicRuleConfig(cfg.getDynamicHud());
            return new ComponentState(component, component.isHidden(cfg.getHudComponents()),
                    rules != null ? rules.getRulesView() : Set.of(),
                    rules != null && component.supportsThreshold() ? rules.getThreshold() : null);
        }

        private boolean matches(HudComponentsConfig hud, DynamicHudConfig dynamic) {
            if (component.isHidden(hud) != hidden) return false;
            if (!component.supportsDynamicRules()) return true;
            var candidate = component.requireDynamicRuleConfig(dynamic);
            return rules.equals(candidate.getRulesView())
                    && (threshold == null || Float.compare(threshold, candidate.getThreshold()) == 0);
        }
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
