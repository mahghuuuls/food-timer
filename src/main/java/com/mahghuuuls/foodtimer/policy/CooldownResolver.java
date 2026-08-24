package com.mahghuuuls.foodtimer.policy;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sole owner of effective cooldown precedence, policy formulae, and persistence scope.
 */
public final class CooldownResolver {

    public static final int TICKS_PER_SECOND = 20;
    public static final int MAX_DURATION_SECONDS = Integer.MAX_VALUE / TICKS_PER_SECOND;
    private static final int MAX_CAPPED_WARNING_KEYS = 256;

    private static final Set<String> CAPPED_WARNING_KEYS = Collections.newSetFromMap(
            new ConcurrentHashMap<String, Boolean>()
    );

    private CooldownResolver() {
    }

    public static CooldownDecision resolve(CooldownConfigSnapshot snapshot, ItemStack stack) {
        if (snapshot == null || stack == null || stack.isEmpty()) {
            return CooldownDecision.none();
        }

        Item item = stack.getItem();
        ResourceLocation registryName = item == null ? null : item.getRegistryName();
        if (registryName == null) {
            return CooldownDecision.none();
        }

        int metadata = stack.getMetadata();
        CooldownRule exact = snapshot.findExactOverride(registryName, metadata);
        if (exact != null) {
            return fromOverride(exact, CooldownDecision.Source.EXACT_OVERRIDE);
        }

        CooldownRule wildcard = snapshot.findWildcardOverride(registryName);
        if (wildcard != null) {
            return fromOverride(wildcard, CooldownDecision.Source.WILDCARD_OVERRIDE);
        }

        if (!(item instanceof ItemFood)) {
            return CooldownDecision.none();
        }

        if (snapshot.getPolicy() == CooldownPolicy.FIXED_ALL_FOODS) {
            return CooldownDecision.cooldown(
                    CooldownDecision.Source.FIXED_GLOBAL,
                    registryName,
                    metadata,
                    snapshot.getFixedFoodCooldownSeconds()
            );
        }

        if (snapshot.getPolicy() == CooldownPolicy.SCALED_ALL_FOODS) {
            int hungerPoints = ((ItemFood) item).getHealAmount(stack);
            if (hungerPoints <= 0) {
                return CooldownDecision.none();
            }
            long calculated = (long) hungerPoints * (long) snapshot.getSecondsPerHungerPoint();
            int seconds;
            if (calculated > MAX_DURATION_SECONDS) {
                seconds = MAX_DURATION_SECONDS;
                warnCappedOnce(registryName, metadata, calculated);
            } else {
                seconds = (int) calculated;
            }
            return CooldownDecision.cooldown(
                    CooldownDecision.Source.SCALED_GLOBAL,
                    registryName,
                    metadata,
                    seconds
            );
        }

        return CooldownDecision.none();
    }

    private static CooldownDecision fromOverride(CooldownRule rule, CooldownDecision.Source source) {
        if (rule.getDurationSeconds() == 0) {
            return CooldownDecision.excluded(source, rule.getRegistryName(), rule.getMetadata());
        }
        return CooldownDecision.cooldown(
                source,
                rule.getRegistryName(),
                rule.getMetadata(),
                rule.getDurationSeconds()
        );
    }

    private static void warnCappedOnce(ResourceLocation registryName, int metadata, long calculated) {
        String key = registryName.toString() + ':' + metadata;
        synchronized (CAPPED_WARNING_KEYS) {
            if (CAPPED_WARNING_KEYS.contains(key) || CAPPED_WARNING_KEYS.size() >= MAX_CAPPED_WARNING_KEYS) {
                return;
            }
            CAPPED_WARNING_KEYS.add(key);
        }
        LoggerHelper.warn(
                "Scaled cooldown for '{}' meta {} calculated {} seconds and was capped at {} seconds.",
                registryName,
                metadata,
                calculated,
                MAX_DURATION_SECONDS
        );
    }
}
