package com.tom.immersivehudplugin.config;

import com.tom.immersivehudplugin.ImmersiveHudPlugin;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;

public final class GlobalConfigStore {

    private final ImmersiveHudPlugin plugin;
    private final ConfigSupport configSupport;
    private final Path configPath;

    private GlobalConfig globalConfig = new GlobalConfig();

    public GlobalConfigStore(
            ImmersiveHudPlugin plugin,
            ConfigSupport configSupport
    ) {
        this.plugin = plugin;
        this.configSupport = configSupport;
        this.configPath = plugin.getDataDirectory().resolve("config.json");
    }

    public com.tom.immersivehudplugin.config.GlobalConfig get() {
        return globalConfig;
    }

    public void load() {

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

        if (result.changed()) { save(); }
    }

    public void save() {
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
        com.tom.immersivehudplugin.config.GlobalConfig cfg = new GlobalConfig();
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