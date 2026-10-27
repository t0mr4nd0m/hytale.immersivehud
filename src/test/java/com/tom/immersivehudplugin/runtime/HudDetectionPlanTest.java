package com.tom.immersivehudplugin.runtime;

import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;
import com.tom.immersivehudplugin.runtime.context.HudBarStateUpdater;
import com.tom.immersivehudplugin.runtime.context.HudTriggerContextFactory;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;
import com.tom.immersivehudplugin.runtime.signal.CombatSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.HeldItemSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.HudSignalPipeline;
import com.tom.immersivehudplugin.runtime.signal.MovementSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.ReticleSignalTracker;

import java.util.EnumSet;

public final class HudDetectionPlanTest {
    private static int checks;

    static void main() {
        PlayerConfig health = only("health", HudTrigger.HEALTH_NOT_FULL);
        HudDetectionPlan plan = build(health);
        verify("health only needs bars", plan.enabled() && plan.bars() && !plan.movement()
                && !plan.reticle() && !plan.heldItem() && !plan.hotbar() && !plan.interactions()
                && !plan.requires(HudTrigger.IN_COMBAT));

        var context = new PlayerTickContext(null, null, null, null, null, null);
        var state = new PlayerHudState();
        var heldTracker = new HeldItemSignalTracker();
        var pipeline = new HudSignalPipeline(heldTracker, new MovementSignalTracker(),
                new ReticleSignalTracker(), new CombatSignalTracker(null));
        // With no entity/store/world available, any unnecessary query would fail or change scan timestamps.
        heldTracker.refreshFromInventory(state, context, 10000, plan);
        pipeline.update(state, null, context, new GlobalConfig(), 10000, plan);
        verify("health skips inventory, reticle and combat scans", state.lastCombatScanMs == 0
                && state.lastReticleScanMs == 0 && !state.heldItem.hasAnyWeaponInHand());
        state.manaBar.update(25, 100);
        new HudBarStateUpdater(0, 1, 2, 3).update(state, context, plan);
        verify("only requested bars are updated", state.healthBar.percent() == 100
                && state.manaBar.percent() == 25 && !state.staminaBar.isValid());

        plan = build(only("reticle", HudTrigger.HOLDING_BUILDING_BLOCK));
        verify("construction only needs held item", plan.heldItem() && !plan.weapons()
                && !plan.hotbar() && !plan.interactions() && !plan.reticle() && !plan.bars());
        plan = build(only("hotbar", HudTrigger.HOTBAR_INPUT));
        verify("slot changes do not need item classification", plan.hotbar() && !plan.heldItem()
                && !plan.interactions() && !plan.weapons());
        plan = build(only("ammo", HudTrigger.HOLDING_RANGED_WEAPON));
        verify("holding ranged does not inspect active attacks", plan.heldItem() && plan.weapons()
                && !plan.interactions() && !plan.hotbar());
        plan = build(only("reticle", HudTrigger.HOLDING_MELEE_WEAPON));
        verify("holding melee needs weapon classification", plan.weapons() && !plan.interactions());
        plan = build(only("reticle", HudTrigger.CHARGING_WEAPON));
        verify("charging includes inventory and slot cancellation dependencies", plan.heldItem()
                && plan.weapons() && plan.interactions() && plan.hotbar());
        plan = build(only("reticle", HudTrigger.BLOCKING_ATTACK));
        verify("blocking includes inventory and slot cancellation dependencies", plan.weapons()
                && plan.interactions() && plan.hotbar());
        plan = build(only("reticle", HudTrigger.CONSUMABLE_USE));
        verify("consumable use does not classify weapons", plan.heldItem() && plan.interactions()
                && !plan.weapons() && !plan.hotbar());
        plan = build(only("compass", HudTrigger.PLAYER_FLYING));
        verify("movement does not need other detectors", plan.movement() && !plan.reticle()
                && !plan.heldItem() && !plan.bars());
        pipeline.update(state, null, context, new GlobalConfig(), 10000, plan);
        verify("movement skips combat and reticle scans", state.lastCombatScanMs == 0 && state.lastReticleScanMs == 0);
        plan = build(only("reticle", HudTrigger.TARGET_ENTITY));
        verify("entity targeting does not require block targeting", plan.reticle()
                && plan.requires(HudTrigger.TARGET_ENTITY) && !plan.requires(HudTrigger.INTERACTABLE_BLOCK));
        plan = build(only("reticle", HudTrigger.INTERACTABLE_BLOCK));
        verify("block targeting does not require entity targeting", plan.reticle()
                && !plan.requires(HudTrigger.TARGET_ENTITY) && plan.requires(HudTrigger.INTERACTABLE_BLOCK));
        plan = build(only("health", HudTrigger.IN_COMBAT));
        verify("health shown in combat does not need health stats", plan.requires(HudTrigger.IN_COMBAT)
                && !plan.bars() && !plan.reticle() && !plan.heldItem());
        pipeline.update(state, null, context, new GlobalConfig(), 10000, plan);
        verify("combat scan still executes when requested", state.lastCombatScanMs == 10000);

        health.getHudComponents().setHideHealthHud(false);
        verify("visible components do not request detectors", !build(health).enabled());
        verify("hidden components without rules do not request detectors", !build(only("reticle")).enabled());
        PlayerConfig vanilla = new PlayerConfig();
        ProfilePresets.applyTo(vanilla, Profile.VANILLA);
        verify("Vanilla has no detectors", !build(vanilla).enabled());
        PlayerConfig immersive = new PlayerConfig();
        ProfilePresets.applyTo(immersive, Profile.IMMERSIVE);
        verify("Immersive retains combat but skips unused bars", build(immersive).requires(HudTrigger.IN_COMBAT)
                && !build(immersive).bars());

        PlayerConfig combined = only("health", HudTrigger.HEALTH_NOT_FULL);
        combined.getHudComponents().setHideCompassHud(true);
        combined.getDynamicHud().getCompass().setRules(EnumSet.of(HudTrigger.PLAYER_MOVING));
        verify("detector requests are combined across components", build(combined).bars() && build(combined).movement());

        state = new PlayerHudState();
        HudDetectionPlan cached = state.detectionPlan(combined.getHudComponents(), combined.getDynamicHud());
        verify("plan is reused between ticks", cached == state.detectionPlan(combined.getHudComponents(), combined.getDynamicHud()));
        state.t.pulse(HudTrigger.IN_COMBAT, 10000, 5000);
        state.applyHeldItemState(true, false, false, true);
        state.lastCombatScanMs = 10000;
        state.invalidateDetectionPlan();
        PlayerConfig construction = only("reticle", HudTrigger.HOLDING_BUILDING_BLOCK);
        plan = state.detectionPlan(construction.getHudComponents(), construction.getDynamicHud());
        verify("config changes rebuild detector requests", plan != cached && !plan.bars() && plan.heldItem());
        verify("re-enabling detectors cannot reuse stale signals", !state.t.active(HudTrigger.IN_COMBAT, 10000)
                && !state.heldItem.rangedWeaponInHand && !state.heldItem.buildingBlockInHand && state.lastCombatScanMs == 0);
        state.heldItem.buildingBlockInHand = true;
        state.t.pulse(HudTrigger.IN_COMBAT, 10000, 5000);
        var signals = new HudTriggerContextFactory().create(state, 10000, plan);
        verify("context exposes only requested signals", signals.active(HudTrigger.HOLDING_BUILDING_BLOCK)
                && !signals.active(HudTrigger.IN_COMBAT));
        construction.getDynamicHud().getReticle().setRules(EnumSet.noneOf(HudTrigger.class));
        verify("plan is an independent snapshot of configuration",
                plan.requires(HudTrigger.HOLDING_BUILDING_BLOCK) && !build(construction).enabled());
        state.reset(500);
        verify("player reset invalidates detector cache", state.dynamicHudCache.get() == null);
        System.out.println("Passed " + checks + " detector selection regression checks.");
    }

    private static PlayerConfig only(String componentKey, HudTrigger... triggers) {
        PlayerConfig config = new PlayerConfig();
        ProfilePresets.applyTo(config, Profile.VANILLA);
        config.getHudComponents().setByKey(componentKey, true);
        var component = HudComponentRegistry.find(componentKey);
        if (component == null) throw new AssertionError(componentKey);
        EnumSet<HudTrigger> rules = EnumSet.noneOf(HudTrigger.class);
        java.util.Collections.addAll(rules, triggers);
        component.requireDynamicRuleConfig(config.getDynamicHud()).setRules(rules);
        return config;
    }

    private static HudDetectionPlan build(PlayerConfig config) {
        return HudDetectionPlan.build(config.getHudComponents(), config.getDynamicHud());
    }

    private static void verify(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
