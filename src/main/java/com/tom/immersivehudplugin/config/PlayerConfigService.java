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

    @Nullable
    public PlayerConfig requirePlayerConfig(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }

        return getOrLoadPlayerConfig(playerRef.getUuid());
    }

    public PlayerConfig getOrLoadPlayerConfig(UUID uuid) {
        PlayerConfig cached = playerConfigStore.getCached(uuid);
        if (cached != null) {
            return cached;
        }

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

    public void save(UUID uuid) {
        playerConfigStore.save(uuid);
    }

    public void updatePlayerConfig(PlayerRef playerRef, Consumer<PlayerConfig> mutator) {
        if (playerRef == null) { return; }

        UUID uuid = playerRef.getUuid();
        PlayerConfig config = getOrLoadPlayerConfig(uuid);

        mutator.accept(config);

        playerConfigStore.markDirty(uuid);
        playerConfigStore.saveAsync(uuid);
    }

    public void updateDynamicHud(PlayerRef playerRef, Consumer<DynamicHudConfig> mutator) {
        updatePlayerConfig(playerRef, config -> mutator.accept(config.getDynamicHud()));
    }

    private GlobalConfig getGlobalConfig() {
        return globalConfigSupplier.get();
    }
}