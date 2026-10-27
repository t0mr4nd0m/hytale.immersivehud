package com.tom.immersivehudplugin.runtime;

import javax.annotation.Nullable;

public final class DynamicHudCache {

    private volatile HudDetectionPlan plan;

    public void invalidate() {
        plan = null;
    }

    public void cache(HudDetectionPlan plan) {
        this.plan = plan;
    }

    @Nullable
    public HudDetectionPlan get() {
        return plan;
    }

    public void reset() {
        invalidate();
    }
}
