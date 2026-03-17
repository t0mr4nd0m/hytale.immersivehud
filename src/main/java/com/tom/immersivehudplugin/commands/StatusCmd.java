package com.tom.immersivehudplugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.tom.immersivehudplugin.ImmersiveHudPlugin;
import com.tom.immersivehudplugin.config.DynamicHudConfig;
import com.tom.immersivehudplugin.config.DynamicHudRuleConfig;
import com.tom.immersivehudplugin.config.GlobalConfig;
import com.tom.immersivehudplugin.config.HudComponentsConfig;
import com.tom.immersivehudplugin.config.PlayerConfig;
import com.tom.immersivehudplugin.registry.HudComponentRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.StringJoiner;
import java.util.Locale;

public final class StatusCmd extends AbstractPlayerCommand {

    private static final Color SECTION_COLOR = Color.YELLOW;
    private static final Color LABEL_COLOR = Color.LIGHT_GRAY;
    private static final Color VALUE_COLOR = Color.WHITE;
    private static final Color SHOW_COLOR = Color.GREEN;
    private static final Color HIDE_COLOR = Color.RED;
    private static final Color ERROR_COLOR = Color.RED;

    private final ImmersiveHudPlugin plugin;

    public StatusCmd(ImmersiveHudPlugin plugin) {
        super("status", "Show your ImmersiveHud settings");
        this.plugin = plugin;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        PlayerConfig playerCfg = requirePlayerConfig(plugin, playerRef, context);
        if (playerCfg == null) {
            return;
        }

        GlobalConfig global = plugin.getImmersiveHudGlobalConfig();
        HudComponentsConfig hud = playerCfg.getHudComponents();
        DynamicHudConfig dynamic = playerCfg.getDynamicHud();

        context.sendMessage(Message.raw("=== ImmersiveHud Status ===").color(SECTION_COLOR));

        sendGlobalSettings(context, global);
        sendGroupedHudComponents(context, hud, dynamic);
    }

    @Nullable
    private static PlayerConfig requirePlayerConfig(
            @Nonnull ImmersiveHudPlugin plugin,
            @Nonnull PlayerRef playerRef,
            @Nonnull CommandContext context
    ) {
        PlayerConfig playerConfig = plugin.requirePlayerConfig(playerRef);
        if (playerConfig == null) {
            context.sendMessage(Message.raw("Failed to load your ImmersiveHud profile.").color(ERROR_COLOR));
            return null;
        }
        return playerConfig;
    }

    private static void sendGlobalSettings(@Nonnull CommandContext context, @Nonnull GlobalConfig cfg) {
        sendSectionHeader(context, "Global");

        sendSettingLine(context, "configSchemaVersion", cfg.getConfigVersion(), VALUE_COLOR);
        sendSettingLine(context, "intervalMs", cfg.getIntervalMs(), VALUE_COLOR);
        sendSettingLine(context, "hideDelayMs", cfg.getHideDelayMs(), VALUE_COLOR);
        sendSettingLine(context, "reticleTargetRange", cfg.getReticleTargetRange(), VALUE_COLOR);
    }

    private static void sendGroupedHudComponents(
            @Nonnull CommandContext context,
            @Nonnull HudComponentsConfig hud,
            @Nonnull DynamicHudConfig dynamic
    ) {
        sendSectionHeader(context, "HudComponents");

        for (HudComponentRegistry.Group group : HudComponentRegistry.Group.values()) {
            boolean wroteGroupHeader = false;

            for (var entry : HudComponentRegistry.allList()) {
                if (entry.group() != group) {
                    continue;
                }

                if (!wroteGroupHeader) {
                    sendSectionHeader(context, group.label);
                    wroteGroupHeader = true;
                }

                boolean hiddenNow = entry.staticGetter().get(hud);

                if (entry.supportsDynamicRules()) {
                    @SuppressWarnings("DataFlowIssue") DynamicHudRuleConfig rule = entry.dynamicGetter().apply(dynamic);
                    sendDynamicComponentLine(context, entry.key(), hiddenNow, rule);
                } else {
                    sendStaticComponentLine(context, entry.key(), hiddenNow);
                }
            }
        }
    }

    private static void sendStaticComponentLine(
            @Nonnull CommandContext context,
            @Nonnull String key,
            boolean hidden
    ) {
        context.sendMessage(Message.join(
                Message.raw(" • ").color(LABEL_COLOR),
                Message.raw(key).color(VALUE_COLOR),
                Message.raw(": ").color(LABEL_COLOR),
                Message.raw(hidden ? "hide" : "show").color(hidden ? HIDE_COLOR : SHOW_COLOR)
        ));
    }

    private static void sendDynamicComponentLine(
            @Nonnull CommandContext context,
            @Nonnull String key,
            boolean hidden,
            @Nullable DynamicHudRuleConfig rule
    ) {

        context.sendMessage(Message.join(
                Message.raw(" • ").color(LABEL_COLOR),
                Message.raw(key).color(VALUE_COLOR),
                Message.raw(": ").color(LABEL_COLOR),
                Message.raw(hidden ? "hide" : "show").color(hidden ? HIDE_COLOR : SHOW_COLOR),
                Message.raw(" | ").color(LABEL_COLOR),
                Message.raw("rules=").color(LABEL_COLOR),
                Message.raw(renderRules(rule)).color(VALUE_COLOR)
        ));
    }

    private static String renderRules(@Nullable DynamicHudRuleConfig rule) {
        if (rule == null) {
            return "<null>";
        }

        var triggers = rule.getRules();
        if (triggers.isEmpty()) {
            return "[]";
        }

        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (var trigger : triggers) {
            joiner.add(trigger.name().toLowerCase(Locale.ROOT));
        }
        return joiner.toString();
    }

    private static void sendSectionHeader(@Nonnull CommandContext context, @Nonnull String label) {
        context.sendMessage(Message.raw("[" + label + "]").color(SECTION_COLOR));
    }

    private static void sendSettingLine(
            @Nonnull CommandContext context,
            @Nonnull String label,
            @Nonnull Object value,
            @SuppressWarnings("SameParameterValue") @Nonnull Color valueColor
    ) {
        context.sendMessage(Message.join(
                Message.raw(" • ").color(LABEL_COLOR),
                Message.raw(label).color(LABEL_COLOR),
                Message.raw(": ").color(LABEL_COLOR),
                Message.raw(String.valueOf(value)).color(valueColor)
        ));
    }
}