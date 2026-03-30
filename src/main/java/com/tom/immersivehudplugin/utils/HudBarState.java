package com.tom.immersivehudplugin.utils;

public final class HudBarState {

    private float current;
    private float max;

    public void update(float current, float max) {
        this.current = current;
        this.max = max;
    }

    public boolean isValid() {
        return max > 0f;
    }

    public float ratio() {
        return isValid() ? (current / max) : 1f;
    }

    public float percent() {
        return ratio() * 100f;
    }

    public boolean isBelowPercent(float percent) {
        return isValid() && percent() < percent;
    }

    public void reset() {
        current = max = 0f;
    }
}