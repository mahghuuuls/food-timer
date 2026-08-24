package com.mahghuuuls.foodtimer.config;

import com.mahghuuuls.foodtimer.FoodTimerMod;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads user-facing configuration and publishes one immutable gameplay snapshot.
 */
public final class ModConfig {

    public static final String CATEGORY_GENERAL = "general";
    public static final int DEFAULT_FIXED_FOOD_COOLDOWN_SECONDS = 30;
    public static final int DEFAULT_SECONDS_PER_HUNGER_POINT = 5;

    private static final String[] DEFAULT_FOOD_COOLDOWNS = new String[]{
            "minecraft:golden_apple:0=60",
            "minecraft:golden_apple:1=300"
    };

    public static Configuration config;

    public static CooldownPolicy cooldownPolicy = CooldownPolicy.CONFIGURED_ONLY;
    public static int fixedFoodCooldownSeconds = DEFAULT_FIXED_FOOD_COOLDOWN_SECONDS;
    public static int secondsPerHungerPoint = DEFAULT_SECONDS_PER_HUNGER_POINT;
    public static String[] foodCooldowns = DEFAULT_FOOD_COOLDOWNS.clone();
    public static boolean enableTooltips = true;
    public static String tooltipPrefix = "Cooldown: ";
    public static boolean enableDebugLogging = false;

    private static volatile CooldownConfigSnapshot gameplaySnapshot = defaultSnapshot();

