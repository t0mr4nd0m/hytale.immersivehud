package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.profiles.Profile;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public final class HudConfigProfilesRenderer {

    private static final String PROFILES_UI = "Views/ProfilesView.ui";
    private static final String PROFILE_ROW_UI = "Views/ProfileRow.ui";

    private static final String CONTENT_SELECTOR = "#ProfilesContent";

    private final HudConfigPresenter presenter;

    public HudConfigProfilesRenderer(@Nonnull HudConfigPresenter presenter) {
        this.presenter = presenter;
    }

    public void renderProfilesView(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull HudConfigUiSession session
    ) {
        commands.append("#ContentHost", PROFILES_UI);
        commands.clear(CONTENT_SELECTOR);

        Profile currentProfile = presenter.resolveCurrentProfile(
                session.getDraftHudComponents(),
                session.getDraftDynamicHud()
        );

        List<Profile> profiles = profilesToRender(currentProfile);

        for (int i = 0; i < profiles.size(); i++) {
            renderProfileRow(commands, events, profiles.get(i), currentProfile, i);
        }
    }

    private List<Profile> profilesToRender(@Nonnull Profile currentProfile) {
        List<Profile> baseProfiles = Arrays.stream(Profile.values())
                .filter(profile -> profile != Profile.CUSTOM)
                .toList();

        if (currentProfile != Profile.CUSTOM) {
            return baseProfiles;
        }

        return Arrays.stream(Profile.values()).toList();
    }

    private void renderProfileRow(
            @Nonnull UICommandBuilder commands,
            @Nonnull UIEventBuilder events,
            @Nonnull Profile profile,
            @Nonnull Profile currentProfile,
            int rowIndex
    ) {
        boolean selected = profile == currentProfile;

        commands.append(CONTENT_SELECTOR, PROFILE_ROW_UI);

        ProfileRowSelectors selectors = ProfileRowSelectors.from(rowIndex);

        commands.set(selectors.icon() + ".Background", Value.ref(PROFILE_ROW_UI, profileIconRef(profile)));
        commands.set(selectors.label() + ".TextSpans", Message.raw(profile.label()));
        commands.set(selectors.label() + ".Style", Value.ref(PROFILE_ROW_UI, labelStyleRef(selected)));
        commands.set(selectors.description() + ".TextSpans", Message.raw(profile.description()));
        commands.set(selectors.button() + ".Style", Value.ref(PROFILE_ROW_UI, buttonStyleRef(selected)));
        commands.set(selectors.selectedIcon() + ".Visible", selected);

        if (!selected) {
            bindSelectProfileEvent(events, selectors.button(), profile);
        }
    }

    private void bindSelectProfileEvent(
            @Nonnull UIEventBuilder events,
            @Nonnull String selector,
            @Nonnull Profile profile
    ) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                IHudConfigPage.PageEventData.action("SELECT_PROFILE")
                        .append("Value", profile.name()),
                false
        );
    }

    private String profileIconRef(@Nonnull Profile profile) {
        return profile.label() + "ProfileIcon";
    }

    private String labelStyleRef(boolean selected) {
        return selected ? "ProfileSelectedLabelStyle" : "ProfileLabelStyle";
    }

    private String buttonStyleRef(boolean selected) {
        return selected ? "SelectedProfileButtonStyle" : "ProfileButtonStyle";
    }

    private record ProfileRowSelectors(
            String row,
            String button,
            String icon,
            String label,
            String description,
            String selectedIcon
    ) {

        private static ProfileRowSelectors from(int rowIndex) {
            String row = CONTENT_SELECTOR + "[" + rowIndex + "]";

            return new ProfileRowSelectors(
                    row,
                    row + " #ProfileButton",
                    row + " #ProfileIcon",
                    row + " #ProfileLabel",
                    row + " #ProfileDescription",
                    row + " #ProfileSelectedIcon"
            );
        }
    }
}