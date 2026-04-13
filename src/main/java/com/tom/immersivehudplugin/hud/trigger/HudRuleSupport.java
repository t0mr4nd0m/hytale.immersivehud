package com.tom.immersivehudplugin.hud.trigger;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.stream.Collectors;

public final class HudRuleSupport {

    private HudRuleSupport() {
    }

    public static String formatRules(@Nullable EnumSet<HudTrigger> rules) {
        if (rules == null || rules.isEmpty()) {
            return "(none)";
        }

        return rules.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    public static String formatThreshold(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }

    @Nullable
    public static Float parseThreshold(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            float value = Float.parseFloat(raw.trim());
            if (Float.isNaN(value) || Float.isInfinite(value)) {
                return null;
            }
            if (value < 0f || value > 100f) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}