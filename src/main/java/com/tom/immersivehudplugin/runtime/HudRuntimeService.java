package com.tom.immersivehudplugin.runtime;

import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.config.PlayerConfigService;
import com.tom.immersivehudplugin.runtime.context.HudBarStateUpdater;
import com.tom.immersivehudplugin.runtime.context.HudTriggerContextFactory;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContextFactory;
import com.tom.immersivehudplugin.runtime.signal.CombatSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.HeldItemSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.HudSignalPipeline;
import com.tom.immersivehudplugin.runtime.signal.MovementSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.ReticleSignalTracker;
import com.tom.immersivehudplugin.runtime.visibility.HudVisibilityCoordinator;

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
    private final PlayerConfigService playerConfigService;
    private final Supplier<GlobalConfig> globalConfigSupplier;

    private final HeldItemSignalTracker heldItemSignalTracker;
    private final HudTickProcessor hudTickProcessor;

    private final Map<UUID, PlayerHudState> playerState = new ConcurrentHashMap<>();

    private volatile boolean running;
    private volatile boolean inboundRegistered;
    private volatile ScheduledFuture<?> tickTask;
    private volatile int runningIntervalMs = -1;

    public HudRuntimeService(
            JavaPlugin plugin,
            PlayerConfigService playerConfigService,
            HudVisibilityCoordinator hudVisibilityCoordinator,
            Supplier<GlobalConfig> globalConfigSupplier,
            int healthState,
            int staminaState,
            int manaState,
            int oxygenState
    ) {
        this.plugin = plugin;
        this.playerConfigService = playerConfigService;
        this.globalConfigSupplier = globalConfigSupplier;

        this.heldItemSignalTracker = new HeldItemSignalTracker();
        MovementSignalTracker movementSignalTracker = new MovementSignalTracker();
        ReticleSignalTracker reticleSignalTracker = new ReticleSignalTracker();
        CombatSignalTracker combatSignalTracker = new CombatSignalTracker(plugin);
        HudSignalPipeline hudSignalPipeline = new HudSignalPipeline(
                heldItemSignalTracker,
                movementSignalTracker,
                reticleSignalTracker,
                combatSignalTracker
        );
        HudBarStateUpdater barUpdater = new HudBarStateUpdater(
                healthState,
                staminaState,
                manaState,
                oxygenState
        );
        HudTriggerContextFactory triggerContextFactory = new HudTriggerContextFactory();
        PlayerTickContextFactory tickContextFactory = new PlayerTickContextFactory();
        this.hudTickProcessor = new HudTickProcessor(
                tickContextFactory,
                hudSignalPipeline,
                barUpdater,
                triggerContextFactory,
                hudVisibilityCoordinator,
                heldItemSignalTracker
        );
    }

    public void start() {
        running = true;
        registerInboundWatcher();
        restartTickTaskIfNeeded();
    }

    public void shutdown() {
        running = false;

        ScheduledFuture<?> task = tickTask;
        tickTask = null;
        runningIntervalMs = -1;

        if (task != null) task.cancel(false);

        playerState.keySet().forEach(playerConfigService::save);
        playerState.clear();
    }

    public void restartTickTaskIfNeeded() {

        if (!running) return;

        GlobalConfig global = getGlobalConfig();
        int wantedInterval = intervalMs(global);
        if (wantedInterval <= 0) wantedInterval = GlobalConfig.INTERVAL_MS;

        if (isTickTaskAlreadyRunningFor(wantedInterval)) return;

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

    public void onPlayerReady(@Nullable PlayerRef playerRef) {
        if (!running || playerRef == null || !playerRef.isValid()) return;

        GlobalConfig global = getGlobalConfig();

        int hideDelay = hideDelayMs(global);
        int initialGrace = initialHudGraceMs(global);

        PlayerHudState state = stateFor(playerRef.getUuid());

        state.reset(hideDelay);
        state.startInitialHudVisibleGrace(nowMs(), initialGrace);
    }

    public void onPlayerDisconnect(@Nullable PlayerRef playerRef) {
        if (playerRef == null) return;

        playerState.remove(playerRef.getUuid());
    }

    public void onPlayerConfigChanged(@Nullable PlayerRef playerRef) {
        if (!running || playerRef == null || !playerRef.isValid()) return;

        PlayerHudState state = playerState.get(playerRef.getUuid());

        if (state == null) return;

        state.markStaticHudDirty();
        state.invalidateDynamicHudEnabledCache();
    }

    private void registerInboundWatcher() {
        if (inboundRegistered) return;
        inboundRegistered = true;

        PacketAdapters.registerInbound((PlayerPacketWatcher) (playerRef, packet) -> {
            if (!running) return;

            if (playerRef == null || !playerRef.isValid()) return;

            if (!(packet instanceof SyncInteractionChains updates)) return;

            PlayerHudState state = playerState.get(playerRef.getUuid());

            if (state == null) return;

            long now = nowMs();

            heldItemSignalTracker.applyPacketBatch(state, updates, now);

            heldItemSignalTracker.cleanupWeaponSignals(state);
        });
    }

    private void tickReadyPlayers() {
        if (!running) return;

        Universe universe = Universe.get();
        GlobalConfig global = getGlobalConfig();
        playerState.forEach((uuid, state) -> processReadyPlayerTick(universe, uuid, state, global));
    }

    private void processReadyPlayerTick(
            Universe universe,
            UUID uuid,
            PlayerHudState state,
            GlobalConfig global
    ) {
        state.hideDelayMs = hideDelayMs(global);

        if (!state.tryMarkTickPending()) return;

        ResolvedPlayerWorld resolved = resolvePlayerWorld(universe, uuid);
        if (resolved == null) {
            state.clearTickPending();
            return;
        }

        try {
            resolved.world().execute(() -> {
                try {
                    processReadyPlayerTickOnWorldThread(resolved.uuid(), resolved.worldUuid(), state, global);
                } catch (Exception exception) {
                    plugin.getLogger()
                            .at(Level.WARNING)
                            .withCause(exception)
                            .log("Failed to process HUD tick for " + uuid);
                } finally {
                    state.clearTickPending();
                }
            });
        } catch (Exception exception) {
            state.clearTickPending();

            plugin.getLogger()
                    .at(Level.WARNING)
                    .withCause(exception)
                    .log("Failed to queue HUD tick for " + uuid);
        }
    }

    private void processReadyPlayerTickOnWorldThread(
            UUID uuid,
            UUID expectedWorldUuid,
            PlayerHudState expectedState,
            GlobalConfig global
    ) {
        if (!running) return;

        if (playerState.get(uuid) != expectedState) return;

        ResolvedPlayerWorld resolved =
                revalidatePlayerWorldOnWorldThread(
                        uuid,
                        expectedWorldUuid
                );

        if (resolved == null) return;

        long now = nowMs();

        if (expectedState.isInitialHudVisibleGraceActive(now)) return;

        expectedState.finishInitialHudVisibleGrace();

        PlayerConfig playerConfig = playerConfigService.getCachedPlayerConfig(uuid);

        if (playerConfig == null) return;

        hudTickProcessor.processPlayerTick(
                resolved.playerRef(),
                resolved.world(),
                global,
                now,
                expectedState,
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
        if (current != null) current.cancel(false);
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

    private int initialHudGraceMs(GlobalConfig config) {
        return config != null ? config.getInitialHudGraceMs() : GlobalConfig.INITIAL_HUD_GRACE_MS;
    }

    private int intervalMs(GlobalConfig config) {
        return config != null ? config.getIntervalMs() : GlobalConfig.INTERVAL_MS;
    }

    private static long nowMs() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    private record ResolvedPlayerWorld(
            UUID uuid,
            PlayerRef playerRef,
            UUID worldUuid,
            World world
    ) {}

    @Nullable
    private ResolvedPlayerWorld resolvePlayerWorld(Universe universe, UUID uuid) {
        PlayerRef playerRef = universe.getPlayer(uuid);
        if (playerRef == null || !playerRef.isValid()) return null;

        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid == null) return null;

        World world = universe.getWorld(worldUuid);
        if (world == null || !world.isAlive()) return null;

        return new ResolvedPlayerWorld(uuid, playerRef, worldUuid, world);
    }

    @Nullable
    private ResolvedPlayerWorld revalidatePlayerWorldOnWorldThread(
            UUID uuid,
            UUID expectedWorldUuid
    ) {
        Universe universe = Universe.get();

        PlayerRef playerRef = universe.getPlayer(uuid);
        if (playerRef == null || !playerRef.isValid()) return null;

        if (!expectedWorldUuid.equals(playerRef.getWorldUuid())) return null;

        World world = universe.getWorld(expectedWorldUuid);
        if (world == null || !world.isAlive()) return null;

        return new ResolvedPlayerWorld(uuid, playerRef, expectedWorldUuid, world);
    }
}