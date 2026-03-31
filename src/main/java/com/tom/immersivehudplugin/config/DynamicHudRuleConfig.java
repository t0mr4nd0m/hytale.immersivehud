package com.tom.immersivehudplugin.config;

import com.google.gson.annotations.SerializedName;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;
import java.util.Objects;

public final class DynamicHudRuleConfig {

    private static final float DEFAULT_THRESHOLD = 100f;

    @SerializedName("Rules")
    private String[] ruleNames = new String[0];

    @SerializedName("Threshold")
    private Float threshold = DEFAULT_THRESHOLD;

    private transient EnumSet<DynamicHudTriggers> parsedRules;
    private transient boolean parsedRulesDirty = true;

    public String[] getRuleNames() {
        return ruleNames != null ? ruleNames.clone() : new String[0];
    }

    public float getThreshold() {
        return sanitizeThreshold(threshold);
    }

    public void setThreshold(Float value) {
        threshold = sanitizeThreshold(value);
    }

    public EnumSet<DynamicHudTriggers> getRules() {

        if (!parsedRulesDirty && parsedRules != null) { return EnumSet.copyOf(parsedRules); }

        parsedRules = parseRuleNames(ruleNames);
        parsedRulesDirty = false;

        return EnumSet.copyOf(parsedRules);
    }

    public void setRules(EnumSet<DynamicHudTriggers> rules) {

        EnumSet<DynamicHudTriggers> safeRules =
                (rules != null && !rules.isEmpty())
                        ? EnumSet.copyOf(rules)
                        : EnumSet.noneOf(DynamicHudTriggers.class);

        ruleNames = toRuleNames(safeRules);
        parsedRules = EnumSet.copyOf(safeRules);
        parsedRulesDirty = false;
    }

    public boolean addRule(DynamicHudTriggers rule) {

        if (rule == null) { return false; }

        EnumSet<DynamicHudTriggers> set = getRules();
        boolean changed = set.add(rule);

        if (changed) { setRules(set); }

        return changed;
    }

    public boolean removeRule(DynamicHudTriggers rule) {

        if (rule == null) { return false; }

        EnumSet<DynamicHudTriggers> set = getRules();
        boolean changed = set.remove(rule);

        if (changed) { setRules(set); }

        return changed;
    }

    public boolean sanitize() {

        boolean changed = false;

        String[] normalized = toRuleNames(getRules());
        if (!sameContents(ruleNames, normalized)) {
            ruleNames = normalized;
            parsedRules = parseRuleNames(normalized);
            parsedRulesDirty = false;
            changed = true;
        }

        float sanitizedThreshold = sanitizeThreshold(threshold);
        if (threshold == null || Float.compare(threshold, sanitizedThreshold) != 0) {
            threshold = sanitizedThreshold;
            changed = true;
        }

        return changed;
    }

    public DynamicHudRuleConfig copy() {

        DynamicHudRuleConfig c = new DynamicHudRuleConfig();
        c.setRules(getRules());
        c.setThreshold(getThreshold());

        return c;
    }

    private static EnumSet<DynamicHudTriggers> parseRuleNames(String[] values) {

        EnumSet<DynamicHudTriggers> set = EnumSet.noneOf(DynamicHudTriggers.class);

        //noinspection RedundantLengthCheck
        if (values == null || values.length == 0) { return set; }

        for (String value : values) {

            DynamicHudTriggers rule = DynamicHudTriggers.fromString(value);
            if (rule != null) { set.add(rule); }
        }

        return set;
    }

    private static String[] toRuleNames(EnumSet<DynamicHudTriggers> rules) {

        if (rules == null || rules.isEmpty()) { return new String[0]; }

        return rules.stream()
                .map(Enum::name)
                .toArray(String[]::new);
    }

    private static boolean sameContents(String[] a, String[] b) {

        if (a == b) { return true; }
        if (a == null || b == null || a.length != b.length) { return false; }

        for (int i = 0; i < a.length; i++) {
            if (!Objects.equals(a[i], b[i])) { return false; }
        }

        return true;
    }

    private static float sanitizeThreshold(Float value) {

        if (value == null || Float.isNaN(value) || Float.isInfinite(value)) { return DEFAULT_THRESHOLD; }

        return Math.max(0f, Math.min(100f, value));
    }
}