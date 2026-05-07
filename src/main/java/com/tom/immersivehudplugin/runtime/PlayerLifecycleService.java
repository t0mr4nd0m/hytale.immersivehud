package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.config.PlayerConfigService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class PlayerLifecycleService {

    private final JavaPlugin plugin;
    private final PlayerConfigService playerConfigService;
    private final HudRuntimeService hudRuntimeService;

    private boolean playerEventsRegistered;

    public PlayerLifecycleService(
            JavaPlugin plugin,
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService
    ) {
        this.plugin = plugin;
        this.playerConfigService = playerConfigService;
        this.hudRuntimeService = hudRuntimeService;
    }

    public void start() {
        if (playerEventsRegistered) {
            return;
        }
        playerEventsRegistered = true;

        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        PlayerRef playerRef = resolvePlayerRef(event.getPlayer());
        if  (playerRef == null) {
            return;
        }

        playerConfigService.getOrLoadPlayerConfig(playerRef.getUuid());
        hudRuntimeService.onPlayerReady(playerRef);
    }

    @Nullable
    private PlayerRef resolvePlayerRef(@Nonnull Player player) {
        Ref<EntityStore> ref = player.getReference();

        if (ref == null || !ref.isValid()) { return null; }
        Store<EntityStore> store = ref.getStore();

        return store.getComponent(ref, PlayerRef.getComponentType());
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        hudRuntimeService.onPlayerDisconnect(playerRef);
        playerConfigService.saveAndUnload(playerRef);
    }
}