package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.RuleRegistry;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RuleRegistryTest {

    private final ResourceLocation goldenApple = new ResourceLocation("minecraft", "golden_apple");
    private final ResourceLocation apple = new ResourceLocation("minecraft", "apple");
    private final ResourceLocation bread = new ResourceLocation("minecraft", "bread");

    @BeforeEach
    public void setup() {
        RuleRegistry.clear();
    }

    @Test
    public void testExactMetadataMatching() {
        CooldownRule regularGapple = new CooldownRule(goldenApple, 0, 60);
        CooldownRule enchantedGapple = new CooldownRule(goldenApple, 1, 300);

        RuleRegistry.register(regularGapple);
        RuleRegistry.register(enchantedGapple);

        CooldownRule match0 = RuleRegistry.findRule(goldenApple, 0);
        assertNotNull(match0);
        assertEquals(60, match0.getDurationSeconds());
        assertEquals(1200, match0.getDurationTicks());

        CooldownRule match1 = RuleRegistry.findRule(goldenApple, 1);
        assertNotNull(match1);
        assertEquals(300, match1.getDurationSeconds());
        assertEquals(6000, match1.getDurationTicks());

        // Metadata 2 has no rule
        assertNull(RuleRegistry.findRule(goldenApple, 2));
    }

    @Test
    public void testWildcardMatching() {
        CooldownRule wildcardApple = new CooldownRule(apple, CooldownRule.WILDCARD_META, 15);
        RuleRegistry.register(wildcardApple);

        CooldownRule match0 = RuleRegistry.findRule(apple, 0);
        assertNotNull(match0);
        assertEquals(15, match0.getDurationSeconds());

        CooldownRule match99 = RuleRegistry.findRule(apple, 99);
        assertNotNull(match99);
        assertEquals(15, match99.getDurationSeconds());
    }

    @Test
    public void testExactMetadataPrecedenceOverWildcard() {
        CooldownRule wildcardApple = new CooldownRule(apple, CooldownRule.WILDCARD_META, 10);
        CooldownRule specialApple = new CooldownRule(apple, 5, 50);

        RuleRegistry.register(wildcardApple);
        RuleRegistry.register(specialApple);

        // Exact match on meta 5 should get 50s
        CooldownRule match5 = RuleRegistry.findRule(apple, 5);
        assertNotNull(match5);
        assertEquals(50, match5.getDurationSeconds());

        // Other metadata should fall back to wildcard 10s
        CooldownRule match0 = RuleRegistry.findRule(apple, 0);
        assertNotNull(match0);
        assertEquals(10, match0.getDurationSeconds());
    }

    @Test
    public void testUnregisteredItem() {
        assertNull(RuleRegistry.findRule(bread, 0));
        assertFalse(RuleRegistry.hasRule(bread, 0));
    }
}
