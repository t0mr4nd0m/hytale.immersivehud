package com.tom.immersivehudplugin.config;

import com.tom.immersivehudplugin.registry.HudComponentRegistry;
import com.tom.immersivehudplugin.registry.HudComponentRegistry.HudEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class HudComponentsConfig {

    private final Map<String, Boolean> hiddenByKey = new LinkedHashMap<>();

    public HudComponentsConfig() {
        ensureAllEntries();
    }

    public boolean isHideHotbarHud() { return getByKey("hotbar"); }
    public void setHideHotbarHud(boolean v) { setByKey("hotbar", v); }

    public boolean isHideCompassHud() { return getByKey("compass"); }
    public void setHideCompassHud(boolean v) { setByKey("compass", v); }

    public boolean isHideReticleHud() { return getByKey("reticle"); }
    public void setHideReticleHud(boolean v) { setByKey("reticle", v); }

    public boolean isHideStaminaHud() { return getByKey("stamina"); }
    public void setHideStaminaHud(boolean v) { setByKey("stamina", v); }

    public boolean isHideHealthHud() { return getByKey("health"); }
    public void setHideHealthHud(boolean v) { setByKey("health", v); }

    public boolean isHideManaHud() { return getByKey("mana"); }
    public void setHideManaHud(boolean v) { setByKey("mana", v); }

    public boolean isHideOxygenHud() { return getByKey("oxygen"); }
    public void setHideOxygenHud(boolean v) { setByKey("oxygen", v); }

    public boolean isHideInputBindingsHud() { return getByKey("inputbindings"); }
    public void setHideInputBindingsHud(boolean v) { setByKey("inputbindings", v); }

    public boolean isHideNotificationsHud() { return getByKey("notifications"); }
    public void setHideNotificationsHud(boolean v) { setByKey("notifications", v); }

    public boolean isHideSpeedometerHud() { return getByKey("speedometer"); }
    public void setHideSpeedometerHud(boolean v) { setByKey("speedometer", v); }

    public boolean isHideStatusIconsHud() { return getByKey("statusicons"); }
    public void setHideStatusIconsHud(boolean v) { setByKey("statusicons", v); }

    public boolean isHideChatHud() { return getByKey("chat"); }
    public void setHideChatHud(boolean v) { setByKey("chat", v); }

    public boolean isHideRequestsHud() { return getByKey("requests"); }
    public void setHideRequestsHud(boolean v) { setByKey("requests", v); }

    public boolean isHideKillFeedHud() { return getByKey("killfeed"); }
    public void setHideKillFeedHud(boolean v) { setByKey("killfeed", v); }

    public boolean isHidePlayerListHud() { return getByKey("playerlist"); }
    public void setHidePlayerListHud(boolean v) { setByKey("playerlist", v); }

    public boolean isHideEventTitleHud() { return getByKey("eventtitle"); }
    public void setHideEventTitleHud(boolean v) { setByKey("eventtitle", v); }

    public boolean isHideObjectivePanelHud() { return getByKey("objectivepanel"); }
    public void setHideObjectivePanelHud(boolean v) { setByKey("objectivepanel", v); }

    public boolean isHidePortalPanelHud() { return getByKey("portalpanel"); }
    public void setHidePortalPanelHud(boolean v) { setByKey("portalpanel", v); }

    public boolean isHideBuilderToolsLegendHud() { return getByKey("buildertoolslegend"); }
    public void setHideBuilderToolsLegendHud(boolean v) { setByKey("buildertoolslegend", v); }

    public boolean isHideUtilitySlotSelectorHud() { return getByKey("utilityslotselector"); }
    public void setHideUtilitySlotSelectorHud(boolean v) { setByKey("utilityslotselector", v); }

    public boolean isHideBlockVariantSelectorHud() { return getByKey("blockvariantselector"); }
    public void setHideBlockVariantSelectorHud(boolean v) { setByKey("blockvariantselector", v); }

    public boolean isHideBuilderToolsMaterialSlotSelectorHud() { return getByKey("buildertoolsmaterialslotselector"); }
    public void setHideBuilderToolsMaterialSlotSelectorHud(boolean v) { setByKey("buildertoolsmaterialslotselector", v); }

    public boolean isHideAmmoIndicatorHud() { return getByKey("ammo"); }
    public void setHideAmmoIndicatorHud(boolean v) { setByKey("ammo", v); }

    public boolean isHideSleepHud() { return getByKey("sleep"); }
    public void setHideSleepHud(boolean v) { setByKey("sleep", v); }

    public boolean getByKey(@Nullable String key) {
        String normalized = HudComponentRegistry.normalize(key);
        ensureAllEntries();
        return hiddenByKey.computeIfAbsent(normalized, this::defaultHiddenFor);
    }

    public void setByKey(@Nullable String key, boolean hidden) {
        String normalized = HudComponentRegistry.normalize(key);
        if (normalized.isEmpty()) {
            return;
        }

        hiddenByKey.put(normalized, hidden);
    }

    @Nonnull
    public Map<String, Boolean> asMap() {
        ensureAllEntries();
        return Collections.unmodifiableMap(hiddenByKey);
    }

    public boolean sanitize() {
        boolean changed = false;

        for (HudEntry entry : HudComponentRegistry.allList()) {
            String key = HudComponentRegistry.normalize(entry.key());
            Boolean value = hiddenByKey.get(key);

            if (value == null) {
                hiddenByKey.put(key, entry.defaultHidden());
                changed = true;
            }
        }

        Set<String> validKeys = HudComponentRegistry.allList().stream()
                .map(HudEntry::key)
                .map(HudComponentRegistry::normalize)
                .collect(Collectors.toSet());

        Iterator<Map.Entry<String, Boolean>> it = hiddenByKey.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Boolean> entry = it.next();
            if (!validKeys.contains(entry.getKey())) {
                it.remove();
                changed = true;
            }
        }

        return changed;
    }

    public HudComponentsConfig copy() {
        HudComponentsConfig c = new HudComponentsConfig();
        c.hiddenByKey.clear();
        c.hiddenByKey.putAll(this.hiddenByKey);
        c.ensureAllEntries();
        return c;
    }

    private void ensureAllEntries() {
        for (HudEntry entry : HudComponentRegistry.allList()) {
            String key = HudComponentRegistry.normalize(entry.key());
            hiddenByKey.computeIfAbsent(key, k -> entry.defaultHidden());
        }
    }

    private boolean defaultHiddenFor(String normalizedKey) {
        HudEntry entry = HudComponentRegistry.find(normalizedKey);
        return entry != null && entry.defaultHidden();
    }
}