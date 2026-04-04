package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import java.util.function.Supplier;

public final class CommandCollection extends AbstractCommandCollection {

    public CommandCollection(
            HudRuntimeService hudRuntimeService,
            PlayerConfigManager playerConfigManager,
            HudConfigUiService hudConfigUiService,
            Supplier<GlobalConfig> globalConfigSupplier
    ) {
        super("immersivehud", "ImmersiveHud plugin commands");
        addAliases("ihud");
        addSubCommand(new StatusCmd(hudRuntimeService, globalConfigSupplier));
        addSubCommand(new ToggleCmd(hudRuntimeService, playerConfigManager));
        addSubCommand(new RulesCmd(hudRuntimeService, playerConfigManager));
        addSubCommand(new ProfileCmd(hudRuntimeService, playerConfigManager));
        addSubCommand(new ConfigUICmd(hudRuntimeService, hudConfigUiService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}