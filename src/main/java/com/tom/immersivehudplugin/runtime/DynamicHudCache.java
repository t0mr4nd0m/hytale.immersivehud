package com.tom.immersivehudplugin.runtime;

public final class DynamicHudCache {

    private volatile boolean known;
    private volatile boolean enabled;

    public void invalidate() {
        known = false;
    }

    public void cache(boolean enabled) {
        this.enabled = enabled;
        this.known = true;
    }

    public boolean isKnown() {
        return known;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reset() {
        known = false;
        enabled = false;
    }
}