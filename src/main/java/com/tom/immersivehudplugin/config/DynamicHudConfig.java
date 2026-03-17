package com.tom.immersivehudplugin.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.tom.immersivehudplugin.rules.DynamicHudTriggers;

import java.util.EnumSet;

public final class DynamicHudConfig {

    public static final BuilderCodec<DynamicHudConfig> CODEC =
            BuilderCodec.builder(DynamicHudConfig.class, DynamicHudConfig::new)

                    .append(new KeyedCodec<>("Hotbar", DynamicHudRuleConfig.CODEC),
                            (cfg, v) -> cfg.hotbar = v,
                            DynamicHudConfig::getHotbar)
                    .add()

                    .append(new KeyedCodec<>("Reticle", DynamicHudRuleConfig.CODEC),
                            (cfg, v) -> cfg.reticle = v,
                            DynamicHudConfig::getReticle)
                    .add()

                    .append(new KeyedCodec<>("Compass", DynamicHudRuleConfig.CODEC),
                            (cfg, v) -> cfg.compass = v,
                            DynamicHudConfig::getCompass)
                    .add()

                    .append(new KeyedCodec<>("Health", DynamicHudRuleConfig.CODEC),
                            (cfg, v) -> cfg.health = v,
                            DynamicHudConfig::getHealth)
                    .add()

                    .append(new KeyedCodec<>("Stamina", DynamicHudRuleConfig.CODEC),
                            (cfg, v) -> cfg.stamina = v,
                            DynamicHudConfig::getStamina)
                    .add()

                    .append(new KeyedCodec<>("Mana", DynamicHudRuleConfig.CODEC),
                            (cfg, v) -> cfg.mana = v,
                            DynamicHudConfig::getMana)
                    .add()

                    .build();

    private DynamicHudRuleConfig hotbar = defaultHotbar();
    private DynamicHudRuleConfig reticle = defaultReticle();
    private DynamicHudRuleConfig compass = defaultCompass();
    private DynamicHudRuleConfig health = defaultHealth();
    private DynamicHudRuleConfig stamina = defaultStamina();
    private DynamicHudRuleConfig mana = defaultMana();

    private static DynamicHudRuleConfig defaultHotbar() {
        DynamicHudRuleConfig r = new DynamicHudRuleConfig();
        r.setRules(EnumSet.of(DynamicHudTriggers.HOTBAR_INPUT));
        return r;
    }

    private static DynamicHudRuleConfig defaultReticle() {
        DynamicHudRuleConfig r = new DynamicHudRuleConfig();
        r.setRules(EnumSet.of(
                DynamicHudTriggers.HOLDING_RANGED_WEAPON,
                DynamicHudTriggers.CHARGING_WEAPON,
                DynamicHudTriggers.CONSUMABLE_USE,
                DynamicHudTriggers.TARGET_ENTITY,
                DynamicHudTriggers.INTERACTABLE_BLOCK
        ));
        return r;
    }

    private static DynamicHudRuleConfig defaultCompass() {
        DynamicHudRuleConfig r = new DynamicHudRuleConfig();
        r.setRules(EnumSet.of(DynamicHudTriggers.PLAYER_MOVING));
        return r;
    }

    private static DynamicHudRuleConfig defaultHealth() {
        DynamicHudRuleConfig r = new DynamicHudRuleConfig();
        r.setRules(EnumSet.of(DynamicHudTriggers.HEALTH_NOT_FULL));
        return r;
    }

    private static DynamicHudRuleConfig defaultStamina() {
        DynamicHudRuleConfig r = new DynamicHudRuleConfig();
        r.setRules(EnumSet.of(DynamicHudTriggers.STAMINA_CRITICAL));
        return r;
    }

    private static DynamicHudRuleConfig defaultMana() {
        DynamicHudRuleConfig r = new DynamicHudRuleConfig();
        r.setRules(EnumSet.of(DynamicHudTriggers.MANA_CRITICAL));
        return r;
    }

    public DynamicHudRuleConfig getHotbar() {
        if (hotbar == null) hotbar = defaultHotbar();
        return hotbar;
    }

    public void setHotbar(DynamicHudRuleConfig v) {
        hotbar = (v != null) ? v : defaultHotbar();
    }

    public DynamicHudRuleConfig getReticle() {
        if (reticle == null) reticle = defaultReticle();
        return reticle;
    }

    public void setReticle(DynamicHudRuleConfig v) {
        reticle = (v != null) ? v : defaultReticle();
    }

    public DynamicHudRuleConfig getCompass() {
        if (compass == null) compass = defaultCompass();
        return compass;
    }

    public void setCompass(DynamicHudRuleConfig v) {
        compass = (v != null) ? v : defaultCompass();
    }

    public DynamicHudRuleConfig getHealth() {
        if (health == null) health = defaultHealth();
        return health;
    }

    public void setHealth(DynamicHudRuleConfig v) {
        health = (v != null) ? v : defaultHealth();
    }

    public DynamicHudRuleConfig getStamina() {
        if (stamina == null) stamina = defaultStamina();
        return stamina;
    }

    public void setStamina(DynamicHudRuleConfig v) {
        stamina = (v != null) ? v : defaultStamina();
    }

    public DynamicHudRuleConfig getMana() {
        if (mana == null) mana = defaultMana();
        return mana;
    }

    public void setMana(DynamicHudRuleConfig v) {
        mana = (v != null) ? v : defaultMana();
    }

    public boolean sanitize() {
        boolean changed = false;

        if (hotbar == null) { hotbar = defaultHotbar(); changed = true; }
        else changed |= hotbar.sanitize();

        if (reticle == null) { reticle = defaultReticle(); changed = true; }
        else changed |= reticle.sanitize();

        if (compass == null) { compass = defaultCompass(); changed = true; }
        else changed |= compass.sanitize();

        if (health == null) { health = defaultHealth(); changed = true; }
        else changed |= health.sanitize();

        if (stamina == null) { stamina = defaultStamina(); changed = true; }
        else changed |= stamina.sanitize();

        if (mana == null) { mana = defaultMana(); changed = true; }
        else changed |= mana.sanitize();

        return changed;
    }

    public DynamicHudConfig copy() {
        DynamicHudConfig c = new DynamicHudConfig();

        c.setHotbar(getHotbar() != null ? getHotbar().copy() : new DynamicHudRuleConfig());
        c.setReticle(getReticle() != null ? getReticle().copy() : new DynamicHudRuleConfig());
        c.setCompass(getCompass() != null ? getCompass().copy() : new DynamicHudRuleConfig());
        c.setHealth(getHealth() != null ? getHealth().copy() : new DynamicHudRuleConfig());
        c.setStamina(getStamina() != null ? getStamina().copy() : new DynamicHudRuleConfig());
        c.setMana(getMana() != null ? getMana().copy() : new DynamicHudRuleConfig());

        return c;
    }
}