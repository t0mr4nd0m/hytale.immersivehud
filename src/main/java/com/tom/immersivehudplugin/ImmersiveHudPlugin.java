package com.tom.immersivehudplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.tom.immersivehudplugin.commands.IHudCommands;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.config.ConfigSupport;
import com.tom.immersivehudplugin.config.GlobalConfigStore;
import com.tom.immersivehudplugin.config.PlayerConfigStore;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.ui.HudConfigUiService;
import com.tom.immersivehudplugin.runtime.visibility.HudVisibilityCoordinator;

public final class ImmersiveHudPlugin extends JavaPlugin {

    private String pluginVersion;

    private GlobalConfigStore globalConfigStore;
    private PlayerConfigStore playerConfigStore;

    private HudRuntimeService hudRuntimeService;
    private HudConfigUiService hudConfigUiService;
    private PlayerConfigService playerConfigService;

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
        if (hudRuntimeService != null) { hudRuntimeService.shutdown(); }
    }

    private void setupConfigServices() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ConfigSupport configSupport = new ConfigSupport(this, gson);
        this.pluginVersion = this.getManifest().getVersion().toString();
        this.globalConfigStore = new GlobalConfigStore(this, configSupport);
        this.playerConfigStore = new PlayerConfigStore(this, configSupport);
        globalConfigStore.load();
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public com.tom.immersivehudplugin.config.GlobalConfig getImmersiveHudGlobalConfig() {
        return globalConfigStore.get();
    }

    private void startRuntimeServices() {
        this.hudRuntimeService = createHudRuntimeCoordinator();
        this.playerConfigService = createHudSettingsService();
        this.hudConfigUiService = new HudConfigUiService(hudRuntimeService);
        hudRuntimeService.start();
    }

    private HudRuntimeService createHudRuntimeCoordinator() {
        HudVisibilityCoordinator hudVisibilityCoordinator = new HudVisibilityCoordinator();

        return new HudRuntimeService(
                this,
                playerConfigStore,
                hudVisibilityCoordinator,
                this::getImmersiveHudGlobalConfig,
                DefaultEntityStatTypes.getHealth(),
                DefaultEntityStatTypes.getStamina(),
                DefaultEntityStatTypes.getMana(),
                DefaultEntityStatTypes.getOxygen()
        );
    }

    private PlayerConfigService createHudSettingsService() {

        return new PlayerConfigService(
                playerConfigStore,
                hudRuntimeService,
                globalConfigStore::get
        );
    }

    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new IHudCommands(
                hudRuntimeService,
                hudConfigUiService,
                this::getImmersiveHudGlobalConfig,
                playerConfigService
        ));
    }
}