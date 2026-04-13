package com.tom.immersivehudplugin.commands.validation;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class AllowedValuesValidator implements Validator<String> {

    private final Set<String> allowed;
    private final String display;

    public AllowedValuesValidator(String... allowedValues) {
        this.allowed = Arrays.stream(allowedValues)
                .map(v -> v.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        this.display = String.join(", ", allowedValues);
    }

    @Override
    public void accept(@Nullable String input, @Nonnull ValidationResults results) {
        String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            results.fail("Allowed values: " + display);
        }
    }

    @Override
    public void updateSchema(SchemaContext context, @Nonnull Schema target) {
    }
}