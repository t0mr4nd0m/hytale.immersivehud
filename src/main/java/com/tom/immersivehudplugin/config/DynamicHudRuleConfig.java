package com.tom.immersivehudplugin.config;

import com.google.gson.annotations.SerializedName;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import java.util.EnumSet;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

public final class DynamicHudRuleConfig {

    public static final float DEFAULT_THRESHOLD = 100f;

    @SerializedName("Rules")
    private String[] ruleNames = new String[0];

    @SerializedName("Threshold")
    private Float threshold = DEFAULT_THRESHOLD;

    private transient EnumSet<HudTrigger> parsedRules;
    private transient Set<HudTrigger> rulesView;

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
        return copyRules(parsedRules());
    }

    /** Reusable read-only snapshot; later edits do not change previously returned snapshots. */
    public Set<HudTrigger> getRulesView() {
        parsedRules();
        return rulesView;
    }

    public boolean hasRule(@Nullable HudTrigger rule) {
        return rule != null && parsedRules().contains(rule);
    }

    public boolean hasSameRules(@Nullable DynamicHudRuleConfig other) {
        return other != null && parsedRules().equals(other.parsedRules());
    }

    public void setRules(@Nullable Set<HudTrigger> rules) {
        replaceRules(copyRules(rules));
        this.ruleNames = toRuleNames(parsedRules);
    }

    public boolean addRule(HudTrigger rule) {
        if (rule == null || hasRule(rule)) {
            return false;
        }

        EnumSet<HudTrigger> rules = getRules();
        rules.add(rule);
        replaceRules(rules);
        ruleNames = toRuleNames(rules);
        return true;
    }

    public boolean removeRule(HudTrigger rule) {
        if (!hasRule(rule)) {
            return false;
        }

        EnumSet<HudTrigger> rules = getRules();
        rules.remove(rule);
        replaceRules(rules);
        ruleNames = toRuleNames(rules);
        return true;
    }

    public boolean sanitize() {
        boolean changed = false;

        EnumSet<HudTrigger> normalizedRules = parseRuleNames(ruleNames);
        String[] normalizedRuleNames = toRuleNames(normalizedRules);

        if (!sameContents(ruleNames, normalizedRuleNames)) {
            this.ruleNames = normalizedRuleNames;
            changed = true;
        }

        replaceRules(normalizedRules);

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
        copy.replaceRules(copyRules(parsedRules()));
        return copy;
    }

    private EnumSet<HudTrigger> parsedRules() {
        if (parsedRules == null) {
            replaceRules(parseRuleNames(ruleNames));
        }
        return parsedRules;
    }

    // Takes ownership of a fresh set. Published sets are never mutated in place.
    private void replaceRules(EnumSet<HudTrigger> rules) {
        this.parsedRules = rules;
        this.rulesView = Collections.unmodifiableSet(rules);
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

    private static EnumSet<HudTrigger> copyRules(@Nullable Set<HudTrigger> rules) {
        return rules == null || rules.isEmpty()
                ? EnumSet.noneOf(HudTrigger.class)
                : EnumSet.copyOf(rules);
    }

    public boolean hasRules() {
        return !parsedRules().isEmpty();
    }
}
