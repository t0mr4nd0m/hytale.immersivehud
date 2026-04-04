package com.tom.immersivehudplugin.config;

import com.google.gson.annotations.SerializedName;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;
import java.util.Objects;

public final class DynamicHudRuleConfig {

    public static final float DEFAULT_THRESHOLD = 100f;

    @SerializedName("Rules")
    private String[] ruleNames = new String[0];

    @SerializedName("Threshold")
    private Float threshold = DEFAULT_THRESHOLD;

    private transient EnumSet<DynamicHudTriggers> parsedRules;

    public String[] getRuleNames() {
        return ruleNames != null ? ruleNames.clone() : new String[0];
    }

    public float getThreshold() {
        return sanitizeThreshold(threshold);
    }

    public void setThreshold(Float value) {
        this.threshold = sanitizeThreshold(value);
    }

    public EnumSet<DynamicHudTriggers> getRules() {
        if (parsedRules == null) {
            parsedRules = parseRuleNames(ruleNames);
        }

        return copyRules(parsedRules);
    }

    public void setRules(EnumSet<DynamicHudTriggers> rules) {
        EnumSet<DynamicHudTriggers> safeRules = normalizeRules(rules);

        this.ruleNames = toRuleNames(safeRules);
        this.parsedRules = copyRules(safeRules);
    }

    public boolean addRule(DynamicHudTriggers rule) {
        if (rule == null) {
            return false;
        }

        EnumSet<DynamicHudTriggers> rules = getRules();
        boolean changed = rules.add(rule);

        if (changed) {
            setRules(rules);
        }

        return changed;
    }

    public boolean removeRule(DynamicHudTriggers rule) {
        if (rule == null) {
            return false;
        }

        EnumSet<DynamicHudTriggers> rules = getRules();
        boolean changed = rules.remove(rule);

        if (changed) {
            setRules(rules);
        }

        return changed;
    }

    public boolean sanitize() {
        boolean changed = false;

        EnumSet<DynamicHudTriggers> normalizedRules = parseRuleNames(ruleNames);
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

    private static EnumSet<DynamicHudTriggers> normalizeRules(EnumSet<DynamicHudTriggers> rules) {
        if (rules == null || rules.isEmpty()) {
            return EnumSet.noneOf(DynamicHudTriggers.class);
        }

        return copyRules(rules);
    }

    private static EnumSet<DynamicHudTriggers> parseRuleNames(String[] values) {
        EnumSet<DynamicHudTriggers> rules = EnumSet.noneOf(DynamicHudTriggers.class);

        if (values == null) {
            return rules;
        }

        for (String value : values) {
            DynamicHudTriggers trigger = DynamicHudTriggers.fromString(value);
            if (trigger != null) {
                rules.add(trigger);
            }
        }

        return rules;
    }

    private static String[] toRuleNames(EnumSet<DynamicHudTriggers> rules) {
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

    private static EnumSet<DynamicHudTriggers> copyRules(EnumSet<DynamicHudTriggers> rules) {
        return rules == null || rules.isEmpty()
                ? EnumSet.noneOf(DynamicHudTriggers.class)
                : EnumSet.copyOf(rules);
    }
}