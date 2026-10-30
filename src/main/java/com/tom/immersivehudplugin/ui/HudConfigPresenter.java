package com.tom.immersivehudplugin.ui;

import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;

public final class HudConfigPresenter {

    public Profile resolveCurrentProfile(
            HudComponentsConfig hud,
            DynamicHudConfig dynamic
    ) {
        return ProfilePresets.resolveCurrentProfile(hud, dynamic);
    }
}
