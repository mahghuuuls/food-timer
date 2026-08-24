package com.mahghuuuls.foodtimer.client;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientConnectionGenerationTest {

    @AfterEach
    void clearPolicy() {
        ClientPolicyState.clear();
    }

    @Test
    void queuedOldConnectionPacketCannotRepopulateStateAfterBoundaryCleanup() {
        CooldownConfigSnapshot oldSnapshot = snapshot(CooldownPolicy.FIXED_ALL_FOODS, 30);
        CooldownConfigSnapshot newSnapshot = snapshot(CooldownPolicy.SCALED_ALL_FOODS, 40);
        long oldGeneration = ClientConnectionGeneration.current();

        Runnable queuedOldPacket = () -> ClientConnectionGeneration.runIfCurrent(
                oldGeneration,
                () -> ClientPolicyState.install(oldSnapshot)
        );

        long cleanupGeneration = ClientConnectionGeneration.advance();
        ClientConnectionGeneration.runIfCurrent(cleanupGeneration, ClientPolicyState::clear);
        queuedOldPacket.run();
        assertFalse(ClientPolicyState.isAvailable());

        long newConnectionGeneration = ClientConnectionGeneration.advance();
        ClientConnectionGeneration.runIfCurrent(
                newConnectionGeneration,
                () -> ClientPolicyState.install(newSnapshot)
        );
        assertTrue(ClientPolicyState.isAvailable());
        assertSame(newSnapshot, ClientPolicyState.getSnapshot());
    }

    @Test
    void dimensionUnloadRetainsConnectionStateButFinalWorldTeardownClears() {
        assertFalse(ClientConnectionStateHandler.shouldClearOnWorldUnload(false, false));
        assertFalse(ClientConnectionStateHandler.shouldClearOnWorldUnload(true, true));
        assertTrue(ClientConnectionStateHandler.shouldClearOnWorldUnload(true, false));
    }

    private static CooldownConfigSnapshot snapshot(CooldownPolicy policy, int fixedSeconds) {
        return new CooldownConfigSnapshot(policy, fixedSeconds, 5, Collections.emptyList());
    }
}
