package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.config.RuleRegistry;
import com.mahghuuuls.foodtimer.util.TimeFormatter;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FoodTimerIntegrationTest {

    @BeforeEach
    public void setup() {
        RuleRegistry.clear();
    }

    @Test
    public void testDefaultConfigurationAndTooltips() {
        // Load default rules
        for (String entry : ModConfig.foodCooldowns) {
            CooldownRule rule = ModConfig.parseRule(entry);
            if (rule != null) {
                RuleRegistry.register(rule);
            }
        }

        ResourceLocation gapple = new ResourceLocation("minecraft", "golden_apple");
        ResourceLocation bread = new ResourceLocation("minecraft", "bread");

        // Regular golden apple (meta 0)
        CooldownRule regularRule = RuleRegistry.findRule(gapple, 0);
        assertNotNull(regularRule);
        assertEquals(60, regularRule.getDurationSeconds());
        assertEquals(1200, regularRule.getDurationTicks());
        assertEquals("1m 0s", TimeFormatter.format(regularRule.getDurationSeconds()));

        // Enchanted golden apple (meta 1)
        CooldownRule enchantedRule = RuleRegistry.findRule(gapple, 1);
        assertNotNull(enchantedRule);
        assertEquals(300, enchantedRule.getDurationSeconds());
        assertEquals(6000, enchantedRule.getDurationTicks());
        assertEquals("5m 0s", TimeFormatter.format(enchantedRule.getDurationSeconds()));

        // Unconfigured food (bread)
        assertNull(RuleRegistry.findRule(bread, 0));
        assertFalse(RuleRegistry.hasRule(bread, 0));
    }

    @Test
    public void testCustomFoodAndModdedItemRules() {
        String[] customRules = new String[]{
                "minecraft:cooked_beef=30",
                "minecraft:golden_apple:0=45",
                "minecraft:golden_apple:1=600",
                "modid:custom_soup:2=120",
                "modid:wildcard_potion:*=15"
        };

        for (String entry : customRules) {
            CooldownRule rule = ModConfig.parseRule(entry);
            assertNotNull(rule);
            RuleRegistry.register(rule);
        }

        ResourceLocation beef = new ResourceLocation("minecraft", "cooked_beef");
        ResourceLocation gapple = new ResourceLocation("minecraft", "golden_apple");
        ResourceLocation soup = new ResourceLocation("modid", "custom_soup");
        ResourceLocation potion = new ResourceLocation("modid", "wildcard_potion");

        // Steak (implicit wildcard)
        CooldownRule beefRule = RuleRegistry.findRule(beef, 0);
        assertNotNull(beefRule);
        assertEquals(30, beefRule.getDurationSeconds());
        assertEquals("30s", TimeFormatter.format(beefRule.getDurationSeconds()));

        // Golden apple custom overrides
        CooldownRule gapple0 = RuleRegistry.findRule(gapple, 0);
        assertNotNull(gapple0);
        assertEquals(45, gapple0.getDurationSeconds());
        assertEquals("45s", TimeFormatter.format(gapple0.getDurationSeconds()));

        CooldownRule gapple1 = RuleRegistry.findRule(gapple, 1);
        assertNotNull(gapple1);
        assertEquals(600, gapple1.getDurationSeconds());
        assertEquals("10m 0s", TimeFormatter.format(gapple1.getDurationSeconds()));

        // Modded item with meta
        CooldownRule soupRule = RuleRegistry.findRule(soup, 2);
        assertNotNull(soupRule);
        assertEquals(120, soupRule.getDurationSeconds());
        assertEquals("2m 0s", TimeFormatter.format(soupRule.getDurationSeconds()));

        // Modded item with explicit wildcard
        CooldownRule potionRule = RuleRegistry.findRule(potion, 99);
        assertNotNull(potionRule);
        assertEquals(15, potionRule.getDurationSeconds());
        assertEquals("15s", TimeFormatter.format(potionRule.getDurationSeconds()));
    }
}
