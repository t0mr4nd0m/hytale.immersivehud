package com.tom.immersivehudplugin.config;

import com.google.gson.annotations.SerializedName;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.EnumSet;
import java.util.Objects;

public final class DynamicHudRuleConfig {

    public static final float DEFAULT_THRESHOLD = 100f;

    @SerializedName("Rules")
    private String[] ruleNames = new String[0];

    @SerializedName("Threshold")
    private Float threshold = DEFAULT_THRESHOLD;

    private transient EnumSet<HudTrigger> parsedRules;

    public String[] getRuleNames() {
        return ruleNames != null ? ruleNames.clone() : new String[0];
    }

    public float getThreshold() {
        return sanitizeThreshold(threshold);
    }

    public void setThreshold(Float value) {
        this.threshold = sanitizeThreshold(value);
    }

    public EnumSet<HudTrigger> getRules() {
        if (parsedRules == null) {
            parsedRules = parseRuleNames(ruleNames);
        }

        return copyRules(parsedRules);
    }

    public void setRules(EnumSet<HudTrigger> rules) {
        EnumSet<HudTrigger> safeRules = normalizeRules(rules);

        this.ruleNames = toRuleNames(safeRules);
        this.parsedRules = copyRules(safeRules);
    }

    public boolean addRule(HudTrigger rule) {
        if (rule == null) {
            return false;
        }

        EnumSet<HudTrigger> rules = getRules();
        boolean changed = rules.add(rule);

        if (changed) {
            setRules(rules);
        }

        return changed;
    }

    public boolean removeRule(HudTrigger rule) {
        if (rule == null) {
            return false;
        }

        EnumSet<HudTrigger> rules = getRules();
        boolean changed = rules.remove(rule);

        if (changed) {
            setRules(rules);
        }

        return changed;
    }

    public boolean sanitize() {
        boolean changed = false;

        EnumSet<HudTrigger> normalizedRules = parseRuleNames(ruleNames);
        String[] normalizedRuleNames = toRuleNames(normalizedRules);

        if (!sameContents(ruleNames, normalizedRuleNames)) {
            this.ruleNames = normalizedRuleNames;
            changed = true;
        }

        this.parsedRules = copyRules(normalizedRules);

        float sanitizedThreshold = sanitizeThreshold(threshold);
        if (threshold == null || Float.compare(threshold, sanitizedThreshold) != 0) {
            this.threshold = sanitizedThreshold;
            changed = true;
        }

        return changed;
    }

    public DynamicHudRuleConfig copy() {
        DynamicHudRuleConfig copy = new DynamicHudRuleConfig();
        copy.ruleNames = getRuleNames();
        copy.threshold = getThreshold();
        copy.parsedRules = copyRules(getRules());
        return copy;
    }

    private static EnumSet<HudTrigger> normalizeRules(EnumSet<HudTrigger> rules) {
        if (rules == null || rules.isEmpty()) {
            return EnumSet.noneOf(HudTrigger.class);
        }

        return copyRules(rules);
    }

    private static EnumSet<HudTrigger> parseRuleNames(String[] values) {
        EnumSet<HudTrigger> rules = EnumSet.noneOf(HudTrigger.class);

        if (values == null) {
            return rules;
        }

        for (String value : values) {
            HudTrigger trigger = HudTrigger.fromString(value);
            if (trigger != null) {
                rules.add(trigger);
            }
        }

        return rules;
    }

    private static String[] toRuleNames(EnumSet<HudTrigger> rules) {
        if (rules == null || rules.isEmpty()) {
            return new String[0];
        }

        return rules.stream()
                .map(Enum::name)
                .toArray(String[]::new);
    }

    private static boolean sameContents(String[] a, String[] b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null || a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (!Objects.equals(a[i], b[i])) {
                return false;
            }
        }

        return true;
    }

    private static float sanitizeThreshold(Float value) {
        if (value == null || Float.isNaN(value) || Float.isInfinite(value)) {
            return DEFAULT_THRESHOLD;
        }

        return Math.max(0f, Math.min(100f, value));
    }

    private static EnumSet<HudTrigger> copyRules(EnumSet<HudTrigger> rules) {
        return rules == null || rules.isEmpty()
                ? EnumSet.noneOf(HudTrigger.class)
                : EnumSet.copyOf(rules);
    }

    public boolean hasActiveThresholdRule() {
        return getRules().stream().anyMatch(rule -> rule.source() == HudTrigger.Source.HUD_BAR);
    }

    public boolean hasRules() {
        return !getRules().isEmpty();
    }

    public static DynamicHudRuleConfig empty() {
        return EMPTY;
    }

    private static final DynamicHudRuleConfig EMPTY = new DynamicHudRuleConfig();
}