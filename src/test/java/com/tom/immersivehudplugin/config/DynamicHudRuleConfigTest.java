package com.tom.immersivehudplugin.config;

import com.google.gson.JsonParser;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.profiles.ProfilePresets;
import com.tom.immersivehudplugin.ui.HudConfigPresenter;
import com.tom.immersivehudplugin.ui.HudConfigUiSession;

import java.util.EnumSet;

public final class DynamicHudRuleConfigTest {
    private static int checks;

    static void main() {
        var config = new DynamicHudRuleConfig();
        verify("empty rules", !config.hasRules() && !config.hasRule(HudTrigger.IN_COMBAT));
        var empty = config.getRulesView();
        verify("repeated reads reuse the snapshot", empty == config.getRulesView());
        verify("empty configs compare by content", config.hasSameRules(new DynamicHudRuleConfig()));

        var input = EnumSet.of(HudTrigger.IN_COMBAT, HudTrigger.HOTBAR_INPUT);
        config.setRules(input);
        verify("missing rule in a non-empty configuration", !config.hasRule(HudTrigger.PLAYER_MOVING));
        verify("different rule sets do not match", !config.hasSameRules(new DynamicHudRuleConfig()));
        input.clear();
        verify("setter does not retain caller-owned set", config.hasRule(HudTrigger.IN_COMBAT)
                && config.hasRule(HudTrigger.HOTBAR_INPUT));
        verify("published empty snapshot remains unchanged", empty.isEmpty());
        var snapshot = config.getRulesView();
        config.getRules().clear();
        verify("editable getter remains a defensive copy", config.hasRule(HudTrigger.IN_COMBAT));
        verify("queries do not replace snapshots", config.hasRules() && config.getRulesView() == snapshot);
        verify("duplicate addition is a no-op", !config.addRule(HudTrigger.IN_COMBAT)
                && config.getRulesView() == snapshot);
        verify("missing removal is a no-op", !config.removeRule(HudTrigger.PLAYER_MOVING)
                && config.getRulesView() == snapshot);
        config.addRule(null);
        config.removeRule(null);
        verify("null mutations preserve the current snapshot", config.getRulesView() == snapshot);

        verify("addition succeeds", config.addRule(HudTrigger.PLAYER_MOVING));
        verify("old snapshot is stable after addition", !snapshot.contains(HudTrigger.PLAYER_MOVING));
        verify("removal succeeds", config.removeRule(HudTrigger.IN_COMBAT));
        verify("old snapshot is stable after removal", snapshot.contains(HudTrigger.IN_COMBAT));
        verify("serialized names track edits", EnumSet.of(HudTrigger.HOTBAR_INPUT, HudTrigger.PLAYER_MOVING)
                .equals(config.getRules()));

        config.setThreshold(42f);
        var copy = config.copy();
        verify("copy retains content and threshold", copy.hasSameRules(config) && copy.getThreshold() == 42f);
        copy.removeRule(HudTrigger.HOTBAR_INPUT);
        config.addRule(HudTrigger.IN_COMBAT);
        verify("copy edits are independent", config.hasRule(HudTrigger.HOTBAR_INPUT)
                && !copy.hasRule(HudTrigger.IN_COMBAT));
        copy.setRules(config.getRulesView());
        verify("read-only snapshots can be imported", copy.hasSameRules(config));
        config.removeRule(HudTrigger.IN_COMBAT);
        verify("imported snapshot remains independent", copy.hasRule(HudTrigger.IN_COMBAT));

        var loaded = ConfigJsonMapper.fromJsonDynamicHudRuleConfig(JsonParser.parseString(
                "{\"Rules\":[\"in_combat\",\"HOTBAR_INPUT\",\"unknown\"],\"Threshold\":35}").getAsJsonObject());
        verify("JSON loading preserves normalized rules", loaded.hasRule(HudTrigger.IN_COMBAT)
                && loaded.hasRule(HudTrigger.HOTBAR_INPUT) && loaded.getRulesView().size() == 2);
        loaded.sanitize();
        verify("sanitization retains rules and threshold", loaded.hasRules() && loaded.getThreshold() == 35f);
        String[] names = loaded.getRuleNames();
        names[0] = "unknown";
        verify("serialized name arrays remain defensive", loaded.getRuleNames()[0].equals("HOTBAR_INPUT"));
        loaded.setRules(null);
        verify("null setter clears rules", !loaded.hasRules() && loaded.getRulesView().isEmpty());

        var player = new PlayerConfig();
        ProfilePresets.applyTo(player, Profile.BALANCED);
        var session = new HudConfigUiSession(player);
        var reticle = HudComponentRegistry.find("reticle");
        if (reticle == null) throw new AssertionError("Reticle missing");
        session.toggleRule(reticle, HudTrigger.HOLDING_BUILDING_BLOCK);
        verify("draft edits do not change saved config", player.getDynamicHud().getReticle()
                .hasRule(HudTrigger.HOLDING_BUILDING_BLOCK) && !session.isRuleEnabled(reticle, HudTrigger.HOLDING_BUILDING_BLOCK));
        var presenter = new HudConfigPresenter();
        verify("modified draft is Custom", presenter.resolveCurrentProfile(session.getDraftHudComponents(),
                session.getDraftDynamicHud()) == Profile.CUSTOM);
        session.toggleRule(reticle, HudTrigger.HOLDING_BUILDING_BLOCK);
        verify("reverted draft matches Balanced", presenter.resolveCurrentProfile(session.getDraftHudComponents(),
                session.getDraftDynamicHud()) == Profile.BALANCED);
        System.out.println("Passed " + checks + " rule query regression checks.");
    }

    private static void verify(String name, boolean condition) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
}
