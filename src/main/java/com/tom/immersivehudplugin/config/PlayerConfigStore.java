package com.tom.immersivehudplugin.config;

import com.hypixel.hytale.server.core.HytaleServer;

import com.tom.immersivehudplugin.ImmersiveHudPlugin;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerConfigStore {

    private final ImmersiveHudPlugin plugin;
    private final Path playersDir;
    private final ConfigSupport configSupport;

    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Map<UUID, com.tom.immersivehudplugin.config.PlayerConfig> cache = new ConcurrentHashMap<>();

    public PlayerConfigStore(
            ImmersiveHudPlugin plugin,
            ConfigSupport configSupport
    ) {
        this.plugin = plugin;
        this.playersDir = plugin.getDataDirectory().resolve("players");
        this.configSupport = configSupport;
    }

    public Path pathFor(UUID uuid) {
        return playersDir.resolve(uuid + ".json");
    }

    public com.tom.immersivehudplugin.config.PlayerConfig getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
        dirty.remove(uuid);
    }

    public com.tom.immersivehudplugin.config.PlayerConfig loadOrCreate(UUID uuid, GlobalConfig globalCfg) {
        com.tom.immersivehudplugin.config.PlayerConfig cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        Path file = pathFor(uuid);

        ConfigSupport.LoadResult<com.tom.immersivehudplugin.config.PlayerConfig> result = configSupport.loadOrCreate(
                file,
                () -> createDefaultPlayerConfig(globalCfg),
                ConfigSchemaValidator::isValidPlayerConfig,
                ConfigJsonMapper::fromJsonPlayer,
                com.tom.immersivehudplugin.config.PlayerConfig::sanitize,
                "player config does not match expected schema"
        );

        com.tom.immersivehudplugin.config.PlayerConfig cfg = result.config();
        cache.put(uuid, cfg);

        if (result.changed()) {
            markDirty(uuid);
            save(uuid);
        }

        return cfg;
    }

    public void save(UUID uuid) {
        if (uuid == null || !dirty.contains(uuid)) {
            return;
        }

        com.tom.immersivehudplugin.config.PlayerConfig cfg = cache.get(uuid);
        if (cfg == null) {
            dirty.remove(uuid);
            return;
        }

        try {
            Path file = pathFor(uuid);
            configSupport.writeJson(file, ConfigJsonMapper.toJson(cfg));
            dirty.remove(uuid);

        } catch (Throwable t) {
            plugin.getLogger().at(Level.WARNING).log(
                    " Failed to save player config for {} [{}]: {}",
                    uuid,
                    t.getClass().getSimpleName(),
                    t.getMessage()
            );
        }
    }

    public void saveAsync(UUID uuid) {

        if (uuid == null) { return; }
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> save(uuid));
    }

    public void saveAndUnload(UUID uuid) {
        save(uuid);
        unload(uuid);
    }

    public void markDirty(UUID uuid) {
        if (uuid != null) {
            dirty.add(uuid);
        }
    }

    private static com.tom.immersivehudplugin.config.PlayerConfig createDefaultPlayerConfig(GlobalConfig globalCfg) {
        return com.tom.immersivehudplugin.config.PlayerConfig.fromDefaults(
                globalCfg.getDefaultHudComponents(),
                globalCfg.getDefaultDynamicHud()
        );
    }
}