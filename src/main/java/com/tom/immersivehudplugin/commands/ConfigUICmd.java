package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.runtime.HudRuntimeCoordinator;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import javax.annotation.Nonnull;
import java.awt.Color;

public final class ConfigUICmd extends AbstractPlayerCommand {

    private static final Color ERROR_COLOR = Color.RED;

    private final HudRuntimeCoordinator hudRuntimeCoordinator;
    private final HudConfigUiService hudConfigUiService;

    public ConfigUICmd(
            HudRuntimeCoordinator hudRuntimeCoordinator,
            HudConfigUiService hudConfigUiService
    ){
        super("config", "Open the ImmersiveHud configuration UI");
        this.hudRuntimeCoordinator = hudRuntimeCoordinator;
        this.hudConfigUiService = hudConfigUiService;
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
        if (hudRuntimeCoordinator.requirePlayerConfig(playerRef) == null) {
            context.sendMessage(Message.raw("Failed to load your ImmersiveHud profile.").color(ERROR_COLOR));
            return;
        }

        hudConfigUiService.open(playerRef, ref, store);
    }
}