    private ModConfig() {
    }

    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            syncConfig();
        }
    }

    public static CooldownConfigSnapshot getGameplaySnapshot() {
        return gameplaySnapshot;
    }

    /**
     * Loads a complete snapshot. Runtime configuration reload is intentionally unsupported.
     */
    public static void syncConfig() {
        if (config == null) {
            return;
        }

        try {
            config.load();

            Property policyProperty = config.get(
                    CATEGORY_GENERAL,
                    "cooldownPolicy",
                    CooldownPolicy.CONFIGURED_ONLY.name(),
                    "Cooldown policy: CONFIGURED_ONLY, FIXED_ALL_FOODS, or SCALED_ALL_FOODS.\n" +
                            "Changes require a server or integrated-server restart."
            );
            cooldownPolicy = parsePolicy(policyProperty.getString());

            String rawFixedValue = getExistingRawValue("fixedFoodCooldownSeconds");
            Property fixedProperty = config.get(
                    CATEGORY_GENERAL,
                    "fixedFoodCooldownSeconds",
                    DEFAULT_FIXED_FOOD_COOLDOWN_SECONDS,
                    "Cooldown in seconds for qualifying foods under FIXED_ALL_FOODS.\n" +
                            "Valid range: 1 to " + CooldownResolver.MAX_DURATION_SECONDS + ". Restart required."
            );
            fixedFoodCooldownSeconds = parseBoundedPositiveInt(
                    rawFixedValue == null ? fixedProperty.getString() : rawFixedValue,
                    "fixedFoodCooldownSeconds",
                    DEFAULT_FIXED_FOOD_COOLDOWN_SECONDS
            );

            String rawScaleValue = getExistingRawValue("secondsPerHungerPoint");
            Property scaleProperty = config.get(
                    CATEGORY_GENERAL,
                    "secondsPerHungerPoint",
                    DEFAULT_SECONDS_PER_HUNGER_POINT,
                    "Seconds per hunger point under SCALED_ALL_FOODS.\n" +
                            "Example: 5 gives a 20-second cooldown to a food restoring 4 hunger points. Restart required."
            );
            secondsPerHungerPoint = parseBoundedPositiveInt(
                    rawScaleValue == null ? scaleProperty.getString() : rawScaleValue,
                    "secondsPerHungerPoint",
                    DEFAULT_SECONDS_PER_HUNGER_POINT
            );

            Property foodCooldownProperty = config.get(
                    CATEGORY_GENERAL,
                    "foodCooldowns",
                    DEFAULT_FOOD_COOLDOWNS,
                    "Item cooldown overrides in the format 'modid:item[:metadata]=seconds'.\n" +
                            "Omit metadata or use '*' for every variant. Zero excludes a matching item from global policies.\n" +
                            "Example: 'minecraft:golden_apple:0=60'. Restart required."
            );
            foodCooldowns = foodCooldownProperty.getStringList();

            Property tooltipProperty = config.get(
                    CATEGORY_GENERAL,
                    "enableTooltips",
                    true,
                    "Whether to display cooldown duration info in item hover tooltips."
            );
            enableTooltips = tooltipProperty.getBoolean();

            Property prefixProperty = config.get(
                    CATEGORY_GENERAL,
                    "tooltipPrefix",
                    "Cooldown: ",
                    "Text prefix displayed before the cooldown duration in item tooltips."
            );
            tooltipPrefix = prefixProperty.getString();

            Property debugProperty = config.get(
                    CATEGORY_GENERAL,
                    "enableDebugLogging",
                    false,
                    "Enable bounded Food Timer policy and consumption diagnostics."
            );
            enableDebugLogging = debugProperty.getBoolean();

            List<CooldownRule> overrides = parseRules(foodCooldowns, true);
            CooldownConfigSnapshot loadedSnapshot = new CooldownConfigSnapshot(
                    cooldownPolicy,
                    fixedFoodCooldownSeconds,
                    secondsPerHungerPoint,
                    overrides
            );
            gameplaySnapshot = loadedSnapshot;
            RuleRegistry.install(loadedSnapshot);

            LoggerHelper.debug(
                    "Loaded policy {} with fixed={}s, scale={}s/hunger-point, and {} accepted overrides.",
                    cooldownPolicy,
                    fixedFoodCooldownSeconds,
                    secondsPerHungerPoint,
                    overrides.size()
            );
            for (CooldownRule rule : overrides) {
                LoggerHelper.debug("Accepted cooldown override: {}", rule);
            }
        } catch (Exception e) {
            FoodTimerMod.LOGGER.error("Failed to load Food Timer configuration file; retaining the previous snapshot.", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    public static CooldownPolicy parsePolicy(String rawPolicy) {
        if (rawPolicy != null) {
            try {
                return CooldownPolicy.valueOf(rawPolicy.trim());
            } catch (IllegalArgumentException ignored) {
                LoggerHelper.warn(
                        "Invalid cooldownPolicy '{}'; using {}.",
                        rawPolicy,
                        CooldownPolicy.CONFIGURED_ONLY
                );
                return CooldownPolicy.CONFIGURED_ONLY;
            }
        }
        return CooldownPolicy.CONFIGURED_ONLY;
    }

    /**
     * Parses, validates, and deduplicates override entries. The last valid duplicate wins.
     */
    public static List<CooldownRule> parseRules(String[] rawEntries, boolean requireRegisteredItems) {
        Map<String, CooldownRule> byScope = new LinkedHashMap<>();
        if (rawEntries == null) {
            return new ArrayList<>();
        }

        for (String rawEntry : rawEntries) {
            CooldownRule rule = parseRule(rawEntry);
            if (rule == null) {
                continue;
            }
            if (requireRegisteredItems && !Item.REGISTRY.containsKey(rule.getRegistryName())) {
                LoggerHelper.warn("Unknown item in cooldown override; skipping '{}'.", rawEntry == null ? "null" : rawEntry.trim());
                continue;
            }

            String scope = rule.getRegistryName().toString() + ':' + rule.getMetadata();
            if (byScope.containsKey(scope)) {
                LoggerHelper.warn("Duplicate cooldown override for '{}'; the last valid entry wins.", scope);
                byScope.remove(scope);
            }
            byScope.put(scope, rule);
        }
        return new ArrayList<>(byScope.values());
    }

    /**
     * Parses one override without consulting the item registry.
     */
    public static CooldownRule parseRule(String rawEntry) {
        if (rawEntry == null) {
            return null;
        }

        String trimmed = rawEntry.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }

        int equalIndex = trimmed.indexOf('=');
        if (equalIndex < 0) {
            LoggerHelper.warn("Malformed food cooldown override (missing '='): '{}'.", trimmed);
            return null;
        }

        String itemPart = trimmed.substring(0, equalIndex).trim();
        String durationPart = trimmed.substring(equalIndex + 1).trim();
        int durationSeconds;
        try {
            durationSeconds = Integer.parseInt(durationPart);
        } catch (NumberFormatException e) {
            LoggerHelper.warn("Malformed cooldown duration in override: '{}'.", trimmed);
            return null;
        }
        if (durationSeconds < 0 || durationSeconds > CooldownResolver.MAX_DURATION_SECONDS) {
            LoggerHelper.warn(
                    "Invalid cooldown duration in override (expected 0 to {} seconds): '{}'.",
                    CooldownResolver.MAX_DURATION_SECONDS,
                    trimmed
            );
            return null;
        }

        String[] itemTokens = itemPart.split(":", -1);
        if (itemTokens.length < 2 || itemTokens.length > 3) {
            LoggerHelper.warn("Malformed item identifier in override: '{}'.", trimmed);
            return null;
        }

        String modId = itemTokens[0].trim();
        String itemName = itemTokens[1].trim();
        if (modId.isEmpty() || itemName.isEmpty()) {
            LoggerHelper.warn("Malformed item identifier in override: '{}'.", trimmed);
            return null;
        }

        ResourceLocation registryName;
        try {
            registryName = new ResourceLocation(modId, itemName);
        } catch (RuntimeException e) {
            LoggerHelper.warn("Invalid item registry name in override: '{}'.", trimmed);
            return null;
        }

        int metadata = CooldownRule.WILDCARD_META;
        if (itemTokens.length == 3) {
            String metadataPart = itemTokens[2].trim();
            if (!"*".equals(metadataPart)) {
                try {
                    metadata = Integer.parseInt(metadataPart);
                } catch (NumberFormatException e) {
                    LoggerHelper.warn("Malformed metadata in override: '{}'.", trimmed);
                    return null;
                }
                if (metadata < 0) {
                    LoggerHelper.warn("Invalid negative metadata in override: '{}'.", trimmed);
                    return null;
                }
            }
        }

        return new CooldownRule(registryName, metadata, durationSeconds);
    }

    static int parseBoundedPositiveInt(String raw, String name, int defaultValue) {
        try {
            int value = Integer.parseInt(raw.trim());
            if (value > 0 && value <= CooldownResolver.MAX_DURATION_SECONDS) {
                return value;
            }
        } catch (RuntimeException ignored) {
        }
        LoggerHelper.warn(
                "Invalid {} '{}' (expected 1 to {}); using {}.",
                name,
                raw,
                CooldownResolver.MAX_DURATION_SECONDS,
                defaultValue
        );
        return defaultValue;
    }

    private static String getExistingRawValue(String propertyName) {
        if (!config.hasCategory(CATEGORY_GENERAL)) {
            return null;
        }
        net.minecraftforge.common.config.ConfigCategory category = config.getCategory(CATEGORY_GENERAL);
        Property existing = category.get(propertyName);
        return existing == null ? null : existing.getString();
    }

    private static CooldownConfigSnapshot defaultSnapshot() {
        return new CooldownConfigSnapshot(
                CooldownPolicy.CONFIGURED_ONLY,
                DEFAULT_FIXED_FOOD_COOLDOWN_SECONDS,
                DEFAULT_SECONDS_PER_HUNGER_POINT,
                parseRules(DEFAULT_FOOD_COOLDOWNS, false)
        );
    }
}
