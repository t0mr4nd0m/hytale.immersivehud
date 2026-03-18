package com.tom.immersivehudplugin;

import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.util.Config;
import com.tom.immersivehudplugin.commands.CommandCollection;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.context.HudContextBuilder;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.visibility.HudVisibilityService;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

public final class ImmersiveHudPlugin extends JavaPlugin {

    private final Config<GlobalConfig> globalConfig =
            this.withConfig("config", GlobalConfig.CODEC);

    private final PlayerConfigManager playerConfigManager =
            new PlayerConfigManager(this);

    private final AssetMap<String, Item> itemAssetMap = Item.getAssetMap();

    private HudRuntimeService hudRuntimeService;

    public ImmersiveHudPlugin(JavaPluginInit init) {
        super(init);
    }

    private static String pluginVersion;

    @Override
    protected void setup() {

        pluginVersion = this.getManifest().getVersion().toString();

        globalConfig.load().join();

        GlobalConfig cfg = getImmersiveHudGlobalConfig();
        boolean changed = migrateGlobalConfigIfNeeded(cfg);
        Path cfgPath = this.getDataDirectory().resolve("config.json");
        boolean fileMissing = !Files.exists(cfgPath);

        if (fileMissing || changed) {
            globalConfig.save().join();
        }

        this.getCommandRegistry().registerCommand(new CommandCollection(this));
    }

    @Override
    public void start() {
        int healthState = DefaultEntityStatTypes.getHealth();
        int staminaState = DefaultEntityStatTypes.getStamina();
        int manaState = DefaultEntityStatTypes.getMana();

        HudContextBuilder hudContextBuilder = new HudContextBuilder(
                itemAssetMap,
                healthState,
                staminaState,
                manaState
        );

        HudVisibilityService hudVisibilityService = new HudVisibilityService();

        hudRuntimeService = new HudRuntimeService(
                this,
                playerConfigManager,
                hudContextBuilder,
                hudVisibilityService,
                itemAssetMap,
                this::getImmersiveHudGlobalConfig
        );

        hudRuntimeService.start();
    }

    @Override
    public void shutdown() {
        if (hudRuntimeService != null) {
            hudRuntimeService.shutdown();
        }
    }

    public GlobalConfig getImmersiveHudGlobalConfig() {
        return globalConfig.get();
    }

    public static String getPluginVersion() {
        return pluginVersion;
    }

    @Nullable
    public PlayerConfig requirePlayerConfig(PlayerRef playerRef) {
        return hudRuntimeService != null ? hudRuntimeService.requirePlayerConfig(playerRef) : null;
    }

    public void markPlayerConfigDirty(UUID uuid) {
        if (hudRuntimeService != null) {
            hudRuntimeService.markPlayerConfigDirty(uuid);
        } else {
            playerConfigManager.markDirty(uuid);
        }
    }

    public void markPlayerStaticHudDirty(PlayerRef playerRef) {
        if (hudRuntimeService != null) {
            hudRuntimeService.markPlayerStaticHudDirty(playerRef);
        }
    }

    public void restartTickTaskIfNeeded() {
        if (hudRuntimeService != null) {
            hudRuntimeService.restartTickTaskIfNeeded();
        }
    }

    public void savePlayerConfigAsync(UUID uuid) {
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> playerConfigManager.save(uuid));
    }

    private boolean migrateGlobalConfigIfNeeded(GlobalConfig cfg) {
        boolean changed = false;

        if (!Objects.equals(cfg.getConfigVersion(), pluginVersion)) {
            cfg.setConfigVersion(pluginVersion);
            changed = true;
        }

        return changed || cfg.sanitize();
    }
}