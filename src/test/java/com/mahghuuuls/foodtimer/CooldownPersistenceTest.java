package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.handler.CooldownPersistence;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CooldownPersistenceTest {

    @Test
    public void testMetadataSpecificCooldownPersistenceNbtCalculation() {
        NBTTagCompound persisted = new NBTTagCompound();
        NBTTagCompound cooldowns = new NBTTagCompound();

        long currentWorldTime = 10000L;
        long regularGappleExpire = currentWorldTime + 1200; // 60s (meta 0)
        long enchantedGappleExpire = currentWorldTime + 6000; // 300s (meta 1)

        cooldowns.setLong("minecraft:golden_apple:0", regularGappleExpire);
        cooldowns.setLong("minecraft:golden_apple:1", enchantedGappleExpire);
        persisted.setTag(CooldownPersistence.NBT_KEY_ROOT, cooldowns);

        // Reconnect 20 seconds later (400 ticks elapsed)
        long reconnectWorldTime = 10400L;
        long remaining0 = cooldowns.getLong("minecraft:golden_apple:0") - reconnectWorldTime;
        long remaining1 = cooldowns.getLong("minecraft:golden_apple:1") - reconnectWorldTime;

        assertEquals(800, remaining0);
        assertEquals(5600, remaining1);
        assertTrue(remaining0 > 0);
        assertTrue(remaining1 > 0);

        // Reconnect 70 seconds later (1400 ticks elapsed) -> meta 0 expired, meta 1 has 4600 ticks left
        long laterWorldTime = 11400L;
        long laterRemaining0 = cooldowns.getLong("minecraft:golden_apple:0") - laterWorldTime;
        long laterRemaining1 = cooldowns.getLong("minecraft:golden_apple:1") - laterWorldTime;

        assertTrue(laterRemaining0 <= 0);
        assertEquals(4600, laterRemaining1);
    }

    @Test
    public void testMultipleCooldownsAndCleanup() {
        NBTTagCompound cooldowns = new NBTTagCompound();
        long currentWorldTime = 50000L;

        // Golden apple meta 0: expires at 51200 (1200 ticks duration)
        cooldowns.setLong("minecraft:golden_apple:0", 51200L);
        // Golden apple meta 1: expires at 56000 (6000 ticks duration)
        cooldowns.setLong("minecraft:golden_apple:1", 56000L);

        // Time advanced to 52000: regular golden apple expired, enchanted has 4000 ticks left
        long simulatedLoginTime = 52000L;
        List<String> expired = new ArrayList<>();

        for (String key : cooldowns.getKeySet()) {
            long expire = cooldowns.getLong(key);
            long remaining = expire - simulatedLoginTime;
            if (remaining <= 0) {
                expired.add(key);
            }
        }

        assertEquals(1, expired.size());
        assertTrue(expired.contains("minecraft:golden_apple:0"));

        for (String key : expired) {
            cooldowns.removeTag(key);
        }

        assertFalse(cooldowns.hasKey("minecraft:golden_apple:0"));
        assertTrue(cooldowns.hasKey("minecraft:golden_apple:1"));
        assertEquals(4000, cooldowns.getLong("minecraft:golden_apple:1") - simulatedLoginTime);
    }
}
