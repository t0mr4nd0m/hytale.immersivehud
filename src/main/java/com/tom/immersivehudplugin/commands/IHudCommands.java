package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import javax.annotation.Nonnull;

public final class IHudCommands extends AbstractCommandCollection {

    private static final String COMMAND_NAME = "immersivehud";
    private static final String [] COMMAND_ALIASES = {"ihud"};

    public IHudCommands(
            HudConfigUiService hudConfigUiService
    ) {
        super(COMMAND_NAME, "immersivehud.cmd.ihud");
        addAliases(COMMAND_ALIASES);
        addSubCommand(new ConfigCmd(hudConfigUiService));
    }

    @Override
    public boolean hasPermission(@Nonnull CommandSender sender) {
        return true;
    }
}