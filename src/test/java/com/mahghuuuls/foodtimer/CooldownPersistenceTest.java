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
    public void testCooldownPersistenceNbtCalculation() {
        NBTTagCompound persisted = new NBTTagCompound();
        NBTTagCompound cooldowns = new NBTTagCompound();

        long currentWorldTime = 10000L;
        int durationTicks = 1200; // 60s
        long expireTime = currentWorldTime + durationTicks;

        cooldowns.setLong("minecraft:golden_apple", expireTime);
        persisted.setTag(CooldownPersistence.NBT_KEY_ROOT, cooldowns);

        // Simulate reconnecting 20 seconds later (400 ticks elapsed)
        long reconnectWorldTime = 10400L;
        long remaining = cooldowns.getLong("minecraft:golden_apple") - reconnectWorldTime;
        assertEquals(800, remaining);
        assertTrue(remaining > 0);

        // Simulate reconnecting 70 seconds later (1400 ticks elapsed) -> expired
        long laterWorldTime = 11400L;
        long laterRemaining = cooldowns.getLong("minecraft:golden_apple") - laterWorldTime;
        assertTrue(laterRemaining <= 0);
    }

    @Test
    public void testMultipleCooldownsAndCleanup() {
        NBTTagCompound cooldowns = new NBTTagCompound();
        long currentWorldTime = 50000L;

        // Golden apple: expires at 51200 (1200 ticks duration)
        cooldowns.setLong("minecraft:golden_apple", 51200L);
        // Enchanted golden apple: expires at 56000 (6000 ticks duration)
        cooldowns.setLong("minecraft:enchanted_golden_apple", 56000L);

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
        assertTrue(expired.contains("minecraft:golden_apple"));

        for (String key : expired) {
            cooldowns.removeTag(key);
        }

        assertFalse(cooldowns.hasKey("minecraft:golden_apple"));
        assertTrue(cooldowns.hasKey("minecraft:enchanted_golden_apple"));
        assertEquals(4000, cooldowns.getLong("minecraft:enchanted_golden_apple") - simulatedLoginTime);
    }
}
