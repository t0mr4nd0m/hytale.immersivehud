package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import com.tom.immersivehudplugin.ImmersiveHudPlugin;

public final class CommandCollection extends AbstractCommandCollection {

    public CommandCollection(ImmersiveHudPlugin plugin) {
        super("immersivehud", "ImmersiveHud plugin commands");
        addAliases("ihud");
        addSubCommand(new StatusCmd(plugin));
        addSubCommand(new ToggleCmd(plugin));
        addSubCommand(new RulesCmd(plugin));
        addSubCommand(new ProfileCmd(plugin));
        addSubCommand(new ConfigUICmd(plugin));
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}