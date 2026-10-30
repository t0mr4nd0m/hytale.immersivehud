package com.tom.immersivehudplugin.runtime.signal;

import com.hypixel.hytale.component.spatial.KDTree;
import com.hypixel.hytale.component.spatial.SpatialData;
import com.tom.immersivehudplugin.config.GlobalConfig;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public final class CombatScanTest {
    private static int checks;

    static void main() {
        verifySpatialBoundaries();
        verifyViewGeometry();
        verifySpatialEquivalence();
        System.out.println("Passed " + checks + " combat spatial scan regression checks.");
    }

    private static void verifySpatialBoundaries() {
        var index = new KDTree<Integer>(_ -> true);
        var data = new SpatialData<Integer>();
        data.add(new Vector3d(0, 0, 0), 0);
        data.add(new Vector3d(12, 0, 0), 1);
        data.add(new Vector3d(0, -12, 0), 2);
        data.add(new Vector3d(0, 0, -12), 3);
        data.add(new Vector3d(12.001, 0, 0), 4);
        data.add(new Vector3d(9, 9, 0), 5);
        index.rebuild(data);
        var found = CombatSignalScanner.collectNearby(index, new Vector3d(), 12f);
        verify("query includes center and exact radius in all axes", new HashSet<>(found).equals(java.util.Set.of(0, 1, 2, 3)));
        verify("query excludes entities just beyond range", !found.contains(4));
        verify("query is spherical rather than a box", !found.contains(5));

        data.clear();
        data.add(new Vector3d(100, 100, 100), 6);
        index.rebuild(data);
        verify("index rebuild removes old candidates", CombatSignalScanner.collectNearby(index, new Vector3d(), 12f).isEmpty());
        verify("query uses current player position", CombatSignalScanner.collectNearby(index, new Vector3d(100, 100, 100), 12f).equals(List.of(6)));
        data.clear();
        index.rebuild(data);
        verify("empty index yields no candidates", CombatSignalScanner.collectNearby(index, new Vector3d(), 12f).isEmpty());
    }

    private static void verifyViewGeometry() {
        var origin = new Vector3d(0, 1, 0);
        var direction = new Vector3d(0, 0, 1);
        var config = new GlobalConfig();
        var view = CombatScanView.capture(origin, direction, config);
        verify("hostile ahead is inside FOV", view.insideFov(new Vector3d(0, 0, 10)));
        verify("hostile behind is outside FOV", !view.insideFov(new Vector3d(0, 0, -10)));
        verify("hostile at side is outside FOV", !view.insideFov(new Vector3d(10, 0, 0)));
        verify("left inside horizontal FOV", view.insideFov(new Vector3d(-10 * Math.tan(Math.toRadians(44)), 0, 10)));
        verify("right outside horizontal FOV", !view.insideFov(new Vector3d(10 * Math.tan(Math.toRadians(46)), 0, 10)));
        verify("above inside vertical FOV", view.insideFov(new Vector3d(0, 10 * Math.tan(Math.toRadians(34)), 10)));
        verify("above outside vertical FOV", !view.insideFov(new Vector3d(0, 10 * Math.tan(Math.toRadians(36)), 10)));
        verify("below inside vertical FOV", view.insideFov(new Vector3d(0, -10 * Math.tan(Math.toRadians(34)), 10)));
        verify("below outside vertical FOV", !view.insideFov(new Vector3d(0, -10 * Math.tan(Math.toRadians(36)), 10)));
        verify("coincident target is handled", view.insideFov(new Vector3d(0, 0, 0)));

        var upward = CombatScanView.capture(origin, new Vector3d(0, 1, 0), config);
        verify("vertical look accepts target above", upward.insideFov(new Vector3d(0, 10, 0)));
        verify("vertical look rejects level target", !upward.insideFov(new Vector3d(0, 0, 10)));
        var pitched = CombatScanView.capture(origin, new Vector3d(0, Math.tan(Math.toRadians(30)), 1), config);
        verify("pitch is included in vertical FOV", pitched.insideFov(new Vector3d(0, 10 * Math.tan(Math.toRadians(60)), 10)));

        origin.set(100, 100, 100);
        direction.set(0, 0, -1);
        config.setLosNpcTargetHeight(3);
        config.setPlayerCombatHorizontalFovDegrees(120);
        verify("scan view snapshots mutable origin and direction", view.insideFov(new Vector3d(0, 0, 10)));
        verify("scan view snapshots settings", view.targetHeight() == 1 && view.horizontalHalfFov() == 45);
        var changed = CombatScanView.capture(new Vector3d(0, 3, 0), new Vector3d(0, 0, 1), config);
        verify("next scan picks up changed FOV", changed.insideFov(new Vector3d(10 * Math.tan(Math.toRadians(55)), 0, 10)));
        verify("default settings work without config", CombatScanView.capture(new Vector3d(0, 1, 0), new Vector3d(0, 0, 1), null)
                .insideFov(new Vector3d(0, 0, 10)));
    }

    private static void verifySpatialEquivalence() {
        var random = new Random(20261030);
        var index = new KDTree<Integer>(_ -> true);
        var data = new SpatialData<Integer>();
        List<Vector3d> positions = new ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            double spread = i < 1000 ? 80 : 2000;
            var point = new Vector3d((random.nextDouble() - 0.5) * spread,
                    (random.nextDouble() - 0.5) * 40, (random.nextDouble() - 0.5) * spread);
            positions.add(point);
            data.add(point, i);
        }
        index.rebuild(data);
        long candidatesExamined = 0;
        for (int query = 0; query < 100; query++) {
            var center = new Vector3d((random.nextDouble() - 0.5) * 100,
                    (random.nextDouble() - 0.5) * 20, (random.nextDouble() - 0.5) * 100);
            float range = 4 + random.nextInt(21);
            double rangeSquared = (double) range * range;
            var expected = new HashSet<Integer>();
            for (int i = 0; i < positions.size(); i++) {
                if (positions.get(i).distanceSquared(center) <= rangeSquared) expected.add(i);
            }
            var candidates = CombatSignalScanner.collectNearby(index, center, range);
            candidatesExamined += candidates.size();
            var actual = new HashSet<Integer>();
            for (int id : candidates) {
                if (positions.get(id).distanceSquared(center) <= rangeSquared) actual.add(id);
            }
            verify("spatial query matches exhaustive range filter " + query, actual.equals(expected));
        }
        verify("distant population is excluded before per-NPC checks", candidatesExamined < 20000L * 100 / 10);
        System.out.println("Synthetic scenario: " + candidatesExamined
                + " candidate NPCs returned across 100 queries, versus 2000000 entries examined by exhaustive scans.");
    }

    private static void verify(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
