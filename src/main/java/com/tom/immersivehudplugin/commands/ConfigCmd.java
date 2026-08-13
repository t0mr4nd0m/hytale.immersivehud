package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import javax.annotation.Nonnull;

public final class ConfigCmd extends AbstractPlayerCommand {

    private static final String COMMAND_NAME = "config";

    private final HudConfigUiService hudConfigUiService;

    public ConfigCmd(HudConfigUiService hudConfigUiService) {
        super(COMMAND_NAME, "immersivehud.cmd.config");
        this.hudConfigUiService = hudConfigUiService;
    }

    @Override
    public boolean hasPermission(@Nonnull CommandSender sender) {
        return true;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        hudConfigUiService.open(playerRef, ref, store);
    }
}