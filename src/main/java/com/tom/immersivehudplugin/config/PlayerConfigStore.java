package com.tom.immersivehudplugin.config;

import com.google.gson.JsonElement;
import com.hypixel.hytale.server.core.HytaleServer;
import com.tom.immersivehudplugin.ImmersiveHudPlugin;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class PlayerConfigStore {
    @FunctionalInterface
    interface JsonWriter {
        void write(Path path, JsonElement json) throws Exception;
    }

    private final Path playersDir;
    private final BiFunction<UUID, GlobalConfig, ConfigSupport.LoadResult<PlayerConfig>> loader;
    private final JsonWriter writer;
    private final Executor executor;
    private final BiConsumer<UUID, Exception> saveFailure;
    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    public PlayerConfigStore(ImmersiveHudPlugin plugin, ConfigSupport support) {
        this(plugin.getDataDirectory().resolve("players"),
                (uuid, global) -> support.loadOrCreate(
                        plugin.getDataDirectory().resolve("players").resolve(uuid + ".json"),
                        () -> PlayerConfig.fromDefaults(global.getDefaultHudComponents(), global.getDefaultDynamicHud()),
                        ConfigSchemaValidator::isValidPlayerConfig, ConfigJsonMapper::fromJsonPlayer,
                        PlayerConfig::sanitize, "player config does not match expected schema"),
                support::writeJson,
                HytaleServer.SCHEDULED_EXECUTOR,
                (uuid, failure) -> plugin.getLogger().at(Level.WARNING).withCause(failure)
                        .log("Failed to save player config for " + uuid + "; changes remain pending"));
    }

    PlayerConfigStore(Path playersDir,
                      BiFunction<UUID, GlobalConfig, ConfigSupport.LoadResult<PlayerConfig>> loader,
                      JsonWriter writer, Executor executor, BiConsumer<UUID, Exception> saveFailure) {
        this.playersDir = playersDir;
        this.loader = loader;
        this.writer = writer;
        this.executor = executor;
        this.saveFailure = saveFailure;
    }

    public Path pathFor(UUID uuid) {
        return playersDir.resolve(uuid + ".json");
    }

    /** Published configurations are read-only; changes must go through update(). */
    @Nullable
    public PlayerConfig getCached(UUID uuid) {
        Entry entry = cache.get(uuid);
        return entry == null ? null : entry.config;
    }

    public PlayerConfig loadOrCreate(UUID uuid, GlobalConfig global) {
        while (true) {
            boolean[] created = {false};
            Entry entry = cache.computeIfAbsent(uuid, key -> {
                var loaded = loader.apply(key, global);
                created[0] = true;
                return new Entry(loaded.config(), loaded.changed());
            });
            PlayerConfig config;
            boolean pending;
            synchronized (entry.stateLock) {
                if (cache.get(uuid) != entry) continue;
                entry.unloadRequested = false;
                config = entry.config;
                pending = entry.revision != entry.savedRevision;
            }
            if (pending) {
                if (created[0]) saveEntry(uuid, entry);
                else saveAsync(uuid);
            }
            return config;
        }
    }

    public void update(UUID uuid, GlobalConfig global, Consumer<PlayerConfig> mutator) {
        while (true) {
            Entry entry = cache.get(uuid);
            if (entry == null) {
                loadOrCreate(uuid, global);
                continue;
            }
            synchronized (entry.stateLock) {
                if (cache.get(uuid) != entry) continue;
                PlayerConfig updated = entry.config.copy();
                mutator.accept(updated);
                entry.config = updated;
                entry.revision++;
                entry.unloadRequested = false;
            }
            saveAsync(uuid);
            return;
        }
    }

    public boolean save(@Nullable UUID uuid) {
        Entry entry = uuid == null ? null : cache.get(uuid);
        return entry == null || saveEntry(uuid, entry);
    }

    private boolean saveEntry(UUID uuid, Entry entry) {
        // Async saves, disconnects and shutdown all serialize on the same entry.
        synchronized (entry.saveLock) {
            PlayerConfig snapshot;
            long revision;
            synchronized (entry.stateLock) {
                // An old queued save must never overwrite a newer session.
                if (cache.get(uuid) != entry) return true;
                if (entry.revision == entry.savedRevision) {
                    unloadIfSaved(uuid, entry);
                    return true;
                }
                snapshot = entry.config.copy();
                revision = entry.revision;
            }
            try {
                writer.write(pathFor(uuid), ConfigJsonMapper.toJson(snapshot));
            } catch (Exception failure) {
                saveFailure.accept(uuid, failure);
                return false;
            }
            synchronized (entry.stateLock) {
                entry.savedRevision = revision;
                unloadIfSaved(uuid, entry);
            }
            return true;
        }
    }

    private void unloadIfSaved(UUID uuid, Entry entry) {
        if (entry.unloadRequested && entry.revision == entry.savedRevision) cache.remove(uuid, entry);
    }

    public void saveAsync(@Nullable UUID uuid) {
        Entry entry = uuid == null ? null : cache.get(uuid);
        if (entry == null) return;
        synchronized (entry.stateLock) {
            if (cache.get(uuid) != entry || entry.saveQueued || entry.revision == entry.savedRevision) return;
            entry.saveQueued = true;
        }
        try {
            executor.execute(() -> {
                boolean saved = false;
                boolean repeat;
                try {
                    saved = saveEntry(uuid, entry);
                } finally {
                    synchronized (entry.stateLock) {
                        entry.saveQueued = false;
                        repeat = saved && cache.get(uuid) == entry && entry.revision != entry.savedRevision;
                    }
                }
                // Edits during I/O need another save; failures stay pending without a busy retry loop.
                if (repeat) saveAsync(uuid);
            });
        } catch (RuntimeException failure) {
            synchronized (entry.stateLock) { entry.saveQueued = false; }
            saveFailure.accept(uuid, failure);
        }
    }

    public boolean saveAndUnload(UUID uuid) {
        Entry entry = cache.get(uuid);
        if (entry == null) return true;
        synchronized (entry.stateLock) { entry.unloadRequested = true; }
        return saveEntry(uuid, entry);
    }

    /** Includes disconnected players whose last save failed. */
    public void flush() {
        cache.keySet().forEach(this::save);
    }

    private static final class Entry {
        private final Object stateLock = new Object();
        private final Object saveLock = new Object();
        private volatile PlayerConfig config;
        private long revision;
        private long savedRevision;
        private boolean saveQueued;
        private boolean unloadRequested;

        private Entry(PlayerConfig config, boolean pending) {
            this.config = config;
            this.revision = pending ? 1 : 0;
        }
    }
}
