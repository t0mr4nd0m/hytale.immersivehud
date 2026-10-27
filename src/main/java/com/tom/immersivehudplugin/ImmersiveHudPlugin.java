package com.tom.immersivehudplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.tom.immersivehudplugin.commands.IHudCommands;
import com.tom.immersivehudplugin.config.ConfigSupport;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.GlobalConfigStore;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.config.PlayerConfigStore;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.runtime.signal.HeldItemState;
import com.tom.immersivehudplugin.runtime.PlayerSessionService;
import com.tom.immersivehudplugin.runtime.visibility.HudDeltaApplier;
import com.tom.immersivehudplugin.runtime.visibility.HudRuleEvaluator;
import com.tom.immersivehudplugin.runtime.visibility.HudVisibilityCoordinator;
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import java.util.logging.Level;

public final class ImmersiveHudPlugin extends JavaPlugin {

    private GlobalConfigStore globalConfigStore;
    private PlayerConfigStore playerConfigStore;

    private PlayerConfigService playerConfigService;
    private PlayerSessionService playerSessionService;
    private HudRuntimeService hudRuntimeService;
    private HudConfigUiService hudConfigUiService;

    public ImmersiveHudPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        setupConfigServices();
        registerWeaponAssetListeners(Item.class);
        registerWeaponAssetListeners(Interaction.class);
        registerWeaponAssetListeners(RootInteraction.class);
    }

    private <K, T extends JsonAssetWithMap<K, M>, M extends AssetMap<K, T>>
    void registerWeaponAssetListeners(Class<T> assetType) {
        getEventRegistry().register(LoadedAssetsEvent.class, assetType,
                (LoadedAssetsEvent<K, T, M> _) -> HeldItemState.invalidateWeaponCache());
        getEventRegistry().register(RemovedAssetsEvent.class, assetType,
                (RemovedAssetsEvent<K, T, M> _) -> HeldItemState.invalidateWeaponCache());
    }

    @Override
    public void start() {
        startRuntimeServices();
    }

    @Override
    public void shutdown() {
        shutdownRuntimeServices();
        HeldItemState.invalidateWeaponCache();
    }

    private void setupConfigServices() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ConfigSupport configSupport = new ConfigSupport(this, gson);
        this.globalConfigStore = new GlobalConfigStore(this, configSupport);
        this.playerConfigStore = new PlayerConfigStore(this, configSupport);

        globalConfigStore.load();

        getLogger().at(Level.INFO).log("Setup complete. Version: " + getPluginVersion());
    }

    private void startRuntimeServices() {
        if (hudRuntimeService != null) {
            getLogger().at(Level.WARNING).log("Runtime services are already started.");
            return;
        }

        this.playerConfigService = createPlayerConfigService();
        this.hudRuntimeService = createHudRuntimeService();
        this.playerSessionService = createPlayerSessionService();
        this.hudConfigUiService = createHudConfigUiService();
        hudConfigUiService.start(this);

        hudRuntimeService.start();
        playerSessionService.start();

        registerCommands();

        getLogger().at(Level.INFO).log("Runtime services started.");
    }

    private void shutdownRuntimeServices() {
        if (hudConfigUiService != null) {
            hudConfigUiService.shutdown();
        }
        if (hudRuntimeService != null) {
            hudRuntimeService.shutdown();
            hudRuntimeService = null;
            hudConfigUiService = null;
            playerSessionService = null;
            playerConfigService = null;
        }

        getLogger().at(Level.INFO).log("Runtime services stopped.");
    }

    public String getPluginVersion() {
        return this.getManifest().getVersion().toString();
    }

    public GlobalConfig getGlobalConfig() {
        return globalConfigStore.get();
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

    private PlayerSessionService createPlayerSessionService() {
        return new PlayerSessionService(
                this,
                playerConfigService,
                hudRuntimeService
        );
    }

    private HudConfigUiService createHudConfigUiService() {
        return new HudConfigUiService(
                playerConfigService,
                hudRuntimeService
        );
    }

    private void registerCommands() {
        this.getCommandRegistry().registerCommand(new IHudCommands(hudConfigUiService));
    }
}
