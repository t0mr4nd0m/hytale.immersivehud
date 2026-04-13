package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.tom.immersivehudplugin.profiles.Profile;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public final class HudConfigProfilesRenderer {

    private static final String PROFILES_UI = "Pages/ImmersiveHud/Views/HudConfigProfilesView.ui";
    private static final String PROFILE_ROW_UI = "Pages/ImmersiveHud/Views/HudConfigProfileRow.ui";

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
        commands.clear("#ProfilesList");

        commands.set(
                "#ViewHelpText.TextSpans",
                Message.raw(session.getCurrentView().helpText())
        );

        List<Profile> profiles = Arrays.stream(Profile.values())
                .filter(profile -> profile != Profile.CUSTOM)
                .toList();

        Profile currentProfile = presenter.resolveCurrentProfile(
                session.getDraftHudComponents(),
                session.getDraftDynamicHud()
        );

        int rowIndex = 0;

        for (Profile profile : profiles) {
            boolean isSelected = currentProfile == profile;

            commands.append("#ProfilesList", PROFILE_ROW_UI);

            String rowRootSelector = "#ProfilesList[" + rowIndex + "]";
            String labelSelector = rowRootSelector + " #ProfileLabel";
            String labelSelectedSelector = rowRootSelector + " #ProfileSelectedLabel";
            String descriptionSelector = rowRootSelector + " #ProfileDescription";
            String selectProfileButtonSelector = rowRootSelector + " #SelectProfileButton";
            String selectedProfileSelector = rowRootSelector + " #SelectedProfile";

            commands.set(labelSelector + ".TextSpans", Message.raw(profile.label().toUpperCase() + " PROFILE"));
            commands.set(labelSelectedSelector + ".TextSpans", Message.raw(profile.label().toUpperCase() + " PROFILE <APPLIED>"));
            commands.set(labelSelector + ".Visible", !isSelected);
            commands.set(labelSelectedSelector + ".Visible", isSelected);
            commands.set(descriptionSelector + ".TextSpans", Message.raw(profile.description()));
            commands.set(selectProfileButtonSelector + ".Visible", !isSelected);
            commands.set(selectedProfileSelector + ".Visible", isSelected);

            events.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selectProfileButtonSelector,
                    HudConfigPage.PageEventData.action("SELECT_PROFILE").append("Value", profile.name()),
                    false
            );

            rowIndex++;
        }

        if (currentProfile == Profile.CUSTOM) {
            commands.append("#ProfilesList", PROFILE_ROW_UI);

            String rowRootSelector = "#ProfilesList[" + rowIndex + "]";
            String labelSelector = rowRootSelector + " #ProfileLabel";
            String labelSelectedSelector = rowRootSelector + " #ProfileSelectedLabel";
            String descriptionSelector = rowRootSelector + " #ProfileDescription";
            String selectedProfileSelector = rowRootSelector + " #SelectedProfile";

            commands.set(labelSelector + ".TextSpans", Message.raw(Profile.CUSTOM.label().toUpperCase()));
            commands.set(labelSelectedSelector + ".TextSpans", Message.raw(Profile.CUSTOM.label().toUpperCase() + " PROFILE <APPLIED>"));
            commands.set(descriptionSelector + ".TextSpans", Message.raw(Profile.CUSTOM.description()));

            commands.set(labelSelector + ".Visible", false);
            commands.set(labelSelectedSelector + ".Visible", true);
            commands.set(selectedProfileSelector + ".Visible", true);
        }
    }
}