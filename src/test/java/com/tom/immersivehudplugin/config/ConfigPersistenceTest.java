package com.tom.immersivehudplugin.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConfigPersistenceTest {
    private static final Gson GSON = new Gson();
    private static int checks;

    static void main() throws Exception {
        Path root = Files.createTempDirectory(Path.of(".").toAbsolutePath(), "ihud-persistence-");
        try {
            verifyAtomicFiles(root.resolve("atomic"));
            verifySnapshotsAndCoalescing(root.resolve("coalescing"));
            verifyConcurrentSaves(root.resolve("concurrent"));
            verifyDisconnectOrdering(root.resolve("disconnect"));
            verifyFailures(root.resolve("failures"));
            System.out.println("Passed " + checks + " configuration persistence regression checks.");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
            }
        }
    }

    private static void verifyAtomicFiles(Path directory) throws Exception {
        Files.createDirectories(directory);
        Path file = directory.resolve("config.json");
        Files.writeString(file, "{\"value\":1}");
        try {
            ConfigSupport.writeAtomically(file, writer -> {
                writer.write("{\"value\":");
                writer.flush();
                verify("original remains readable during temporary write", Files.readString(file).equals("{\"value\":1}"));
                throw new IOException("simulated disk failure");
            });
            throw new AssertionError("Expected write failure");
        } catch (IOException expected) {
            verify("failed write preserves original bytes", Files.readString(file).equals("{\"value\":1}"));
        }
        verify("failed write cleans temporary file", temporaryCount(directory) == 0);
        ConfigSupport.writeAtomically(file, writer -> writer.write("{\"value\":2}"));
        verify("successful atomic replacement", JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                .get("value").getAsInt() == 2);
        verify("successful replacement leaves no temporary file", temporaryCount(directory) == 0);

        Path missing = directory.resolve("new.json");
        try {
            ConfigSupport.writeAtomically(missing, _ -> { throw new IOException("simulated failure"); });
            throw new AssertionError("Expected write failure");
        } catch (IOException expected) {
            verify("failed first write does not publish an incomplete file", !Files.exists(missing));
        }
        Path blocked = directory.resolve("blocked.json");
        Files.createDirectory(blocked);
        Path marker = blocked.resolve("marker");
        Files.writeString(marker, "keep");
        try {
            ConfigSupport.writeAtomically(blocked, writer -> writer.write("{}"));
            throw new AssertionError("Expected replacement failure");
        } catch (IOException expected) {
            verify("replacement failure leaves destination intact", Files.readString(marker).equals("keep"));
            verify("replacement failure cleans temporary file", temporaryCount(directory) == 0);
        }
    }

    private static void verifySnapshotsAndCoalescing(Path directory) throws Exception {
        var tasks = new ManualExecutor();
        var fixture = new Fixture(directory, tasks);
        PlayerConfig original = fixture.load();
        fixture.writes.clear();
        fixture.update(25f);
        verify("updates do not mutate previously published configs", original.getDynamicHud().getHealth().getThreshold() == 100f);
        fixture.update(50f);
        verify("multiple edits share one pending task", tasks.size() == 1);
        tasks.drain();
        verify("coalesced save writes latest revision", fixture.writes.size() == 1 && fixture.readThreshold() == 50f);
        verify("clean save is a no-op", fixture.store.save(fixture.id) && fixture.writes.size() == 1);

        PlayerConfig beforeFailure = fixture.store.getCached(fixture.id);
        try {
            fixture.store.update(fixture.id, fixture.global, config -> {
                config.getDynamicHud().getHealth().setThreshold(12f);
                throw new IllegalStateException("invalid edit");
            });
            throw new AssertionError("Expected mutator failure");
        } catch (IllegalStateException expected) {
            verify("failed mutation does not publish partial config", fixture.store.getCached(fixture.id) == beforeFailure);
            verify("failed mutation does not schedule a write", tasks.size() == 0);
        }
    }

    private static void verifyConcurrentSaves(Path directory) throws Exception {
        var tasks = new ManualExecutor();
        var fixture = new Fixture(directory, tasks);
        fixture.load();
        UUID other = UUID.randomUUID();
        fixture.store.loadOrCreate(other, fixture.global);
        fixture.writes.clear();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var active = new AtomicInteger();
        var maximum = new AtomicInteger();
        fixture.behavior = (path, json) -> {
            if (path.equals(fixture.store.pathFor(fixture.id))) {
                int count = active.incrementAndGet();
                maximum.accumulateAndGet(count, Math::max);
                try {
                    if (threshold(json) == 25f) {
                        entered.countDown();
                        await(release);
                    }
                    write(path, json);
                } finally {
                    active.decrementAndGet();
                }
            } else write(path, json);
        };
        try (var workers = Executors.newFixedThreadPool(3)) {
            fixture.update(25f);
            Future<?> first = workers.submit(tasks::runOne);
            try {
                await(entered);
                fixture.update(50f);
                verify("world-thread edits are not blocked by disk I/O", fixture.store.getCached(fixture.id)
                        .getDynamicHud().getHealth().getThreshold() == 50f);
                fixture.store.update(other, fixture.global, config -> config.getDynamicHud().getHealth().setThreshold(77f));
                verify("other players can save independently", workers.submit(() -> fixture.store.save(other)).get(5, TimeUnit.SECONDS));
                verify("other player's file is complete", threshold(JsonParser.parseString(Files.readString(fixture.store.pathFor(other)))) == 77f);
                Future<Boolean> overlapping = workers.submit(() -> fixture.store.save(fixture.id));
                release.countDown();
                first.get(5, TimeUnit.SECONDS);
                verify("concurrent explicit save succeeds", overlapping.get(5, TimeUnit.SECONDS));
                tasks.drain();
                verify("same-player writes never overlap", maximum.get() == 1);
                verify("newer edits survive completion of older save", fixture.readThreshold() == 50f);
                List<Float> revisions = fixture.writes.stream().filter(json -> threshold(json) != 77f)
                        .map(ConfigPersistenceTest::threshold).toList();
                verify("same-player revisions are written in order", revisions.equals(List.of(25f, 50f)));
            } finally {
                release.countDown();
            }
        }
    }

    private static void verifyDisconnectOrdering(Path directory) throws Exception {
        var tasks = new ManualExecutor();
        var fixture = new Fixture(directory, tasks);
        fixture.load();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        fixture.behavior = (path, json) -> {
            if (threshold(json) == 25f) { entered.countDown(); await(release); }
            write(path, json);
        };
        try (var workers = Executors.newFixedThreadPool(2)) {
            fixture.update(25f);
            Future<?> first = workers.submit(tasks::runOne);
            try {
                await(entered);
                fixture.update(50f);
                Future<Boolean> disconnect = workers.submit(() -> fixture.store.saveAndUnload(fixture.id));
                release.countDown();
                first.get(5, TimeUnit.SECONDS);
                verify("disconnect waits for ordered save", disconnect.get(5, TimeUnit.SECONDS));
                verify("disconnect persists latest revision", fixture.readThreshold() == 50f);
                verify("successful disconnect unloads config", fixture.store.getCached(fixture.id) == null);
            } finally {
                release.countDown();
            }
        }
        fixture.behavior = ConfigPersistenceTest::write;
        fixture.load();
        fixture.update(60f);
        verify("save before queued task can unload safely", fixture.store.saveAndUnload(fixture.id));
        fixture.load();
        fixture.update(90f);
        fixture.writes.clear();
        tasks.drain();
        verify("old queued tasks cannot overwrite reconnected session", fixture.readThreshold() == 90f
                && fixture.writes.size() == 1);
    }

    private static void verifyFailures(Path directory) throws Exception {
        var tasks = new ManualExecutor();
        var fixture = new Fixture(directory.resolve("disconnect"), tasks);
        fixture.load();
        fixture.behavior = (_, _) -> { throw new IOException("disk full"); };
        fixture.update(35f);
        tasks.drain();
        verify("failed async save is reported without a retry loop", fixture.failures.size() == 1 && tasks.size() == 0);
        verify("failure preserves last good file", fixture.readThreshold() == 100f);
        verify("failed disconnect reports failure", !fixture.store.saveAndUnload(fixture.id));
        verify("failed disconnect retains unsaved configuration", fixture.store.getCached(fixture.id)
                .getDynamicHud().getHealth().getThreshold() == 35f);
        fixture.behavior = ConfigPersistenceTest::write;
        fixture.store.flush();
        verify("shutdown flush retries disconnected dirty players", fixture.readThreshold() == 35f);
        verify("successful retry completes deferred unload", fixture.store.getCached(fixture.id) == null);

        var reconnect = new Fixture(directory.resolve("reconnect"), new ManualExecutor());
        reconnect.load();
        reconnect.behavior = (_, _) -> { throw new IOException("disk full"); };
        reconnect.update(65f);
        reconnect.store.saveAndUnload(reconnect.id);
        var service = new PlayerConfigService(reconnect.store, () -> reconnect.global);
        PlayerConfig retained = service.getOrLoadPlayerConfig(reconnect.id);
        verify("reconnect recovers unsaved edits instead of stale disk config", retained.getDynamicHud().getHealth().getThreshold() == 65f);
        reconnect.behavior = ConfigPersistenceTest::write;
        reconnect.store.flush();
        verify("reconnect cancels deferred unload", reconnect.store.getCached(reconnect.id) != null
                && reconnect.readThreshold() == 65f);

        var rejected = new Fixture(directory.resolve("rejected"), _ -> { throw new RejectedExecutionException("stopped"); });
        rejected.load();
        rejected.update(45f);
        verify("executor rejection is reported", rejected.failures.size() == 1);
        verify("rejected tasks retain edits for synchronous flush", rejected.store.save(rejected.id) && rejected.readThreshold() == 45f);
    }

    private static void write(Path path, JsonElement json) throws IOException {
        ConfigSupport.writeAtomically(path, writer -> GSON.toJson(json, writer));
    }

    private static float threshold(JsonElement json) {
        return ConfigJsonMapper.fromJsonPlayer(json.getAsJsonObject()).getDynamicHud().getHealth().getThreshold();
    }

    private static long temporaryCount(Path directory) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().startsWith(".ihud-")).count();
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for test coordination");
    }

    private static void verify(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }

    private static final class ManualExecutor implements Executor {
        private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
        @Override public void execute(Runnable task) { tasks.add(task); }
        int size() { return tasks.size(); }
        void runOne() {
            Runnable task = tasks.poll();
            if (task == null) throw new AssertionError("No pending save");
            task.run();
        }
        void drain() {
            int runs = 0;
            while (!tasks.isEmpty()) {
                if (++runs > 20) throw new AssertionError("Unexpected save retry loop");
                runOne();
            }
        }
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        final GlobalConfig global = new GlobalConfig();
        final List<JsonElement> writes = new CopyOnWriteArrayList<>();
        final List<Exception> failures = new ArrayList<>();
        final PlayerConfigStore store;
        volatile PlayerConfigStore.JsonWriter behavior = ConfigPersistenceTest::write;

        Fixture(Path directory, Executor executor) {
            store = new PlayerConfigStore(directory, (uuid, defaults) -> {
                Path file = directory.resolve(uuid + ".json");
                try {
                    if (Files.exists(file)) return new ConfigSupport.LoadResult<>(
                            ConfigJsonMapper.fromJsonPlayer(JsonParser.parseString(Files.readString(file)).getAsJsonObject()), false, false);
                } catch (IOException failure) {
                    throw new IllegalStateException(failure);
                }
                return new ConfigSupport.LoadResult<>(PlayerConfig.fromDefaults(
                        defaults.getDefaultHudComponents(), defaults.getDefaultDynamicHud()), true, true);
            }, (path, json) -> {
                writes.add(json.deepCopy());
                behavior.write(path, json);
            }, executor, (_, failure) -> failures.add(failure));
        }
        PlayerConfig load() { return store.loadOrCreate(id, global); }
        void update(float threshold) {
            store.update(id, global, config -> config.getDynamicHud().getHealth().setThreshold(threshold));
        }
        float readThreshold() throws IOException {
            return threshold(JsonParser.parseString(Files.readString(store.pathFor(id))));
        }
    }
}
