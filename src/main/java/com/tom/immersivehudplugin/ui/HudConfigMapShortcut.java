package com.tom.immersivehudplugin.ui;

import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.protocol.packets.player.ClientMovement;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapVisible;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.window.ClientOpenWindow;
import com.hypixel.hytale.protocol.packets.window.WindowType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.tom.immersivehudplugin.config.GlobalConfig;
import java.util.function.Supplier;

/** Opens configuration when the player performs the configured action while crouching. */
public final class HudConfigMapShortcut {
    private final HudConfigUiService uiService;
    private final Supplier<GlobalConfig> config;
    private PacketFilter registration;
    private volatile boolean active;
    private final HudShortcutInputState inputState = new HudShortcutInputState();

    public HudConfigMapShortcut(HudConfigUiService uiService, Supplier<GlobalConfig> config) {
        this.uiService = uiService;
        this.config = config;
    }

    public void start(JavaPlugin plugin) {
        if (registration != null) return;
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class,
                event -> inputState.remove(event.getPlayerRef().getUuid()));
        active = true;
        registration = PacketAdapters.registerInbound((PlayerPacketFilter) (playerRef, packet) -> {
            if (!active) return false;
            var worldUuid = playerRef.getWorldUuid();
            if (worldUuid == null) return false;
            if (packet instanceof ClientMovement movement && movement.movementStates != null) {
                inputState.update(playerRef.getUuid(), worldUuid, movement.movementStates.crouching);
                return false;
            }
            String action = actionForPacket(packet);
            var shortcut = config.get().getConfigShortcut();
            boolean crouching = inputState.isCrouching(playerRef.getUuid(), worldUuid);
            if (action == null || !shortcut.matches(crouching) || !action.equals(shortcut.getAction())) return false;
            var world = Universe.get().getWorld(worldUuid);
            if (world == null || !world.isAlive()) return false;

            // Packet callbacks run off the world thread; access player components only there.
            world.execute(() -> {
                if (!active || !worldUuid.equals(playerRef.getWorldUuid())) return;
                var ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) return;
                var store = ref.getStore();
                if (store.getExternalData().getWorld() != world) return;
                var currentShortcut = config.get().getConfigShortcut();
                if (!currentShortcut.matches(crouching) || !action.equals(currentShortcut.getAction())) return;
                if (uiService.getSession(playerRef) != null) return;
                uiService.open(playerRef, ref, store);
            });
            // Only consume crafting when the shortcut matched. Otherwise inventory works normally.
            return "OPEN_POCKET_CRAFTING".equals(action);
        });
    }

    public static String actionForPacket(Packet packet) {
        if (packet instanceof UpdateWorldMapVisible map && map.visible) return "OPEN_MAP";
        if (packet instanceof ClientOpenWindow window && window.type == WindowType.PocketCrafting) {
            return "OPEN_POCKET_CRAFTING";
        }
        return null;
    }

    public void shutdown() {
        active = false;
        inputState.clear();
        if (registration == null) return;
        PacketAdapters.deregisterInbound(registration);
        registration = null;
    }
}
