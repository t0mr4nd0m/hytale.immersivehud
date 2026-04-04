package com.tom.immersivehudplugin.managers;

import com.hypixel.hytale.server.core.HytaleServer;

import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.config.ConfigJsonMapper;
import com.tom.immersivehudplugin.config.ConfigSchemaValidator;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerConfigManager {

    private final ImmersiveHudPlugin plugin;
    private final Path playersDir;
    private final ConfigSupport configSupport;

    private final Set<UUID> dirty = ConcurrentHashMap.newKeySet();
    private final Map<UUID, PlayerConfig> cache = new ConcurrentHashMap<>();

    public PlayerConfigManager(
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

    public PlayerConfig getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
        dirty.remove(uuid);
    }

    public PlayerConfig loadOrCreate(UUID uuid, GlobalConfig globalCfg) {
        PlayerConfig cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        Path file = pathFor(uuid);

        ConfigSupport.LoadResult<PlayerConfig> result = configSupport.loadOrCreate(
                file,
                () -> createDefaultPlayerConfig(globalCfg),
                ConfigSchemaValidator::isValidPlayerConfig,
                ConfigJsonMapper::fromJsonPlayer,
                PlayerConfig::sanitize,
                "player config does not match expected schema"
        );

        PlayerConfig cfg = result.config();
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

        PlayerConfig cfg = cache.get(uuid);
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

    private static PlayerConfig createDefaultPlayerConfig(GlobalConfig globalCfg) {
        return PlayerConfig.fromDefaults(
                globalCfg.getDefaultHudComponents(),
                globalCfg.getDefaultDynamicHud()
        );
    }
}