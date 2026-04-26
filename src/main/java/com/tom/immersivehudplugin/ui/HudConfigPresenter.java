package com.tom.immersivehudplugin.ui;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;

import javax.annotation.Nullable;

public final class HudConfigPresenter {

    @Nullable
    public Profile resolveCurrentProfile(
            HudComponentsConfig hud,
            DynamicHudConfig dynamic
    ) {
        for (Profile profile : Profile.values()) {
            if (profile == Profile.CUSTOM) continue;

            PlayerConfig temp = new PlayerConfig();
            ProfilePresets.applyTo(temp, profile);

            if (hudComponentsEqual(hud, temp.getHudComponents())
                    && dynamicHudEqual(dynamic, temp.getDynamicHud())) {
                return profile;
            }
        }

        return Profile.CUSTOM;
    }

    // ---------------- internal helpers ----------------

    private boolean hudComponentsEqual(
            HudComponentsConfig a,
            HudComponentsConfig b
    ) {
        for (var entry : HudComponentRegistry.allList()) {
            if (entry.isHidden(a) != entry.isHidden(b)) {
                return false;
            }
        }
        return true;
    }

    private boolean dynamicHudEqual(
            DynamicHudConfig a,
            DynamicHudConfig b
    ) {
        for (var entry : HudComponentRegistry.dynamicList()) {
            DynamicHudRuleConfig ra = entry.requireDynamicRuleConfig(a);
            DynamicHudRuleConfig rb = entry.requireDynamicRuleConfig(b);

            if (!ra.getRules().equals(rb.getRules())) {
                return false;
            }

            if (entry.supportsThreshold()
                    && Float.compare(ra.getThreshold(), rb.getThreshold()) != 0) {
                return false;
            }
        }

        return true;
    }
}