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
import com.tom.immersivehudplugin.ui.HudConfigUiService;

import java.util.logging.Level;

public final class PlayerSessionService {

    private final JavaPlugin plugin;
    private final PlayerConfigService playerConfigService;
    private final HudRuntimeService hudRuntimeService;
    private final HudConfigUiService hudConfigUiService;

    private boolean playerEventsRegistered;

    public PlayerSessionService(
            JavaPlugin plugin,
            PlayerConfigService playerConfigService,
            HudRuntimeService hudRuntimeService,
            HudConfigUiService hudConfigUiService
    ) {
        this.plugin = plugin;
        this.playerConfigService = playerConfigService;
        this.hudRuntimeService = hudRuntimeService;
        this.hudConfigUiService = hudConfigUiService;
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
        Player player = event.getPlayer();
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid()) return;

        Store<EntityStore> store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        boolean newPlayer = playerConfigService.preparePlayerSession(playerRef);
        hudRuntimeService.onPlayerReady(playerRef);
        if (newPlayer) {
            try {
                hudConfigUiService.open(playerRef, ref, store);
            } catch (RuntimeException exception) {
                plugin.getLogger().at(Level.WARNING).withCause(exception)
                        .log("Failed to open initial HUD configuration for " + playerRef.getUuid());
            }
        }
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();

        hudRuntimeService.onPlayerDisconnect(playerRef);
        playerConfigService.saveAndUnload(playerRef);
    }
}
