package com.tom.immersivehudplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.tom.immersivehudplugin.commands.CommandCollection;
import com.tom.immersivehudplugin.config.ConfigJsonMapper;
import com.tom.immersivehudplugin.config.ConfigSchemaValidator;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.context.HudContextBuilder;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;
import com.tom.immersivehudplugin.visibility.HudVisibilityService;

import javax.annotation.Nullable;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public final class ImmersiveHudPlugin extends JavaPlugin {

    private final PlayerConfigManager playerConfigManager =
            new PlayerConfigManager(this);

    private final AssetMap<String, Item> itemAssetMap = Item.getAssetMap();

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private GlobalConfig globalConfig = new GlobalConfig();
    private Path globalConfigPath;

    private HudRuntimeService hudRuntimeService;

    private static String pluginVersion;

    public ImmersiveHudPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        pluginVersion = this.getManifest().getVersion().toString();
        globalConfigPath = this.getDataDirectory().resolve("config.json");

        loadGlobalConfigSafely();

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

    private void loadGlobalConfigSafely() {
        try {
            Files.createDirectories(this.getDataDirectory());

            if (!Files.exists(globalConfigPath)) {
                globalConfig = new GlobalConfig();
                migrateGlobalConfigIfNeeded(globalConfig);
                saveGlobalConfigSafely();
                return;
            }

            com.google.gson.JsonElement root;
            try (Reader reader = Files.newBufferedReader(globalConfigPath)) {
                root = com.google.gson.JsonParser.parseReader(reader);
            }

            if (!ConfigSchemaValidator.isValidGlobalConfig(root)) {
                throw new IllegalStateException("config.json does not match expected schema");
            }

            globalConfig = ConfigJsonMapper.fromJsonGlobal(root.getAsJsonObject());

            boolean changed = migrateGlobalConfigIfNeeded(globalConfig);
            if (changed) {
                saveGlobalConfigSafely();
            }

        } catch (Throwable t) {
            getLogger().at(Level.WARNING).log(
                    "[ImmersiveHud] Failed to load global config, recreating defaults. "
                            + "[" + t.getClass().getSimpleName() + "]: " + t.getMessage()
            );

            backupBrokenFile(globalConfigPath);

            globalConfig = new GlobalConfig();
            migrateGlobalConfigIfNeeded(globalConfig);
            saveGlobalConfigSafely();
        }
    }

    public void saveGlobalConfigSafely() {
        try {
            Files.createDirectories(this.getDataDirectory());

            try (Writer writer = Files.newBufferedWriter(globalConfigPath)) {
                gson.toJson(ConfigJsonMapper.toJson(globalConfig), writer);
            }

        } catch (Throwable t) {
            getLogger().at(Level.WARNING).log(
                    "[ImmersiveHud] Failed to save global config. "
                            + "[" + t.getClass().getSimpleName() + "]: " + t.getMessage()
            );
        }
    }

    private void backupBrokenFile(Path path) {
        if (path == null) {
            return;
        }

        try {
            if (!Files.exists(path)) {
                return;
            }

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));

            Path backup = path.resolveSibling(path.getFileName().toString() + ".broken-" + timestamp);
            Files.move(path, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            getLogger().at(Level.WARNING).log(
                    "[ImmersiveHud] Backed up invalid config file to: " + backup
            );

        } catch (Throwable moveEx) {
            getLogger().at(Level.WARNING).log(
                    "[ImmersiveHud] Failed to back up broken config file "
                            + path
                            + " [" + moveEx.getClass().getSimpleName() + "]: "
                            + moveEx.getMessage()
            );
        }
    }

    public GlobalConfig getImmersiveHudGlobalConfig() {
        return globalConfig;
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