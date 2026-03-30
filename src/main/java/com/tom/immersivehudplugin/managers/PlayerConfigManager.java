package com.tom.immersivehudplugin.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.config.ConfigJsonMapper;
import com.tom.immersivehudplugin.config.ConfigSchemaValidator;
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

    public Path pathFor(UUID uuid) {
        return playersDir.resolve(uuid + ".json");
    }

    public PlayerConfig getCached(UUID uuid) {
        return cache.get(uuid);
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
                com.google.gson.JsonElement root;
                try (Reader reader = Files.newBufferedReader(file)) {
                    root = com.google.gson.JsonParser.parseReader(reader);
                }

                if (!ConfigSchemaValidator.isValidPlayerConfig(root)) {
                    throw new IllegalStateException("player config does not match expected schema");
                }

                cfg = ConfigJsonMapper.fromJsonPlayer(root.getAsJsonObject());
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

            Path file = pathFor(uuid);
            backupBrokenPlayerFile(file);

            PlayerConfig fallback = PlayerConfig.fromDefaults(
                    globalCfg.getDefaultHudComponents(),
                    globalCfg.getDefaultDynamicHud()
            );

            cache.put(uuid, fallback);
            markDirty(uuid);
            save(uuid);

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
                gson.toJson(ConfigJsonMapper.toJson(cfg), writer);
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

    private void backupBrokenPlayerFile(Path file) {
        try {
            if (file == null || !Files.exists(file)) {
                return;
            }

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"));

            Path backup = file.resolveSibling(file.getFileName().toString() + ".broken-" + timestamp);
            Files.move(file, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            plugin.getLogger().at(Level.WARNING).log(
                    "Backed up invalid player config to " + backup
            );
        } catch (Throwable moveEx) {
            plugin.getLogger().at(Level.WARNING).log(
                    "Failed to back up broken player config "
                            + file
                            + " [" + moveEx.getClass().getSimpleName() + "]: "
                            + moveEx.getMessage()
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
}