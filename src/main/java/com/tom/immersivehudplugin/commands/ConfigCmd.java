package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import javax.annotation.Nonnull;
import java.awt.Color;

public final class ConfigCmd extends AbstractPlayerCommand {

    private static final Color ERROR_COLOR = Color.RED;

    private final HudRuntimeService hudRuntimeService;
    private final HudConfigUiService hudConfigUiService;

    public ConfigCmd(
            HudRuntimeService hudRuntimeService,
            HudConfigUiService hudConfigUiService
    ){
        super("config", "Open the ImmersiveHud configuration UI");
        this.hudRuntimeService = hudRuntimeService;
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
        if (hudRuntimeService.requirePlayerConfig(playerRef) == null) {
            context.sendMessage(Message.raw("Failed to load your ImmersiveHud configuration.").color(ERROR_COLOR));
            return;
        }

        hudConfigUiService.open(playerRef, ref, store);
    }
}