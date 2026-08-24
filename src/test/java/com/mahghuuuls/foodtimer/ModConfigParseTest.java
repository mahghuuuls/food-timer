package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModConfigParseTest {

    @BeforeAll
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void testParseValidExactMetaRule() {
        CooldownRule rule = ModConfig.parseRule("minecraft:golden_apple:0=60");
        assertNotNull(rule);
        assertEquals("minecraft:golden_apple", rule.getRegistryName().toString());
        assertEquals(0, rule.getMetadata());
        assertEquals(60, rule.getDurationSeconds());
        assertEquals(1200, rule.getDurationTicks());
        assertFalse(rule.isWildcard());
    }

    @Test
    public void testParseValidWildcardRuleImplicit() {
        CooldownRule rule = ModConfig.parseRule("minecraft:cooked_beef=30");
        assertNotNull(rule);
        assertEquals("minecraft:cooked_beef", rule.getRegistryName().toString());
        assertEquals(CooldownRule.WILDCARD_META, rule.getMetadata());
        assertEquals(30, rule.getDurationSeconds());
        assertTrue(rule.isWildcard());
    }

    @Test
    public void testParseValidWildcardRuleExplicitAsterisk() {
        CooldownRule rule = ModConfig.parseRule("minecraft:apple:*=15");
        assertNotNull(rule);
        assertEquals("minecraft:apple", rule.getRegistryName().toString());
        assertEquals(CooldownRule.WILDCARD_META, rule.getMetadata());
        assertEquals(15, rule.getDurationSeconds());
        assertTrue(rule.isWildcard());
    }

    @Test
    public void testParseWhitespaceHandling() {
        CooldownRule rule = ModConfig.parseRule("  minecraft:golden_apple:1 = 300  ");
        assertNotNull(rule);
        assertEquals("minecraft:golden_apple", rule.getRegistryName().toString());
        assertEquals(1, rule.getMetadata());
        assertEquals(300, rule.getDurationSeconds());
    }

    @Test
    public void testParseMalformedEntriesGracefulHandling() {
        // Missing equals delimiter
        assertNull(ModConfig.parseRule("minecraft:golden_apple:0"));
        // Invalid duration string
        assertNull(ModConfig.parseRule("minecraft:golden_apple:0=abc"));
        // Negative or oversized duration
        assertNull(ModConfig.parseRule("minecraft:golden_apple:0=-5"));
        assertNull(ModConfig.parseRule("minecraft:golden_apple:0=" + ((long) CooldownResolver.MAX_DURATION_SECONDS + 1L)));
        // Negative metadata
        assertNull(ModConfig.parseRule("minecraft:golden_apple:-1=60"));
        // Malformed metadata string
        assertNull(ModConfig.parseRule("minecraft:golden_apple:xyz=60"));
        // Empty or comment
        assertNull(ModConfig.parseRule(""));
        assertNull(ModConfig.parseRule("   "));
        assertNull(ModConfig.parseRule("# Comment line"));
        assertNull(ModConfig.parseRule(null));
    }

    @Test
    public void testZeroDurationIsAnExclusion() {
        CooldownRule rule = ModConfig.parseRule("minecraft:apple=0");

        assertNotNull(rule);
        assertEquals(0, rule.getDurationSeconds());
        assertTrue(rule.isWildcard());
    }

    @Test
    public void testLastValidDuplicateWins() {
        List<CooldownRule> rules = ModConfig.parseRules(new String[]{
                "minecraft:apple=10",
                "minecraft:apple=bad",
                "minecraft:apple=20"
        }, false);

        assertEquals(1, rules.size());
        assertEquals(20, rules.get(0).getDurationSeconds());
    }

    @Test
    public void testRegistryFilteringKeepsRegisteredItemAndSkipsMissingItem() {
        ResourceLocation registeredName = new ResourceLocation("minecraft:bread");
        List<CooldownRule> rules = ModConfig.parseRules(new String[]{
                "minecraft:bread=12",
                "foodtimer_test:missing_food=14"
        }, true);

        assertEquals(1, rules.size());
        assertEquals(registeredName, rules.get(0).getRegistryName());
        assertEquals(12, rules.get(0).getDurationSeconds());
    }

    @Test
    public void testPolicyNamesAreExactAndBackwardCompatible() {
        assertEquals(CooldownPolicy.CONFIGURED_ONLY, ModConfig.parsePolicy(null));
        assertEquals(CooldownPolicy.FIXED_ALL_FOODS, ModConfig.parsePolicy("FIXED_ALL_FOODS"));
        assertEquals(CooldownPolicy.SCALED_ALL_FOODS, ModConfig.parsePolicy("SCALED_ALL_FOODS"));
        assertEquals(CooldownPolicy.CONFIGURED_ONLY, ModConfig.parsePolicy("fixed_all_foods"));
        assertEquals(CooldownPolicy.CONFIGURED_ONLY, ModConfig.parsePolicy("UNKNOWN"));
    }
}
