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

    public boolean isBelow(float d) {
        return isValid() && current * d < max;
    }

    public boolean isNotFull() {
        return isValid() && current < max;
    }

    public boolean isFull() { return isValid() && current >= max; }

    public void reset() {
        current = 0f;
        max = 0f;
    }
}