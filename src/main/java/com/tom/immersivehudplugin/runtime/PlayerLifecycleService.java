package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.tom.immersivehudplugin.config.PlayerConfigService;

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
        PlayerRef playerRef = event.getPlayer().getPlayerRef();

        playerConfigService.getOrLoadPlayerConfig(playerRef.getUuid());
        hudRuntimeService.onPlayerReady(playerRef);
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        hudRuntimeService.onPlayerDisconnect(playerRef);
        playerConfigService.saveAndUnload(playerRef);
    }
}