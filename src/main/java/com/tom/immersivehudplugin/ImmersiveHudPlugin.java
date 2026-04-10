package com.tom.immersivehudplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.tom.immersivehudplugin.commands.CommandCollection;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.context.HudContextBuilder;
import com.tom.immersivehudplugin.managers.ConfigSupport;
import com.tom.immersivehudplugin.managers.GlobalConfigManager;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.runtime.HudRuntimeCoordinator;
import com.tom.immersivehudplugin.ui.HudConfigUiService;
import com.tom.immersivehudplugin.visibility.HudVisibilityService;

public final class ImmersiveHudPlugin extends JavaPlugin {

    private String pluginVersion;

    private GlobalConfigManager globalConfigManager;
    private PlayerConfigManager playerConfigManager;

    private HudRuntimeCoordinator hudRuntimeCoordinator;
    private HudConfigUiService hudConfigUiService;

    public ImmersiveHudPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        setupConfigServices();
    }

    @Override
    public void start() {
        startRuntimeServices();
        registerCommands();
    }

    @Override
    public void shutdown() {
        if (hudRuntimeCoordinator != null) { hudRuntimeCoordinator.shutdown(); }
    }

    private void setupConfigServices() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ConfigSupport configSupport = new ConfigSupport(this, gson);
        this.pluginVersion = this.getManifest().getVersion().toString();
        this.globalConfigManager = new GlobalConfigManager(this, configSupport);
        this.playerConfigManager = new PlayerConfigManager(this, configSupport);
        globalConfigManager.load();
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public GlobalConfig getImmersiveHudGlobalConfig() {
        return globalConfigManager.get();
    }

    private void startRuntimeServices() {
        this.hudRuntimeCoordinator = createHudRuntimeService();
        this.hudConfigUiService = new HudConfigUiService(hudRuntimeCoordinator);
        hudRuntimeCoordinator.start();
    }

    private HudRuntimeCoordinator createHudRuntimeService() {
        HudContextBuilder hudContextBuilder = new HudContextBuilder(
                DefaultEntityStatTypes.getHealth(),
                DefaultEntityStatTypes.getStamina(),
                DefaultEntityStatTypes.getMana(),
                DefaultEntityStatTypes.getOxygen()
        );

        HudVisibilityService hudVisibilityService = new HudVisibilityService();

        return new HudRuntimeCoordinator(
                this,
                playerConfigManager,
                hudContextBuilder,
                hudVisibilityService,
                this::getImmersiveHudGlobalConfig
        );
    }

    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new CommandCollection(
                hudRuntimeCoordinator,
                playerConfigManager,
                hudConfigUiService,
                this::getImmersiveHudGlobalConfig
        ));
    }
}