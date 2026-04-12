package com.tom.immersivehudplugin.config;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.runtime.HudRuntimeCoordinator;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class HudSettingsService {

    private final PlayerConfigManager playerConfigManager;
    private final HudRuntimeCoordinator hudRuntimeCoordinator;
    private final Supplier<GlobalConfig> globalConfigSupplier;

    public HudSettingsService(
            PlayerConfigManager playerConfigManager,
            HudRuntimeCoordinator hudRuntimeCoordinator,
            Supplier<GlobalConfig> globalConfigSupplier
    ) {
        this.playerConfigManager = playerConfigManager;
        this.hudRuntimeCoordinator = hudRuntimeCoordinator;
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
        PlayerConfig cached = playerConfigManager.getCached(uuid);
        if (cached != null) {
            return cached;
        }

        return playerConfigManager.loadOrCreate(uuid, getGlobalConfig());
    }

    public void updatePlayerConfig(PlayerRef playerRef, Consumer<PlayerConfig> mutator) {
        PlayerConfig config = getOrLoadPlayerConfig(playerRef.getUuid());
        mutator.accept(config);
        hudRuntimeCoordinator.applyAndSavePlayerConfig(playerRef);
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