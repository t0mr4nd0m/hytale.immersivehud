package com.tom.immersivehudplugin.runtime;

import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.concurrent.atomic.AtomicLongArray;

public final class HudTimers {

    private final AtomicLongArray untilMs = new AtomicLongArray(HudTrigger.values().length);

    public void pulse(HudTrigger signal, long now, long durationMs) {
        long newUntil = now + Math.max(0L, durationMs);
        int idx = signal.ordinal();

        while (true) {
            long prev = untilMs.get(idx);
            if (prev >= newUntil) {
                return;
            }
            if (untilMs.compareAndSet(idx, prev, newUntil)) {
                return;
            }
        }
    }

    public void clear(HudTrigger signal) {
        untilMs.set(signal.ordinal(), 0L);
    }

    public void clearAll() {
        for (int i = 0; i < untilMs.length(); i++) {
            untilMs.set(i, 0L);
        }
    }

    public boolean active(HudTrigger signal, long now) {
        return untilMs.get(signal.ordinal()) > now;
    }
}