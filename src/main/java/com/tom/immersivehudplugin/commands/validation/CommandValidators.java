package com.tom.immersivehudplugin.commands.validation;

import com.hypixel.hytale.codec.validation.Validator;
import com.tom.immersivehudplugin.hud.component.HudComponent;
import com.tom.immersivehudplugin.profiles.Profile;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;
import com.tom.immersivehudplugin.hud.trigger.HudTrigger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class CommandValidators {

    private static final Set<String> PROFILE_KEYS = Arrays.stream(Profile.values())
            .map(Profile::name)
            .map(CommandValidators::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private static final Set<String> COMPONENT_KEYS = HudComponentRegistry.allList().stream()
            .map(HudComponent::key)
            .map(CommandValidators::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private static final Set<String> DYNAMIC_COMPONENT_KEYS = HudComponentRegistry.dynamicList().stream()
            .map(HudComponent::key)
            .map(CommandValidators::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private static final Set<String> GROUP_KEYS = Arrays.stream(HudComponentRegistry.Group.values())
            .map(group -> group.key)
            .map(CommandValidators::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private static final Set<String> RULE_KEYS = Arrays.stream(HudTrigger.values())
            .map(HudTrigger::name)
            .map(CommandValidators::normalize)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private static final Set<String> COMPONENT_OR_GROUP_KEYS = union(COMPONENT_KEYS, GROUP_KEYS);

    private CommandValidators() {}

    public static Validator<String> profile() {
        return valueValidator(PROFILE_KEYS, "Unknown profile");
    }

    public static Validator<String> component() {
        return valueValidator(COMPONENT_KEYS, "Unknown HUD component");
    }

    public static Validator<String> dynamicComponent() {
        return valueValidator(DYNAMIC_COMPONENT_KEYS, "Unknown dynamic HUD component");
    }

    public static Validator<String> group() {
        return valueValidator(GROUP_KEYS, "Unknown HUD group");
    }

    public static Validator<String> componentOrGroup() {
        return valueValidator(COMPONENT_OR_GROUP_KEYS, "Unknown HUD component or group");
    }

    public static Validator<String> rule() {
        return valueValidator(RULE_KEYS, "Unknown rule");
    }

    private static Validator<String> valueValidator(Set<String> allowed, String prefix) {
        String allowedText = String.join(", ", allowed);
        return new PredicateValidator<>(
                value -> value != null && allowed.contains(normalize(value)),
                input -> prefix + ": " + input + ". Available values: " + allowedText
        );
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        LinkedHashSet<String> out = new LinkedHashSet<>(a);
        out.addAll(b);
        return out;
    }

    public static String normalize(@Nullable String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    public static Validator<String> validateArguments(Set<String> RULES_KEYS, String prefix) {
        return valueValidator(RULES_KEYS, prefix);
    }
}