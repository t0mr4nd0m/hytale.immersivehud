package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import java.util.function.Supplier;

public final class CommandsCollection extends AbstractCommandCollection {

    public CommandsCollection(
            HudRuntimeService hudRuntimeService,
            HudConfigUiService hudConfigUiService,
            Supplier<GlobalConfig> globalConfigSupplier,
            PlayerConfigService playerConfigService
    ) {
        super("immersivehud", "ImmersiveHud plugin commands");
        addAliases("ihud");
        addSubCommand(new StatusCmd(hudRuntimeService, globalConfigSupplier));
        addSubCommand(new ToggleCmd(playerConfigService));
        addSubCommand(new RulesCmd(playerConfigService));
        addSubCommand(new ProfileCmd(playerConfigService));
        addSubCommand(new ConfigCmd(hudRuntimeService, hudConfigUiService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}