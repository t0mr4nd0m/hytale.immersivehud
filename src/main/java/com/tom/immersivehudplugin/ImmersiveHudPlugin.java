package com.tom.immersivehudplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.tom.immersivehudplugin.commands.IHudCommands;
import com.tom.immersivehudplugin.config.ConfigSupport;
import com.tom.immersivehudplugin.config.GlobalConfigStore;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.config.PlayerConfigStore;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.runtime.PlayerLifecycleService;
import com.tom.immersivehudplugin.runtime.visibility.HudDeltaApplier;
import com.tom.immersivehudplugin.runtime.visibility.HudRuleEvaluator;
import com.tom.immersivehudplugin.runtime.visibility.HudVisibilityCoordinator;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

public final class ImmersiveHudPlugin extends JavaPlugin {

    private GlobalConfigStore globalConfigStore;
    private PlayerConfigStore playerConfigStore;
    private PlayerConfigService playerConfigService;
    private HudRuntimeService hudRuntimeService;
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
        if (hudRuntimeService != null) {
            hudRuntimeService.shutdown();
        }
    }

    private void setupConfigServices() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ConfigSupport configSupport = new ConfigSupport(this, gson);
        this.globalConfigStore = new GlobalConfigStore(this, configSupport);
        this.playerConfigStore = new PlayerConfigStore(this, configSupport);
        globalConfigStore.load();
    }

    public String getPluginVersion() {
        return this.getManifest().getVersion().toString();
    }

    public com.tom.immersivehudplugin.config.GlobalConfig getGlobalConfig() {
        return globalConfigStore.get();
    }

    private void startRuntimeServices() {
        this.playerConfigService = createPlayerConfigService();
        this.hudRuntimeService = createHudRuntimeService();
        PlayerLifecycleService playerLifecycleService = createPlayerLifecycleService();
        this.hudConfigUiService = new HudConfigUiService(playerConfigService, hudRuntimeService);

        hudRuntimeService.start();
        playerLifecycleService.start();
    }

    private PlayerConfigService createPlayerConfigService() {
        return new PlayerConfigService(
                playerConfigStore,
                globalConfigStore::get
        );
    }

    private HudRuntimeService createHudRuntimeService() {
        HudRuleEvaluator hudRuleEvaluator = new HudRuleEvaluator();
        HudDeltaApplier hudDeltaApplier = new HudDeltaApplier();
        HudVisibilityCoordinator hudVisibilityCoordinator =
                new HudVisibilityCoordinator(hudRuleEvaluator, hudDeltaApplier);

        return new HudRuntimeService(
                this,
                playerConfigService,
                hudVisibilityCoordinator,
                this::getGlobalConfig,
                DefaultEntityStatTypes.getHealth(),
                DefaultEntityStatTypes.getStamina(),
                DefaultEntityStatTypes.getMana(),
                DefaultEntityStatTypes.getOxygen()
        );
    }

    private PlayerLifecycleService createPlayerLifecycleService() {
        return new PlayerLifecycleService(
                this,
                playerConfigService,
                hudRuntimeService
        );
    }

    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new IHudCommands(
                hudConfigUiService,
                this::getGlobalConfig,
                playerConfigService,
                hudRuntimeService
        ));
    }
}