package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.policy.CooldownDecision;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CooldownResolverTest {

    @BeforeAll
    public static void bootstrapVanillaRegistries() {
        Bootstrap.register();
    }

    @Test
    public void configuredOnlyUsesOverridesAndLeavesOtherFoodsAlone() {
        ItemFood apple = food("test:apple", 4);
        CooldownConfigSnapshot snapshot = snapshot(
                CooldownPolicy.CONFIGURED_ONLY,
                new CooldownRule(new ResourceLocation("test:apple"), 0, 60)
        );

        CooldownDecision configured = CooldownResolver.resolve(snapshot, new ItemStack(apple, 1, 0));
        CooldownDecision otherMeta = CooldownResolver.resolve(snapshot, new ItemStack(apple, 1, 1));

        assertTrue(configured.hasCooldown());
        assertEquals(60, configured.getDurationSeconds());
        assertEquals(CooldownDecision.Source.EXACT_OVERRIDE, configured.getSource());
        assertFalse(otherMeta.hasCooldown());
        assertEquals(CooldownDecision.Outcome.NONE, otherMeta.getOutcome());
    }

    @Test
    public void shippedGoldenAppleDefaultsResolveToSixtyAndThreeHundredSeconds() {
        CooldownConfigSnapshot shipped = new CooldownConfigSnapshot(
                CooldownPolicy.CONFIGURED_ONLY,
                ModConfig.DEFAULT_FIXED_FOOD_COOLDOWN_SECONDS,
                ModConfig.DEFAULT_SECONDS_PER_HUNGER_POINT,
                ModConfig.parseRules(ModConfig.foodCooldowns, false)
        );

        CooldownDecision normal = CooldownResolver.resolve(shipped, new ItemStack(Items.GOLDEN_APPLE, 1, 0));
        CooldownDecision enchanted = CooldownResolver.resolve(shipped, new ItemStack(Items.GOLDEN_APPLE, 1, 1));
        CooldownDecision unlisted = CooldownResolver.resolve(shipped, new ItemStack(Items.BREAD));

        assertEquals(60, normal.getDurationSeconds());
        assertEquals(300, enchanted.getDurationSeconds());
        assertFalse(unlisted.hasCooldown());
    }

    @Test
    public void exactThenWildcardThenFixedFallbackPrecedenceIsStable() {
        ItemFood apple = food("test:apple", 4);
        CooldownConfigSnapshot snapshot = new CooldownConfigSnapshot(
                CooldownPolicy.FIXED_ALL_FOODS,
                30,
                5,
                Arrays.asList(
                        new CooldownRule(new ResourceLocation("test:apple"), CooldownRule.WILDCARD_META, 15),
                        new CooldownRule(new ResourceLocation("test:apple"), 1, 45)
                )
        );

        CooldownDecision exact = CooldownResolver.resolve(snapshot, new ItemStack(apple, 1, 1));
        CooldownDecision wildcard = CooldownResolver.resolve(snapshot, new ItemStack(apple, 1, 0));
        ItemFood bread = food("test:bread", 5);
        CooldownDecision fallback = CooldownResolver.resolve(snapshot, new ItemStack(bread));

        assertEquals(45, exact.getDurationSeconds());
        assertEquals(CooldownDecision.Source.EXACT_OVERRIDE, exact.getSource());
        assertEquals(1, exact.getMetadata());
        assertEquals(15, wildcard.getDurationSeconds());
        assertEquals(CooldownDecision.Source.WILDCARD_OVERRIDE, wildcard.getSource());
        assertEquals(CooldownRule.WILDCARD_META, wildcard.getMetadata());
        assertEquals(30, fallback.getDurationSeconds());
        assertEquals(CooldownDecision.Source.FIXED_GLOBAL, fallback.getSource());
        assertEquals(0, fallback.getMetadata());
    }

    @Test
    public void exactZeroExcludesWildcardAndGlobalFallback() {
        ItemFood apple = food("test:apple", 4);
        CooldownConfigSnapshot snapshot = new CooldownConfigSnapshot(
                CooldownPolicy.SCALED_ALL_FOODS,
                30,
                5,
                Arrays.asList(
                        new CooldownRule(new ResourceLocation("test:apple"), CooldownRule.WILDCARD_META, 15),
                        new CooldownRule(new ResourceLocation("test:apple"), 1, 0)
                )
        );

        CooldownDecision excluded = CooldownResolver.resolve(snapshot, new ItemStack(apple, 1, 1));

        assertTrue(excluded.isExcluded());
        assertEquals(CooldownDecision.Source.EXACT_OVERRIDE, excluded.getSource());
        assertEquals(0, excluded.getDurationSeconds());
    }

    @Test
    public void scaledPolicyUsesStackSpecificHungerAndNotSaturation() {
        MetadataFood fish = new MetadataFood();
        fish.setRegistryName("test:fish");
        CooldownConfigSnapshot snapshot = new CooldownConfigSnapshot(
                CooldownPolicy.SCALED_ALL_FOODS,
                30,
                5,
                Collections.emptyList()
        );

        CooldownDecision small = CooldownResolver.resolve(snapshot, new ItemStack(fish, 1, 0));
        CooldownDecision large = CooldownResolver.resolve(snapshot, new ItemStack(fish, 1, 1));

        assertEquals(10, small.getDurationSeconds());
        assertEquals(40, large.getDurationSeconds());
        assertEquals(CooldownDecision.Source.SCALED_GLOBAL, large.getSource());
    }

    @Test
    public void scaledPolicyUsesVanillaMetadataSensitiveCookedFishValues() {
        CooldownConfigSnapshot snapshot = new CooldownConfigSnapshot(
                CooldownPolicy.SCALED_ALL_FOODS, 30, 5, Collections.emptyList()
        );

        CooldownDecision cod = CooldownResolver.resolve(snapshot, new ItemStack(Items.COOKED_FISH, 1, 0));
        CooldownDecision salmon = CooldownResolver.resolve(snapshot, new ItemStack(Items.COOKED_FISH, 1, 1));

        assertEquals(25, cod.getDurationSeconds());
        assertEquals(30, salmon.getDurationSeconds());
    }

    @Test
    public void snapshotRejectsDuplicateScopesInsteadOfResolvingThemAgain() {
        ResourceLocation apple = new ResourceLocation("test:apple");

        assertThrows(IllegalArgumentException.class, () -> new CooldownConfigSnapshot(
                CooldownPolicy.CONFIGURED_ONLY,
                30,
                5,
                Arrays.asList(
                        new CooldownRule(apple, 0, 10),
                        new CooldownRule(apple, 0, 20)
                )
        ));
    }

    @Test
    public void nonFoodNeedsAnExplicitOverride() {
        Item drink = new Item().setRegistryName("test:drink");
        CooldownConfigSnapshot global = new CooldownConfigSnapshot(
                CooldownPolicy.FIXED_ALL_FOODS, 30, 5, Collections.emptyList()
        );
        CooldownConfigSnapshot explicit = snapshot(
                CooldownPolicy.FIXED_ALL_FOODS,
                new CooldownRule(new ResourceLocation("test:drink"), CooldownRule.WILDCARD_META, 12)
        );

        assertFalse(CooldownResolver.resolve(global, new ItemStack(drink)).hasCooldown());
        assertEquals(12, CooldownResolver.resolve(explicit, new ItemStack(drink)).getDurationSeconds());
    }

    @Test
    public void scaledResultCapsBeforeTickConversionOverflows() {
        ItemFood huge = new MetadataFood() {
            @Override
            public int getHealAmount(ItemStack stack) {
                return Integer.MAX_VALUE;
            }
        };
        huge.setRegistryName("test:huge_food");
        CooldownConfigSnapshot snapshot = new CooldownConfigSnapshot(
                CooldownPolicy.SCALED_ALL_FOODS,
                30,
                CooldownResolver.MAX_DURATION_SECONDS,
                Collections.emptyList()
        );

        CooldownDecision decision = CooldownResolver.resolve(snapshot, new ItemStack(huge));

        assertEquals(CooldownResolver.MAX_DURATION_SECONDS, decision.getDurationSeconds());
        assertEquals(CooldownResolver.MAX_DURATION_SECONDS * 20, decision.getDurationTicks());
    }

    private static CooldownConfigSnapshot snapshot(CooldownPolicy policy, CooldownRule... rules) {
        return new CooldownConfigSnapshot(policy, 30, 5, Arrays.asList(rules));
    }

    private static ItemFood food(String registryName, int hunger) {
        ItemFood food = new ItemFood(hunger, 0.1F, false);
        food.setRegistryName(registryName);
        return food;
    }

    private static class MetadataFood extends ItemFood {
        MetadataFood() {
            super(1, 1.0F, false);
        }

        @Override
        public int getHealAmount(ItemStack stack) {
            return stack.getMetadata() == 1 ? 8 : 2;
        }
    }
}
