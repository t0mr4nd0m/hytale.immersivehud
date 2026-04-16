package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import java.util.function.Supplier;

public final class IHudCommands extends AbstractCommandCollection {

    public IHudCommands(
            HudConfigUiService hudConfigUiService,
            Supplier<GlobalConfig> globalConfigSupplier,
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService
    ) {
        super("immersivehud", "ImmersiveHud plugin commands");
        addAliases("ihud");
        addSubCommand(new StatusCmd(playerConfigService, globalConfigSupplier));
        addSubCommand(new ToggleCmd(playerConfigService, hudRuntimeService));
        addSubCommand(new RulesCmd(playerConfigService, hudRuntimeService));
        addSubCommand(new ProfileCmd(playerConfigService, hudRuntimeService));
        addSubCommand(new ConfigCmd(playerConfigService, hudConfigUiService));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}