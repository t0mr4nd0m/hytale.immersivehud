package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.HudSettingsService;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.runtime.HudRuntimeCoordinator;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import java.util.function.Supplier;

public final class CommandCollection extends AbstractCommandCollection {

    public CommandCollection(
            HudRuntimeCoordinator hudRuntimeCoordinator,
            HudConfigUiService hudConfigUiService,
            Supplier<GlobalConfig> globalConfigSupplier,
            HudSettingsService hudSettingsService
    ) {
        super("immersivehud", "ImmersiveHud plugin commands");
        addAliases("ihud");
        addSubCommand(new StatusCmd(hudRuntimeCoordinator, globalConfigSupplier));
        addSubCommand(new ToggleCmd(hudSettingsService));
        addSubCommand(new RulesCmd(hudSettingsService));
        addSubCommand(new ProfileCmd(hudSettingsService));
        addSubCommand(new ConfigUICmd(hudRuntimeCoordinator, hudConfigUiService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}