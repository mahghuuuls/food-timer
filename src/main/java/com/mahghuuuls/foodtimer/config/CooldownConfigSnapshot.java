package com.mahghuuuls.foodtimer.config;

import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable validated gameplay settings used by cooldown resolution.
 */
public final class CooldownConfigSnapshot {

    private final CooldownPolicy policy;
    private final int fixedFoodCooldownSeconds;
    private final int secondsPerHungerPoint;
    private final Map<ResourceLocation, Map<Integer, CooldownRule>> overridesByItem;
    private final List<CooldownRule> overrides;

    public CooldownConfigSnapshot(CooldownPolicy policy, int fixedFoodCooldownSeconds,
                                  int secondsPerHungerPoint, List<CooldownRule> rules) {
        this.policy = Objects.requireNonNull(policy, "policy");
        validatePositiveSeconds("fixedFoodCooldownSeconds", fixedFoodCooldownSeconds);
        validatePositiveSeconds("secondsPerHungerPoint", secondsPerHungerPoint);
        this.fixedFoodCooldownSeconds = fixedFoodCooldownSeconds;
        this.secondsPerHungerPoint = secondsPerHungerPoint;

        Map<ResourceLocation, Map<Integer, CooldownRule>> mutableByItem = new HashMap<>();
        List<CooldownRule> canonicalRules = new ArrayList<>();
        Map<String, CooldownRule> scopes = new HashMap<>();
        if (rules != null) {
            for (CooldownRule rule : rules) {
                if (rule == null) {
                    continue;
                }
                String scopeKey = scopeKey(rule.getRegistryName(), rule.getMetadata());
                if (scopes.put(scopeKey, rule) != null) {
                    throw new IllegalArgumentException("Duplicate cooldown override scope: " + scopeKey);
                }
                canonicalRules.add(rule);
            }
        }

        for (CooldownRule rule : canonicalRules) {
            Map<Integer, CooldownRule> itemRules = mutableByItem.computeIfAbsent(
                    rule.getRegistryName(), ignored -> new HashMap<>()
            );
            itemRules.put(rule.getMetadata(), rule);
        }

        Map<ResourceLocation, Map<Integer, CooldownRule>> immutableByItem = new HashMap<>();
        for (Map.Entry<ResourceLocation, Map<Integer, CooldownRule>> entry : mutableByItem.entrySet()) {
            immutableByItem.put(entry.getKey(), Collections.unmodifiableMap(new HashMap<>(entry.getValue())));
        }
        this.overridesByItem = Collections.unmodifiableMap(immutableByItem);
        this.overrides = Collections.unmodifiableList(new ArrayList<>(canonicalRules));
    }

    public CooldownPolicy getPolicy() {
        return policy;
    }

    public int getFixedFoodCooldownSeconds() {
        return fixedFoodCooldownSeconds;
    }

    public int getSecondsPerHungerPoint() {
        return secondsPerHungerPoint;
    }

    public CooldownRule findExactOverride(ResourceLocation registryName, int metadata) {
        Map<Integer, CooldownRule> itemRules = overridesByItem.get(registryName);
        return itemRules == null ? null : itemRules.get(metadata);
    }

    public CooldownRule findWildcardOverride(ResourceLocation registryName) {
        Map<Integer, CooldownRule> itemRules = overridesByItem.get(registryName);
        return itemRules == null ? null : itemRules.get(CooldownRule.WILDCARD_META);
    }

    public CooldownRule findOverride(ResourceLocation registryName, int metadata) {
        CooldownRule exact = findExactOverride(registryName, metadata);
        return exact != null ? exact : findWildcardOverride(registryName);
    }

    public List<CooldownRule> getOverrides() {
        return overrides;
    }

    private static String scopeKey(ResourceLocation registryName, int metadata) {
        return registryName.toString() + ':' + metadata;
    }

    private static void validatePositiveSeconds(String name, int value) {
        if (value <= 0 || value > CooldownResolver.MAX_DURATION_SECONDS) {
            throw new IllegalArgumentException(name + " out of range: " + value);
        }
    }
}
