package com.mahghuuuls.foodtimer.config;

import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compatibility facade for explicit override lookup while client tooltip synchronization is introduced.
 * Gameplay policy resolution belongs to the immutable snapshot and CooldownResolver.
 */
public final class RuleRegistry {

    private static volatile CooldownConfigSnapshot snapshot = emptySnapshot();

    private RuleRegistry() {
    }

    public static void install(CooldownConfigSnapshot newSnapshot) {
        snapshot = newSnapshot == null ? emptySnapshot() : newSnapshot;
    }

    public static void clear() {
        snapshot = emptySnapshot();
    }

    public static CooldownRule findRule(ResourceLocation registryName, int metadata) {
        return registryName == null ? null : snapshot.findOverride(registryName, metadata);
    }

    public static CooldownRule findRule(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        ResourceLocation registryName = item == null ? null : item.getRegistryName();
        return registryName == null ? null : findRule(registryName, stack.getMetadata());
    }

    public static boolean hasRule(ResourceLocation registryName, int metadata) {
        return findRule(registryName, metadata) != null;
    }

    public static Map<ResourceLocation, List<CooldownRule>> getRegisteredRules() {
        Map<ResourceLocation, List<CooldownRule>> result = new LinkedHashMap<>();
        for (CooldownRule rule : snapshot.getOverrides()) {
            result.computeIfAbsent(rule.getRegistryName(), ignored -> new ArrayList<>()).add(rule);
        }
        Map<ResourceLocation, List<CooldownRule>> immutable = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<CooldownRule>> entry : result.entrySet()) {
            immutable.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(immutable);
    }

    private static CooldownConfigSnapshot emptySnapshot() {
        return new CooldownConfigSnapshot(CooldownPolicy.CONFIGURED_ONLY, 30, 5, Collections.emptyList());
    }
}
