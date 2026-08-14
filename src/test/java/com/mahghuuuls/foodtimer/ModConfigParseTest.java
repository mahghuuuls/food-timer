package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.ModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModConfigParseTest {

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
        // Non-positive duration
        assertNull(ModConfig.parseRule("minecraft:golden_apple:0=0"));
        assertNull(ModConfig.parseRule("minecraft:golden_apple:0=-5"));
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
}
