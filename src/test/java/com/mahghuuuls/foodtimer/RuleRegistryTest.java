package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.RuleRegistry;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class RuleRegistryTest {

    @AfterEach
    public void clearFacade() {
        RuleRegistry.clear();
    }

    @Test
    public void compatibilityFacadeReadsInstalledImmutableOverrides() {
        ResourceLocation apple = new ResourceLocation("test:apple");
        RuleRegistry.install(new CooldownConfigSnapshot(
                CooldownPolicy.FIXED_ALL_FOODS,
                30,
                5,
                Arrays.asList(
                        new CooldownRule(apple, CooldownRule.WILDCARD_META, 10),
                        new CooldownRule(apple, 1, 50)
                )
        ));

        assertEquals(50, RuleRegistry.findRule(apple, 1).getDurationSeconds());
        assertEquals(10, RuleRegistry.findRule(apple, 0).getDurationSeconds());
        assertNull(RuleRegistry.findRule(new ResourceLocation("test:bread"), 0));
    }
}
