package com.tom.immersivehudplugin.config;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlayerConfigService {

    private final PlayerConfigStore playerConfigStore;
    private final Supplier<GlobalConfig> globalConfigSupplier;

    public PlayerConfigService(
            PlayerConfigStore playerConfigStore,
            Supplier<GlobalConfig> globalConfigSupplier
    ) {
        this.playerConfigStore = playerConfigStore;
        this.globalConfigSupplier = globalConfigSupplier;
    }

    public PlayerConfig getPlayerConfig(PlayerRef playerRef) {
        return getOrLoadPlayerConfig(playerRef.getUuid());
    }

    public PlayerConfig getOrLoadPlayerConfig(UUID uuid) {
        // Reconnecting also cancels a deferred unload left by a failed disconnect save.
        return playerConfigStore.loadOrCreate(uuid, getGlobalConfig());
    }

    @Nullable
    public PlayerConfig getCachedPlayerConfig(UUID uuid) {
        return playerConfigStore.getCached(uuid);
    }

    public void saveAndUnload(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }

        playerConfigStore.saveAndUnload(playerRef.getUuid());
    }

    public void flush() {
        playerConfigStore.flush();
    }

    public void updatePlayerConfig(PlayerRef playerRef, Consumer<PlayerConfig> mutator) {
        if (playerRef == null) { return; }

        UUID uuid = playerRef.getUuid();
        playerConfigStore.update(uuid, getGlobalConfig(), mutator);
    }

    private GlobalConfig getGlobalConfig() {
        return globalConfigSupplier.get();
    }
}
