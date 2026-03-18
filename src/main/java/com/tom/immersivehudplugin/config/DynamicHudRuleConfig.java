package com.tom.immersivehudplugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;

public final class DynamicHudRuleConfig {

    public static final BuilderCodec<DynamicHudRuleConfig> CODEC =
            BuilderCodec.builder(DynamicHudRuleConfig.class, DynamicHudRuleConfig::new)

                    .append(new KeyedCodec<>("Rules", Codec.STRING),
                            DynamicHudRuleConfig::setRulesCsv,
                            DynamicHudRuleConfig::getRulesCsv)
                    .add()

                    .build();

    private String rulesCsv = "";

    private transient EnumSet<DynamicHudTriggers> parsedRules;
    private transient boolean parsedRulesDirty = true;

    private transient long rulesMask;
    private transient boolean rulesMaskDirty = true;

    public String getRulesCsv() {
        return (rulesCsv != null) ? rulesCsv : "";
    }

    public void setRulesCsv(String v) {
        rulesCsv = DynamicHudTriggers.toCsv(DynamicHudTriggers.parseCsv(v));
        parsedRulesDirty = true;
        rulesMaskDirty = true;
    }

    public EnumSet<DynamicHudTriggers> getRules() {
        if (!parsedRulesDirty && parsedRules != null) {
            return EnumSet.copyOf(parsedRules);
        }

        parsedRules = DynamicHudTriggers.parseCsv(rulesCsv);
        parsedRulesDirty = false;
        rulesMaskDirty = true;

        return EnumSet.copyOf(parsedRules);
    }

    public long getRulesMask() {
        if (!rulesMaskDirty) {
            return rulesMask;
        }

        if (parsedRulesDirty || parsedRules == null) {
            parsedRules = DynamicHudTriggers.parseCsv(rulesCsv);
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

        rulesCsv = DynamicHudTriggers.toCsv(safeRules);
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
        String normalized = DynamicHudTriggers.toCsv(getRules());
        if (!normalized.equals(getRulesCsv())) {
            rulesCsv = normalized;
            parsedRules = DynamicHudTriggers.parseCsv(normalized);
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
}