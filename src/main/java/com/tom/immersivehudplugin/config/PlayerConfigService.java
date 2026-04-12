package com.tom.immersivehudplugin.config;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.tom.immersivehudplugin.runtime.HudRuntimeService;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PlayerConfigService {

    private final PlayerConfigStore playerConfigStore;
    private final HudRuntimeService hudRuntimeService;
    private final Supplier<GlobalConfig> globalConfigSupplier;

    public PlayerConfigService(
            PlayerConfigStore playerConfigStore,
            HudRuntimeService hudRuntimeService,
            Supplier<GlobalConfig> globalConfigSupplier
    ) {
        this.playerConfigStore = playerConfigStore;
        this.hudRuntimeService = hudRuntimeService;
        this.globalConfigSupplier = globalConfigSupplier;
    }

    @Nullable
    public com.tom.immersivehudplugin.config.PlayerConfig requirePlayerConfig(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }

        return getOrLoadPlayerConfig(playerRef.getUuid());
    }

    public com.tom.immersivehudplugin.config.PlayerConfig getOrLoadPlayerConfig(UUID uuid) {
        com.tom.immersivehudplugin.config.PlayerConfig cached = playerConfigStore.getCached(uuid);
        if (cached != null) {
            return cached;
        }

        return playerConfigStore.loadOrCreate(uuid, getGlobalConfig());
    }

    public void updatePlayerConfig(PlayerRef playerRef, Consumer<com.tom.immersivehudplugin.config.PlayerConfig> mutator) {
        com.tom.immersivehudplugin.config.PlayerConfig config = getOrLoadPlayerConfig(playerRef.getUuid());
        mutator.accept(config);
        hudRuntimeService.applyAndSavePlayerConfig(playerRef);
    }

    public void updateHudComponents(PlayerRef playerRef, Consumer<HudComponentsConfig> mutator) {
        updatePlayerConfig(playerRef, config -> mutator.accept(config.getHudComponents()));
    }

    public void updateDynamicHud(PlayerRef playerRef, Consumer<DynamicHudConfig> mutator) {
        updatePlayerConfig(playerRef, config -> mutator.accept(config.getDynamicHud()));
    }

    private GlobalConfig getGlobalConfig() {
        return globalConfigSupplier.get();
    }
}