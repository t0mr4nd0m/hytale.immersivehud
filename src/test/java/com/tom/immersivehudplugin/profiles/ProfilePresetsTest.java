package com.tom.immersivehudplugin.profiles;

import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.ui.HudConfigPresenter;

public final class ProfilePresetsTest {
    private static int checks;

    public static void main(String[] args) {
        for (Profile profile : Profile.values()) {
            if (profile == Profile.CUSTOM) continue;
            verifyIndependentProfile(profile);
        }

        PlayerConfig balanced = preset(Profile.BALANCED);
        verify("Balanced includes construction", balanced.getDynamicHud().getReticle()
                .hasRule(HudTrigger.HOLDING_BUILDING_BLOCK));
        PlayerConfig immersive = preset(Profile.IMMERSIVE);
        for (String key : new String[]{"hotbar", "reticle", "health", "mana", "compass"}) {
            verify("Immersive combat: " + key, immersive.getDynamicHud().getByKey(key).hasRule(HudTrigger.IN_COMBAT));
        }
        PlayerConfig vanilla = preset(Profile.VANILLA);
        verify("Vanilla keeps all components visible", HudComponentRegistry.allList().stream()
                .noneMatch(component -> component.isHidden(vanilla.getHudComponents())));
        verify("Vanilla has no dynamic rules", HudComponentRegistry.dynamicList().stream()
                .noneMatch(component -> component.hasActiveRules(vanilla.getDynamicHud())));

        var hud = balanced.getHudComponents();
        var dynamic = balanced.getDynamicHud();
        ProfilePresets.applyTo(balanced, Profile.CUSTOM);
        verify("Custom is not a preset and leaves config unchanged", balanced.getHudComponents() == hud
                && balanced.getDynamicHud() == dynamic);
        ProfilePresets.applyTo(balanced, null);
        verify("null profile remains a no-op", balanced.getHudComponents() == hud && balanced.getDynamicHud() == dynamic);
        ProfilePresets.applyTo(null, Profile.BALANCED);
        verify("multiple presenters recognize the same preset", new HudConfigPresenter().resolveCurrentProfile(hud, dynamic)
                == new HudConfigPresenter().resolveCurrentProfile(hud, dynamic));
        System.out.println("Passed " + checks + " profile model regression checks.");
    }

    private static void verifyIndependentProfile(Profile profile) {
        PlayerConfig first = preset(profile);
        PlayerConfig second = preset(profile);
        verify(profile + " is recognized", resolve(first) == profile);
        verify(profile + " applies independent configurations", first.getHudComponents() != second.getHudComponents()
                && first.getDynamicHud() != second.getDynamicHud()
                && first.getDynamicHud().getReticle() != second.getDynamicHud().getReticle());

        boolean hidden = first.getHudComponents().isHideHealthHud();
        first.getHudComponents().setHideHealthHud(!hidden);
        verify(profile + " visibility edit becomes Custom", resolve(first) == Profile.CUSTOM);
        verify(profile + " visibility edits are isolated", second.getHudComponents().isHideHealthHud() == hidden
                && resolve(preset(profile)) == profile);
        first.getHudComponents().setHideHealthHud(hidden);
        verify(profile + " visibility revert restores recognition", resolve(first) == profile);

        var rules = first.getDynamicHud().getReticle();
        boolean hadCombat = rules.hasRule(HudTrigger.IN_COMBAT);
        if (hadCombat) rules.removeRule(HudTrigger.IN_COMBAT);
        else rules.addRule(HudTrigger.IN_COMBAT);
        verify(profile + " rule edit becomes Custom", resolve(first) == Profile.CUSTOM);
        verify(profile + " rule edits cannot alter models or other players",
                second.getDynamicHud().getReticle().hasRule(HudTrigger.IN_COMBAT) == hadCombat
                        && resolve(preset(profile)) == profile);
        if (hadCombat) rules.addRule(HudTrigger.IN_COMBAT);
        else rules.removeRule(HudTrigger.IN_COMBAT);
        verify(profile + " rule revert restores recognition", resolve(first) == profile);

        float threshold = first.getDynamicHud().getHealth().getThreshold();
        first.getDynamicHud().getHealth().setThreshold(threshold == 50f ? 25f : 50f);
        verify(profile + " threshold edit becomes Custom", resolve(first) == Profile.CUSTOM);
        verify(profile + " thresholds are independent", second.getDynamicHud().getHealth().getThreshold() == threshold);
        first.getDynamicHud().getHealth().setThreshold(threshold);
        verify(profile + " threshold revert restores recognition", resolve(first) == profile);
        first.getDynamicHud().getReticle().setThreshold(37f);
        verify(profile + " ignores thresholds on components without a slider", resolve(first) == profile);

        var previousHud = first.getHudComponents();
        var previousDynamic = first.getDynamicHud();
        ProfilePresets.applyTo(first, profile);
        previousHud.setHideHealthHud(!hidden);
        previousDynamic.getHealth().setThreshold(17f);
        verify(profile + " reapplication replaces previous mutable state", first.getHudComponents() != previousHud
                && first.getDynamicHud() != previousDynamic && resolve(first) == profile);
    }

    private static PlayerConfig preset(Profile profile) {
        PlayerConfig config = new PlayerConfig();
        ProfilePresets.applyTo(config, profile);
        return config;
    }

    private static Profile resolve(PlayerConfig config) {
        return new HudConfigPresenter().resolveCurrentProfile(config.getHudComponents(), config.getDynamicHud());
    }

    private static void verify(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
