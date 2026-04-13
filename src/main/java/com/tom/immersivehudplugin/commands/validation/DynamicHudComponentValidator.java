package com.tom.immersivehudplugin.commands.validation;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.tom.immersivehudplugin.hud.component.HudComponentRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DynamicHudComponentValidator implements Validator<String> {

    @Override
    public void accept(@Nullable String input, @Nonnull ValidationResults results) {
        String key = HudComponentRegistry.normalize(input);
        if (HudComponentRegistry.findDynamic(key) == null) {
            results.fail(
                    "Unknown dynamic HUD component: " + input
                            + ". Available components: "
                            + HudComponentRegistry.availableDynamicComponentsText()
            );
        }
    }

    @Override
    public void updateSchema(SchemaContext context, @Nonnull Schema target) {
    }
}