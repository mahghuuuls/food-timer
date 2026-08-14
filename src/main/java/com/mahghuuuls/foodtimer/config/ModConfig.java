package com.mahghuuuls.foodtimer.config;

import com.mahghuuuls.foodtimer.FoodTimerMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import java.io.File;

/**
 * Handles reading and parsing configuration from config/foodtimer.cfg.
 */
public final class ModConfig {

    public static final String CATEGORY_GENERAL = "general";

    public static Configuration config;

    // Config properties
    public static String[] foodCooldowns = new String[]{
            "minecraft:golden_apple:0=60",
            "minecraft:golden_apple:1=300"
    };
    public static boolean enableTooltips = true;
    public static String tooltipPrefix = "Cooldown: ";
    public static boolean enableDebugLogging = false;

    private ModConfig() {
    }

    /**
     * Initializes configuration from the provided file.
     *
     * @param configFile The configuration file location.
     */
    public static void init(File configFile) {
        if (config == null) {
            config = new Configuration(configFile);
            syncConfig();
        }
    }

    /**
     * Synchronizes and loads configuration properties into memory and populates RuleRegistry.
     */
    public static void syncConfig() {
        if (config == null) {
            return;
        }

        try {
            config.load();

            Property propFoodCooldowns = config.get(
                    CATEGORY_GENERAL,
                    "foodCooldowns",
                    new String[]{
                            "minecraft:golden_apple:0=60",
                            "minecraft:golden_apple:1=300"
                    },
                    "List of item cooldown rules in the format 'modid:item[:metadata]=seconds'.\n" +
                            "If metadata is omitted or '*', the cooldown applies to all variants of the item.\n" +
                            "Example: 'minecraft:golden_apple:0=60' (standard golden apple with 60s cooldown)\n" +
                            "Example: 'minecraft:golden_apple:1=300' (enchanted golden apple with 300s cooldown)\n" +
                            "Example: 'minecraft:cooked_beef=30' (steak with 30s cooldown)"
            );
            foodCooldowns = propFoodCooldowns.getStringList();

            Property propEnableTooltips = config.get(
                    CATEGORY_GENERAL,
                    "enableTooltips",
                    true,
                    "Whether to display cooldown duration info in item hover tooltips."
            );
            enableTooltips = propEnableTooltips.getBoolean();

            Property propTooltipPrefix = config.get(
                    CATEGORY_GENERAL,
                    "tooltipPrefix",
                    "Cooldown: ",
                    "Text prefix displayed before the cooldown duration in item tooltips."
            );
            tooltipPrefix = propTooltipPrefix.getString();

            Property propEnableDebugLogging = config.get(
                    CATEGORY_GENERAL,
                    "enableDebugLogging",
                    false,
                    "Enable verbose diagnostic debug logging in server/client console output."
            );
            enableDebugLogging = propEnableDebugLogging.getBoolean();

            // Populate RuleRegistry
            RuleRegistry.clear();
            int registeredCount = 0;
            for (String entry : foodCooldowns) {
                CooldownRule rule = parseRule(entry);
                if (rule != null) {
                    RuleRegistry.register(rule);
                    registeredCount++;
                }
            }

            if (enableDebugLogging) {
                FoodTimerMod.LOGGER.info("[FoodTimer-Debug] Loaded {} active food cooldown rules.", registeredCount);
            }

        } catch (Exception e) {
            FoodTimerMod.LOGGER.error("Failed to load Food Timer configuration file!", e);
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    /**
     * Parses a single rule string into a CooldownRule.
     * Accepted syntax:
     * - "modid:item=seconds"
     * - "modid:item:*=seconds"
     * - "modid:item:meta=seconds"
     *
     * @param rawEntry The unparsed rule string.
     * @return A CooldownRule, or null if the string is malformed.
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
        if (equalIndex == -1) {
            FoodTimerMod.LOGGER.warn("Malformed food cooldown rule (missing '=' delimiter): '{}'", trimmed);
            return null;
        }

        String itemPart = trimmed.substring(0, equalIndex).trim();
        String durationPart = trimmed.substring(equalIndex + 1).trim();

        int durationSeconds;
        try {
            durationSeconds = Integer.parseInt(durationPart);
            if (durationSeconds <= 0) {
                FoodTimerMod.LOGGER.warn("Invalid cooldown duration (must be > 0 seconds): '{}'", trimmed);
                return null;
            }
        } catch (NumberFormatException e) {
            FoodTimerMod.LOGGER.warn("Malformed cooldown duration number in rule: '{}'", trimmed);
            return null;
        }

        String[] itemTokens = itemPart.split(":");
        if (itemTokens.length < 2 || itemTokens.length > 3) {
            FoodTimerMod.LOGGER.warn("Malformed item identifier in rule (expected 'modid:item' or 'modid:item:meta'): '{}'", trimmed);
            return null;
        }

        String modId = itemTokens[0].trim();
        String itemName = itemTokens[1].trim();
        if (modId.isEmpty() || itemName.isEmpty()) {
            FoodTimerMod.LOGGER.warn("Malformed item identifier (modid or item name cannot be empty): '{}'", trimmed);
            return null;
        }

        ResourceLocation registryName = new ResourceLocation(modId, itemName);

        int metadata = CooldownRule.WILDCARD_META;
        if (itemTokens.length == 3) {
            String metaStr = itemTokens[2].trim();
            if (!metaStr.equals("*")) {
                try {
                    metadata = Integer.parseInt(metaStr);
                    if (metadata < 0) {
                        FoodTimerMod.LOGGER.warn("Invalid negative metadata in rule: '{}'", trimmed);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    FoodTimerMod.LOGGER.warn("Malformed metadata number in rule: '{}'", trimmed);
                    return null;
                }
            }
        }

        return new CooldownRule(registryName, metadata, durationSeconds);
    }
}
