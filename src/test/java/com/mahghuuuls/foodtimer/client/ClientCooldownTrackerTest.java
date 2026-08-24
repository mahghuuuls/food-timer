package com.mahghuuuls.foodtimer.client;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ClientCooldownTrackerTest {

    @AfterEach
    void clearTracker() {
        ClientCooldownTracker.clear();
    }

    @Test
    void expiredExactEntryFallsThroughToStillActiveWildcard() {
        String itemKey = "minecraft:golden_apple";
        long receivedAt = 100L;
        ClientCooldownTracker.updateCooldownAtTime(itemKey, 0, 20, 1, receivedAt);
        ClientCooldownTracker.updateCooldownAtTime(itemKey, -1, 100, 50, receivedAt);

        ClientCooldownTracker.Entry resolved = ClientCooldownTracker.getEntry(
                new ItemStack(Items.GOLDEN_APPLE, 1, 0),
                receivedAt + 1
        );

        // Compare the stable values because the tracker owns the stored Entry instance.
        assertSame(ClientCooldownTracker.getEntry(new ItemStack(Items.GOLDEN_APPLE, 1, 1), receivedAt + 1), resolved);
    }
}
