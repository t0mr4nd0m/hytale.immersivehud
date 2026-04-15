package com.tom.immersivehudplugin.commands.validation;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public final class PredicateValidator<T> implements Validator<T> {

    private final Predicate<T> predicate;
    private final Function<T, String> errorMessageFactory;

    public PredicateValidator(
            Predicate<T> predicate,
            Function<T, String> errorMessageFactory
    ) {
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.errorMessageFactory = Objects.requireNonNull(errorMessageFactory, "errorMessageFactory");
    }

    @Override
    public void accept(@Nullable T input, @Nonnull ValidationResults results) {
        if (!predicate.test(input)) {
            results.fail(errorMessageFactory.apply(input));
        }
    }

    @Override
    public void updateSchema(SchemaContext context, @Nonnull Schema target) {
        // no-op
    }
}