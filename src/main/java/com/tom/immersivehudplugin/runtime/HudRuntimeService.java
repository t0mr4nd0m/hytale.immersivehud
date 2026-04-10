package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.context.HudContextBuilder;
import com.tom.immersivehudplugin.context.PlayerTickContext;
import com.tom.immersivehudplugin.managers.PlayerConfigManager;
import com.tom.immersivehudplugin.visibility.HudVisibilityService;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class HudRuntimeService {

    private final JavaPlugin plugin;
    private final PlayerConfigManager playerConfigManager;
    private final HudContextBuilder hudContextBuilder;
    private final HudVisibilityService hudVisibilityService;
    private final HeldItemTracker heldItemTracker;
    private final Supplier<GlobalConfig> globalConfigSupplier;

    private final HudTickProcessor hudTickProcessor;

    private final Map<UUID, PlayerHudState> playerState = new ConcurrentHashMap<>();

    private volatile boolean inboundRegistered;
    private volatile ScheduledFuture<?> tickTask;
    private volatile int runningIntervalMs = -1;

    public HudRuntimeService(
            JavaPlugin plugin,
            PlayerConfigManager playerConfigManager,
            HudContextBuilder hudContextBuilder,
            HudVisibilityService hudVisibilityService,
            Supplier<GlobalConfig> globalConfigSupplier
    ) {
        this.plugin = plugin;
        this.playerConfigManager = playerConfigManager;
        this.hudContextBuilder = hudContextBuilder;
        this.hudVisibilityService = hudVisibilityService;
        this.heldItemTracker = new HeldItemTracker();
        this.hudTickProcessor = new HudTickProcessor(
                playerConfigManager,
                hudContextBuilder,
                hudVisibilityService,
                heldItemTracker
        );
        this.globalConfigSupplier = globalConfigSupplier;
    }

    public void start() {
        registerInboundWatcher();
        registerPlayerEvents();
        restartTickTaskIfNeeded();
    }

    public void shutdown() {
        ScheduledFuture<?> task = tickTask;
        tickTask = null;

        if (task != null) {
            task.cancel(true);
        }

        for (UUID uuid : playerState.keySet()) {
            playerConfigManager.save(uuid);
        }

        playerState.clear();
    }

    public void restartTickTaskIfNeeded() {
        GlobalConfig global = getGlobalConfig();
        int wantedInterval = intervalMs(global);
        if (wantedInterval <= 0) {
            wantedInterval = GlobalConfig.INTERVAL_MS;
        }

        if (isTickTaskAlreadyRunningFor(wantedInterval)) { return; }

        cancelCurrentTickTask();

        runningIntervalMs = wantedInterval;
        tickTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                tickReadyPlayers();
            } catch (Throwable t) {
                plugin.getLogger().at(Level.WARNING).withCause(t).log("ImmersiveHud tick crashed");
            }
        }, 0, wantedInterval, TimeUnit.MILLISECONDS);
    }

    public void markPlayerStaticHudDirty(@Nullable PlayerRef playerRef) {
        if (playerRef == null) { return; }
        PlayerHudState state = stateFor(playerRef.getUuid());
        state.markStaticHudDirty();
        state.invalidateDynamicHudEnabledCache();
    }

    public void markPlayerConfigDirty(UUID uuid) {
        playerConfigManager.markDirty(uuid);
        PlayerHudState state = playerState.get(uuid);
        if (state != null) { state.invalidateDynamicHudEnabledCache(); }
    }

    @Nullable
    public PlayerConfig requirePlayerConfig(@Nullable PlayerRef playerRef) {
        if (playerRef == null) { return null; }
        return getOrLoadPlayerConfig(playerRef.getUuid());
    }

    public PlayerConfig getOrLoadPlayerConfig(UUID uuid) {
        PlayerConfig cached = playerConfigManager.getCached(uuid);
        if (cached != null) { return cached; }
        return playerConfigManager.loadOrCreate(uuid, getGlobalConfig());
    }

    public void applyAndSavePlayerConfig(PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        markPlayerConfigDirty(uuid);
        playerConfigManager.saveAsync(uuid);
        markPlayerStaticHudDirty(playerRef);
    }

    private void registerInboundWatcher() {

        if (inboundRegistered) { return; }
        inboundRegistered = true;

        PacketAdapters.registerInbound((PlayerPacketWatcher) (playerRef, packet) -> {

            if (!(packet instanceof SyncInteractionChains updates)) { return; }

            long now = nowMs();
            PlayerHudState state = stateFor(playerRef.getUuid());

            heldItemTracker.applyPacketBatch(state, updates, now);
            heldItemTracker.cleanupWeaponSignals(state);
        });
    }

    private void registerPlayerEvents() {

        plugin.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {

            @SuppressWarnings("removal")
            UUID uuid = event.getPlayer().getUuid();

            long now = nowMs();
            int hideDelay = hideDelayMs(getGlobalConfig());

            getOrLoadPlayerConfig(uuid);

            PlayerHudState state = stateFor(uuid);
            resetPlayerRuntimeState(state, hideDelay, now);
        });

        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
            UUID uuid = event.getPlayerRef().getUuid();
            playerState.remove(uuid);
            playerConfigManager.saveAndUnload(uuid);
        });
    }

    private void resetPlayerRuntimeState(PlayerHudState state, int hideDelay, long now) {
        state.reset(hideDelay);
        state.t.pulse(HudSignal.READY_GRACE, now, hideDelay);
        state.t.pulse(HudSignal.HOTBAR_INPUT, now, hideDelay);
    }

    private void tickReadyPlayers() {

        Universe universe = Universe.get();
        long now = nowMs();
        GlobalConfig global = getGlobalConfig();

        for (Map.Entry<UUID, PlayerHudState> entry : playerState.entrySet()) {
            processReadyPlayerTick(universe, entry.getKey(), entry.getValue(), global, now);
        }
    }

    private void processReadyPlayerTick(
            Universe universe,
            UUID uuid,
            PlayerHudState state,
            GlobalConfig global,
            long now
    ) {

        state.hideDelayMsHint = hideDelayMs(global);

        if (state.t.active(HudSignal.READY_GRACE, now)) { return; }

        ResolvedPlayerWorld resolved = resolvePlayerWorld(universe, uuid);
        if (resolved == null) { return; }

        resolved.world().execute(() ->
                processReadyPlayerTickOnWorldThread(resolved.uuid(), resolved.worldUuid(), global)
        );
    }

    private void processReadyPlayerTickOnWorldThread(
            UUID uuid,
            UUID expectedWorldUuid,
            GlobalConfig global
    ) {

        ResolvedPlayerWorld resolved = revalidatePlayerWorldOnWorldThread(uuid, expectedWorldUuid);
        if (resolved == null) { return; }

        long now = nowMs();
        checkAndToggle(resolved.playerRef(), resolved.world(), global, now);
    }

    private void checkAndToggle(
            PlayerRef playerRef,
            World world,
            GlobalConfig global,
            long now
    ) {
        UUID uuid = playerRef.getUuid();
        PlayerHudState state = stateFor(uuid);
        PlayerConfig playerConfig = getOrLoadPlayerConfig(uuid);

        hudTickProcessor.processPlayerTick(
                playerRef,
                world,
                global,
                now,
                state,
                playerConfig
        );
    }

    private boolean isTickTaskAlreadyRunningFor(int wantedInterval) {
        return tickTask != null
                && !tickTask.isCancelled()
                && !tickTask.isDone()
                && runningIntervalMs == wantedInterval;
    }

    private void cancelCurrentTickTask() {
        ScheduledFuture<?> current = tickTask;
        if (current != null) {
            current.cancel(false);
        }
    }

    private PlayerHudState stateFor(UUID uuid) {
        return playerState.computeIfAbsent(uuid, ignored -> new PlayerHudState());
    }

    private GlobalConfig getGlobalConfig() {
        return globalConfigSupplier.get();
    }

    private int hideDelayMs(GlobalConfig config) {
        return config != null ? config.getHideDelayMs() : GlobalConfig.HIDE_DELAY_MS;
    }

    private int intervalMs(GlobalConfig config) {
        return config != null ? config.getIntervalMs() : GlobalConfig.INTERVAL_MS;
    }

    private static long nowMs() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    private record TickEvaluation(
            PlayerHudState state,
            HudComponentsConfig hudConfig,
            DynamicHudConfig dynamicConfig,
            PlayerTickContext tickContext
    ) {}

    private record ResolvedPlayerWorld(
            UUID uuid,
            PlayerRef playerRef,
            UUID worldUuid,
            World world
    ) {}

    @Nullable
    private ResolvedPlayerWorld resolvePlayerWorld(
            Universe universe,
            UUID uuid
    ) {
        PlayerRef playerRef = universe.getPlayer(uuid);
        if (playerRef == null || !playerRef.isValid()) { return null; }

        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) { return null; }

        World world = universe.getWorld(worldUuid);
        if (world == null || !world.isAlive()) { return null; }

        return new ResolvedPlayerWorld(uuid, playerRef, worldUuid, world);
    }

    @Nullable
    private ResolvedPlayerWorld revalidatePlayerWorldOnWorldThread(
            UUID uuid,
            UUID expectedWorldUuid
    ) {
        Universe universe = Universe.get();

        PlayerRef playerRef = universe.getPlayer(uuid);
        if (playerRef == null || !playerRef.isValid()) { return null; }

        if (!expectedWorldUuid.equals(playerRef.getWorldUuid())) { return null; }

        World world = universe.getWorld(expectedWorldUuid);
        if (world == null || !world.isAlive()) { return null; }

        return new ResolvedPlayerWorld(uuid, playerRef, expectedWorldUuid, world);
    }
}