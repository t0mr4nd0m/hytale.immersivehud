package com.tom.immersivehudplugin.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerConfigManager {

    private final ImmersiveHudPlugin plugin;
    private final Path playersDir;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private final Set<UUID> dirty = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final Map<UUID, PlayerConfig> cache = new ConcurrentHashMap<>();

    public PlayerConfigManager(ImmersiveHudPlugin plugin) {
        this.plugin = plugin;
        this.playersDir = plugin.getDataDirectory().resolve("players");
    }

    public Path playersDir() {
        return playersDir;
    }

    public Path pathFor(UUID uuid) {
        return playersDir.resolve(uuid + ".json");
    }

    public PlayerConfig getCached(UUID uuid) {
        return cache.get(uuid);
    }

    public void put(UUID uuid, PlayerConfig cfg) {
        cache.put(uuid, cfg);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public PlayerConfig loadOrCreate(
            UUID uuid,
            GlobalConfig globalCfg
    ) {
        PlayerConfig cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }

        try {
            Files.createDirectories(playersDir);

            Path file = pathFor(uuid);
            PlayerConfig cfg;

            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    cfg = gson.fromJson(reader, PlayerConfig.class);
                }

                if (cfg == null) {
                    cfg = PlayerConfig.fromDefaults(
                            globalCfg.getDefaultHudComponents(),
                            globalCfg.getDefaultDynamicHud()
                    );
                }
            } else {
                cfg = PlayerConfig.fromDefaults(
                        globalCfg.getDefaultHudComponents(),
                        globalCfg.getDefaultDynamicHud()
                );
            }

            boolean changed = cfg.sanitize();
            cache.put(uuid, cfg);

            if (!Files.exists(file) || changed) {
                markDirty(uuid);
                save(uuid);
            }

            return cfg;
        } catch (Throwable t) {
            plugin.getLogger().at(Level.WARNING).log(
                    "Failed to load player config for " + uuid
                            + " [" + t.getClass().getSimpleName() + "]: "
                            + t.getMessage()
            );

            PlayerConfig fallback = PlayerConfig.fromDefaults(
                    globalCfg.getDefaultHudComponents(),
                    globalCfg.getDefaultDynamicHud()
            );

            cache.put(uuid, fallback);
            return fallback;
        }
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
            Files.createDirectories(playersDir);

            Path file = pathFor(uuid);
            try (Writer writer = Files.newBufferedWriter(file)) {
                gson.toJson(cfg, writer);
            }

            dirty.remove(uuid);
        } catch (Throwable t) {
            plugin.getLogger().at(Level.WARNING).log(
                    "Failed to save player config for " + uuid
                            + " [" + t.getClass().getSimpleName() + "]: "
                            + t.getMessage()
            );
        }
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

    public void reset(UUID uuid) {
        if (uuid == null) {
            return;
        }

        cache.remove(uuid);
        dirty.remove(uuid);

        try {
            Files.deleteIfExists(pathFor(uuid));
        } catch (Throwable t) {
            plugin.getLogger().at(Level.WARNING).log(
                    "Failed to delete player config for " + uuid
                            + " [" + t.getClass().getSimpleName() + "]: "
                            + t.getMessage()
            );
        }
    }

    public boolean isDirty(UUID uuid) {
        return uuid != null && dirty.contains(uuid);
    }

    public void clearDirty(UUID uuid) {
        if (uuid != null) {
            dirty.remove(uuid);
        }
    }
}