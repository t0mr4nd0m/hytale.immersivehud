package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.runtime.HudRuntimeCoordinator;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import java.util.function.Supplier;

public final class CommandCollection extends AbstractCommandCollection {

    public CommandCollection(
            HudRuntimeCoordinator hudRuntimeCoordinator,
            PlayerConfigManager playerConfigManager,
            HudConfigUiService hudConfigUiService,
            Supplier<GlobalConfig> globalConfigSupplier
    ) {
        super("immersivehud", "ImmersiveHud plugin commands");
        addAliases("ihud");
        addSubCommand(new StatusCmd(hudRuntimeCoordinator, globalConfigSupplier));
        addSubCommand(new ToggleCmd(hudRuntimeCoordinator, playerConfigManager));
        addSubCommand(new RulesCmd(hudRuntimeCoordinator, playerConfigManager));
        addSubCommand(new ProfileCmd(hudRuntimeCoordinator, playerConfigManager));
        addSubCommand(new ConfigUICmd(hudRuntimeCoordinator, hudConfigUiService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}