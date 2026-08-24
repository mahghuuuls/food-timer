package com.mahghuuuls.foodtimer.client;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.RuleRegistry;
import com.mahghuuuls.foodtimer.network.SPacketCooldownPolicySnapshot;
import com.mahghuuuls.foodtimer.policy.CooldownDecision;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientPolicyStateTest {

    @AfterEach
    void clearState() {
        ClientPolicyState.clear();
        RuleRegistry.clear();
    }

    @Test
    void unavailableInstallReplaceAndClearAreAtomic() {
        assertFalse(ClientPolicyState.isAvailable());
        assertNull(ClientPolicyState.getSnapshot());

        CooldownConfigSnapshot first = snapshot(CooldownPolicy.FIXED_ALL_FOODS, 30);
        ClientPolicyState.install(first);
        assertTrue(ClientPolicyState.isAvailable());
        assertSame(first, ClientPolicyState.getSnapshot());

        CooldownConfigSnapshot replacement = snapshot(CooldownPolicy.SCALED_ALL_FOODS, 12);
        ClientPolicyState.install(replacement);
        assertSame(replacement, ClientPolicyState.getSnapshot());

        ClientPolicyState.clear();
        assertFalse(ClientPolicyState.isAvailable());
    }

    @Test
    void invalidMessageClearsPriorConnectionSnapshot() {
        ClientPolicyState.apply(new SPacketCooldownPolicySnapshot(snapshot(CooldownPolicy.FIXED_ALL_FOODS, 30)));
        assertTrue(ClientPolicyState.isAvailable());

        ClientPolicyState.apply(new SPacketCooldownPolicySnapshot());
        assertFalse(ClientPolicyState.isAvailable());
    }

    @Test
    void tooltipDecisionUsesInstalledServerSnapshotAndSuppressesUnavailableOrExcludedState() {
        ItemStack bread = new ItemStack(Items.BREAD);
        assertFalse(ClientTooltipHandler.resolveAuthoritativeDecision(bread).hasCooldown());

        RuleRegistry.install(snapshot(CooldownPolicy.FIXED_ALL_FOODS, 99));
        ClientPolicyState.install(snapshot(CooldownPolicy.FIXED_ALL_FOODS, 47));
        CooldownDecision serverDecision = ClientTooltipHandler.resolveAuthoritativeDecision(bread);
        assertTrue(serverDecision.hasCooldown());
        assertEquals(47, serverDecision.getDurationSeconds());

        CooldownConfigSnapshot excluded = new CooldownConfigSnapshot(
                CooldownPolicy.FIXED_ALL_FOODS,
                30,
                5,
                Collections.singletonList(new CooldownRule(new ResourceLocation("minecraft:bread"), -1, 0))
        );
        ClientPolicyState.install(excluded);
        assertTrue(ClientTooltipHandler.resolveAuthoritativeDecision(bread).isExcluded());

        ClientPolicyState.clear();
        assertFalse(ClientTooltipHandler.resolveAuthoritativeDecision(bread).hasCooldown());
    }

    private static CooldownConfigSnapshot snapshot(CooldownPolicy policy, int fixedSeconds) {
        return new CooldownConfigSnapshot(policy, fixedSeconds, 5, Collections.emptyList());
    }
}
