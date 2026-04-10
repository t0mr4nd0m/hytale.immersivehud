package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class ProfileCmd extends AbstractPlayerCommand {

    private static final Color INFO_COLOR = Color.LIGHT_GRAY;
    private static final Color ERROR_COLOR = Color.RED;
    private static final Color SUCCESS_COLOR = Color.GREEN;

    private final HudRuntimeService hudRuntimeService;
    private final PlayerConfigManager playerConfigManager;
    private final RequiredArg<String> profileArg;

    public ProfileCmd(
            HudRuntimeService hudRuntimeService,
            PlayerConfigManager playerConfigManager
    ) {
        super("profile", "Apply a predefined ImmersiveHud profile.");
        this.hudRuntimeService = hudRuntimeService;
        this.playerConfigManager = playerConfigManager;

        this.profileArg = withRequiredArg("profile", "Profile name", ArgTypes.STRING)
                .addValidator(new HudProfileValidator());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Profile profile = Profile.fromString(profileArg.get(context));
        if (profile == null) {
            context.sendMessage(Message.raw("Unknown profile.").color(ERROR_COLOR));
            return;
        }

        PlayerConfig playerCfg = requirePlayerConfig(hudRuntimeService, playerRef, context);
        if (playerCfg == null) {
            return;
        }

        applyProfile(hudRuntimeService, playerConfigManager, playerRef, playerCfg, profile);

        context.sendMessage(Message.join(
                Message.raw("Applied ImmersiveHud profile: ").color(INFO_COLOR),
                Message.raw(profile.name().toLowerCase(Locale.ROOT)).color(SUCCESS_COLOR)
        ));
    }

    @Nullable
    private static PlayerConfig requirePlayerConfig(
            @Nonnull HudRuntimeService hudRuntimeService,
            @Nonnull PlayerRef playerRef,
            @Nonnull CommandContext context
    ) {
        PlayerConfig playerConfig = hudRuntimeService.requirePlayerConfig(playerRef);
        if (playerConfig == null) {
            context.sendMessage(Message.raw("Failed to load your ImmersiveHud profile.").color(ERROR_COLOR));
            return null;
        }
        return playerConfig;
    }

    private static void applyProfile(
            @Nonnull HudRuntimeService hudRuntimeService,
            @Nonnull PlayerConfigManager playerConfigManager,
            @Nonnull PlayerRef playerRef,
            @Nonnull PlayerConfig playerCfg,
            @Nonnull Profile profile
    ) {
        ProfilePresets.applyTo(playerCfg, profile);
        hudRuntimeService.applyAndSavePlayerConfig(playerRef);
        hudRuntimeService.restartTickTaskIfNeeded();
    }

    private static String normalize(@Nullable String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static final class HudProfileValidator implements Validator<String> {
        private final Set<String> allowed = Arrays.stream(Profile.values())
                .map(v -> v.name().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        @Override
        public void accept(@Nullable String input, @Nonnull ValidationResults results) {
            String normalized = normalize(input);
            if (!allowed.contains(normalized)) {
                results.fail("Unknown profile: " + input + ". Available profiles: " + String.join(", ", allowed));
            }
        }

        @Override
        public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        }
    }
}