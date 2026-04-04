package com.tom.immersivehudplugin.managers;

import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.config.ConfigJsonMapper;
import com.tom.immersivehudplugin.config.ConfigSchemaValidator;
import com.tom.immersivehudplugin.config.GlobalConfig;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;

public final class GlobalConfigManager {

    private final ImmersiveHudPlugin plugin;
    private final ConfigSupport configSupport;
    private final Path configPath;

    private GlobalConfig globalConfig = new GlobalConfig();

    public GlobalConfigManager(
            ImmersiveHudPlugin plugin,
            ConfigSupport configSupport
    ) {
        this.plugin = plugin;
        this.configSupport = configSupport;
        this.configPath = plugin.getDataDirectory().resolve("config.json");
    }

    public GlobalConfig get() {
        return globalConfig;
    }

    public void loadSafely() {

        ConfigSupport.LoadResult<GlobalConfig> result = configSupport.loadOrCreate(
                configPath,
                this::createDefaultConfig,
                ConfigSchemaValidator::isValidGlobalConfig,
                ConfigJsonMapper::fromJsonGlobal,
                cfg -> {
                    boolean changed = cfg.sanitize();
                    changed |= migrateIfNeeded(cfg);
                    return changed;
                },
                "config.json does not match expected schema"
        );

        globalConfig = result.config();

        if (result.changed()) { saveSafely(); }
    }

    public void saveSafely() {
        try {
            configSupport.writeJson(configPath, ConfigJsonMapper.toJson(globalConfig));
        } catch (Throwable t) {
            plugin.getLogger().at(Level.WARNING).log(
                    " Failed to save global config. [{}]: {}",
                    t.getClass().getSimpleName(),
                    t.getMessage()
            );
        }
    }

    private GlobalConfig createDefaultConfig() {
        GlobalConfig cfg = new GlobalConfig();
        migrateIfNeeded(cfg);
        cfg.sanitize();
        return cfg;
    }

    private boolean migrateIfNeeded(GlobalConfig cfg) {
        boolean changed = false;

        String pluginVersion = plugin.getPluginVersion();
        if (!Objects.equals(cfg.getConfigVersion(), pluginVersion)) {
            cfg.setConfigVersion(pluginVersion);
            changed = true;
        }

        return changed;
    }
}