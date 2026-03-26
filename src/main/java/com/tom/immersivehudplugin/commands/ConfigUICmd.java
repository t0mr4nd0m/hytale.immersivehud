package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.ImmersiveHudPlugin;

import javax.annotation.Nonnull;
import java.awt.Color;

public final class ConfigUICmd extends AbstractPlayerCommand {

    private static final Color ERROR_COLOR = Color.RED;

    private final ImmersiveHudPlugin plugin;

    public ConfigUICmd(ImmersiveHudPlugin plugin) {
        super("config", "Open the ImmersiveHud configuration UI");
        this.plugin = plugin;
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
        if (plugin.requirePlayerConfig(playerRef) == null) {
            context.sendMessage(Message.raw("Failed to load your ImmersiveHud profile.").color(ERROR_COLOR));
            return;
        }

        plugin.getHudConfigUiService().open(playerRef, ref, store);
    }
}