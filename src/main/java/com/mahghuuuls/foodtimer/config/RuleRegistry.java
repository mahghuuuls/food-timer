package com.mahghuuuls.foodtimer.config;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-indexed registry storing active cooldown rules for O(1) item lookup.
 */
public final class RuleRegistry {

    private static final Map<ResourceLocation, List<CooldownRule>> RULES_BY_ITEM = new HashMap<>();

    private RuleRegistry() {
    }

    /**
     * Clears all registered rules.
     */
    public static synchronized void clear() {
        RULES_BY_ITEM.clear();
    }

    /**
     * Registers a new cooldown rule.
     *
     * @param rule The rule to register.
     */
    public static synchronized void register(CooldownRule rule) {
        if (rule == null) {
            return;
        }
        RULES_BY_ITEM.computeIfAbsent(rule.getRegistryName(), k -> new ArrayList<>()).add(rule);
    }

    /**
     * Finds a matching rule for the specified item and metadata.
     * Specific metadata matches take precedence over wildcard rules.
     *
     * @param registryName The item's resource location registry name.
     * @param metadata     The item's metadata value.
     * @return The matching CooldownRule, or null if no rule applies.
     */
    public static synchronized CooldownRule findRule(ResourceLocation registryName, int metadata) {
        if (registryName == null) {
            return null;
        }

        List<CooldownRule> rules = RULES_BY_ITEM.get(registryName);
        if (rules == null || rules.isEmpty()) {
            return null;
        }

        CooldownRule wildcardMatch = null;
        for (CooldownRule rule : rules) {
            if (rule.getMetadata() == metadata) {
                return rule; // Exact match takes precedence immediately
            } else if (rule.isWildcard()) {
                wildcardMatch = rule;
            }
        }

        return wildcardMatch;
    }

    /**
     * Convenience method to find a matching rule for a given ItemStack.
     *
     * @param stack The item stack.
     * @return The matching CooldownRule, or null if stack is empty/unmatched.
     */
    public static CooldownRule findRule(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == null) {
            return null;
        }
        ResourceLocation registryName = item.getRegistryName();
        if (registryName == null) {
            return null;
        }
        return findRule(registryName, stack.getMetadata());
    }

    /**
     * Checks if any rule exists for the specified item and metadata.
     */
    public static boolean hasRule(ResourceLocation registryName, int metadata) {
        return findRule(registryName, metadata) != null;
    }

    /**
     * Returns an unmodifiable view of all registered rules by item.
     */
    public static synchronized Map<ResourceLocation, List<CooldownRule>> getRegisteredRules() {
        Map<ResourceLocation, List<CooldownRule>> unmodifiable = new HashMap<>();
        for (Map.Entry<ResourceLocation, List<CooldownRule>> entry : RULES_BY_ITEM.entrySet()) {
            unmodifiable.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(unmodifiable);
    }
}
