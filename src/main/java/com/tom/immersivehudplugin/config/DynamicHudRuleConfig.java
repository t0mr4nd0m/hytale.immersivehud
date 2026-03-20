package com.tom.immersivehudplugin.config;

import com.google.gson.annotations.SerializedName;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;
import java.util.Objects;

public final class DynamicHudRuleConfig {

    @SerializedName("Rules")
    private String[] ruleNames = new String[0];

    private transient EnumSet<DynamicHudTriggers> parsedRules;
    private transient boolean parsedRulesDirty = true;

    private transient long rulesMask;
    private transient boolean rulesMaskDirty = true;

    public String[] getRuleNames() {
        return ruleNames != null ? ruleNames.clone() : new String[0];
    }

    public void setRuleNames(String[] values) {
        ruleNames = normalizeRuleNames(values);
        parsedRulesDirty = true;
        rulesMaskDirty = true;
    }

    public EnumSet<DynamicHudTriggers> getRules() {
        if (!parsedRulesDirty && parsedRules != null) {
            return EnumSet.copyOf(parsedRules);
        }

        parsedRules = parseRuleNames(ruleNames);
        parsedRulesDirty = false;
        rulesMaskDirty = true;

        return EnumSet.copyOf(parsedRules);
    }

    public long getRulesMask() {
        if (!rulesMaskDirty) {
            return rulesMask;
        }

        if (parsedRulesDirty || parsedRules == null) {
            parsedRules = parseRuleNames(ruleNames);
            parsedRulesDirty = false;
        }

        rulesMask = DynamicHudTriggers.toMask(parsedRules);
        rulesMaskDirty = false;
        return rulesMask;
    }

    public void setRules(EnumSet<DynamicHudTriggers> rules) {
        EnumSet<DynamicHudTriggers> safeRules =
                (rules != null && !rules.isEmpty())
                        ? EnumSet.copyOf(rules)
                        : EnumSet.noneOf(DynamicHudTriggers.class);

        ruleNames = toRuleNames(safeRules);
        parsedRules = EnumSet.copyOf(safeRules);
        parsedRulesDirty = false;
        rulesMask = DynamicHudTriggers.toMask(safeRules);
        rulesMaskDirty = false;
    }

    public boolean addRule(DynamicHudTriggers rule) {
        if (rule == null) {
            return false;
        }

        EnumSet<DynamicHudTriggers> set = getRules();
        boolean changed = set.add(rule);

        if (changed) {
            setRules(set);
        }

        return changed;
    }

    public boolean removeRule(DynamicHudTriggers rule) {
        if (rule == null) {
            return false;
        }

        EnumSet<DynamicHudTriggers> set = getRules();
        boolean changed = set.remove(rule);

        if (changed) {
            setRules(set);
        }

        return changed;
    }

    public boolean sanitize() {
        String[] normalized = toRuleNames(getRules());

        if (!sameContents(ruleNames, normalized)) {
            ruleNames = normalized;
            parsedRules = parseRuleNames(normalized);
            parsedRulesDirty = false;
            rulesMask = DynamicHudTriggers.toMask(parsedRules);
            rulesMaskDirty = false;
            return true;
        }

        return false;
    }

    public DynamicHudRuleConfig copy() {
        DynamicHudRuleConfig c = new DynamicHudRuleConfig();
        c.setRules(getRules());
        return c;
    }

    private static EnumSet<DynamicHudTriggers> parseRuleNames(String[] values) {
        EnumSet<DynamicHudTriggers> set = EnumSet.noneOf(DynamicHudTriggers.class);

        if (values == null || values.length == 0) {
            return set;
        }

        for (String value : values) {
            DynamicHudTriggers rule = DynamicHudTriggers.fromString(value);
            if (rule != null) {
                set.add(rule);
            }
        }

        return set;
    }

    private static String[] toRuleNames(EnumSet<DynamicHudTriggers> rules) {
        if (rules == null || rules.isEmpty()) {
            return new String[0];
        }

        return rules.stream()
                .map(Enum::name)
                .toArray(String[]::new);
    }

    private static String[] normalizeRuleNames(String[] values) {
        return toRuleNames(parseRuleNames(values));
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
}