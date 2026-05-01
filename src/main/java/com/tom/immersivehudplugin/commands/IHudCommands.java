package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

public final class IHudCommands extends AbstractCommandCollection {

    public IHudCommands(
            HudConfigUiService hudConfigUiService,
            PlayerConfigService playerConfigService
    ) {
        super("immersivehud", "ImmersiveHud plugin commands");
        addAliases("ihud");
        addSubCommand(new ConfigCmd(playerConfigService, hudConfigUiService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}