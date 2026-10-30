package com.tom.immersivehudplugin.runtime;

import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;
import com.tom.immersivehudplugin.runtime.context.HudTriggerContextFactory;
import com.tom.immersivehudplugin.runtime.context.PlayerTickContext;
import com.tom.immersivehudplugin.runtime.signal.CombatSignalTracker;
import com.tom.immersivehudplugin.runtime.signal.ReticleSignalTracker;

import java.util.EnumSet;

public final class HudTimersTest {
    private static int checks;

    static void main() {
        long now = 10000;
        var timers = new HudTimers();
        verify("untriggered signal is inactive", !timers.active(HudTrigger.HOTBAR_INPUT, now));
        timers.pulse(HudTrigger.HOTBAR_INPUT, now, 0);
        verify("zero delay survives current evaluation", timers.active(HudTrigger.HOTBAR_INPUT, now));
        verify("zero delay expires when time advances", !timers.active(HudTrigger.HOTBAR_INPUT, now + 1));
        timers.pulse(HudTrigger.HOTBAR_INPUT, now + 250, 0);
        verify("new zero-delay pulse reactivates signal", timers.active(HudTrigger.HOTBAR_INPUT, now + 250));
        timers.clear(HudTrigger.HOTBAR_INPUT);
        verify("clear cancels a zero-delay pulse immediately", !timers.active(HudTrigger.HOTBAR_INPUT, now + 250));

        timers.pulse(HudTrigger.CONSUMABLE_USE, now, 500);
        verify("positive delay is active immediately", timers.active(HudTrigger.CONSUMABLE_USE, now));
        verify("positive delay retains original duration", timers.active(HudTrigger.CONSUMABLE_USE, now + 499));
        verify("positive delay expires at original boundary", !timers.active(HudTrigger.CONSUMABLE_USE, now + 500));
        timers.pulse(HudTrigger.CONSUMABLE_USE, now + 100, 500);
        verify("repeated pulses extend positive delay", timers.active(HudTrigger.CONSUMABLE_USE, now + 599));
        verify("extended pulse expires at boundary", !timers.active(HudTrigger.CONSUMABLE_USE, now + 600));
        timers.pulse(HudTrigger.CONSUMABLE_USE, now + 200, 0);
        verify("shorter pulse does not truncate existing timer", timers.active(HudTrigger.CONSUMABLE_USE, now + 599));
        timers.clearAll();
        verify("clearAll cancels timers", !timers.active(HudTrigger.CONSUMABLE_USE, now + 200));

        verifyZeroDelayContext("hotbar", HudTrigger.HOTBAR_INPUT);
        verifyZeroDelayContext("compass", HudTrigger.PLAYER_MOVING);
        verifyZeroDelayContext("reticle", HudTrigger.TARGET_ENTITY);
        verifyZeroDelayContext("reticle", HudTrigger.INTERACTABLE_BLOCK);
        verifyZeroDelayContext("reticle", HudTrigger.CONSUMABLE_USE);
        verifyZeroDelayContext("reticle", HudTrigger.CHARGING_WEAPON);
        verifyZeroDelayContext("reticle", HudTrigger.BLOCKING_ATTACK);
        verifyZeroDelayContext("health", HudTrigger.IN_COMBAT);

        var config = new PlayerConfig();
        ProfilePresets.applyTo(config, Profile.VANILLA);
        config.getHudComponents().setHideReticleHud(true);
        config.getDynamicHud().getReticle().setRules(EnumSet.of(HudTrigger.HOLDING_RANGED_WEAPON));
        var state = new PlayerHudState();
        state.applyHeldItemState(true, false, false, false);
        var plan = HudDetectionPlan.build(config.getHudComponents(), config.getDynamicHud());
        var factory = new HudTriggerContextFactory();
        verify("held-item states do not expire with zero hide delay", factory.create(state, now + 100000, plan)
                .active(HudTrigger.HOLDING_RANGED_WEAPON));
        verifyScanCadence(now);
        System.out.println("Passed " + checks + " HUD timer regression checks.");
    }

    private static void verifyScanCadence(long now) {
        var global = new GlobalConfig();
        var context = new PlayerTickContext(null, null, null, null, null, null);
        var state = new PlayerHudState();
        var combat = new CombatSignalTracker(null);
        state.lastCombatScanMs = now;
        combat.updateCombatSignalIfNeeded(state, context, global, now + 1, 500);
        verify("positive delay retains combat scan throttling", state.lastCombatScanMs == now);
        combat.updateCombatSignalIfNeeded(state, context, global, now + 1, 0);
        verify("zero delay checks combat every HUD update", state.lastCombatScanMs == now + 1);

        // With both targeting branches disabled this checks scheduling without querying a world.
        var config = new PlayerConfig();
        ProfilePresets.applyTo(config, Profile.VANILLA);
        var plan = HudDetectionPlan.build(config.getHudComponents(), config.getDynamicHud());
        var reticle = new ReticleSignalTracker();
        state.lastReticleScanMs = now;
        reticle.updateReticleSignalsIfNeeded(state, null, context, global, now + 1, 500, plan);
        verify("positive delay retains reticle scan throttling", state.lastReticleScanMs == now);
        reticle.updateReticleSignalsIfNeeded(state, null, context, global, now + 1, 0, plan);
        verify("zero delay checks reticle every HUD update", state.lastReticleScanMs == now + 1);
    }

    private static void verifyZeroDelayContext(String component, HudTrigger trigger) {
        var config = new PlayerConfig();
        ProfilePresets.applyTo(config, Profile.VANILLA);
        config.getHudComponents().setByKey(component, true);
        config.getDynamicHud().getByKey(component).setRules(EnumSet.of(trigger));
        var plan = HudDetectionPlan.build(config.getHudComponents(), config.getDynamicHud());
        var state = new PlayerHudState();
        state.hideDelayMs = 0;
        state.applyHeldItemState(true, false, true, false);
        var factory = new HudTriggerContextFactory();
        state.t.pulse(trigger, 10000, state.hideDelayMs);
        verify(trigger + " is exposed during current evaluation", factory.create(state, 10000, plan).active(trigger));
        verify(trigger + " is absent on next update without a pulse", !factory.create(state, 10250, plan).active(trigger));
        state.t.pulse(trigger, 10250, state.hideDelayMs);
        verify(trigger + " stays visible when condition is still active", factory.create(state, 10250, plan).active(trigger));
    }

    private static void verify(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
