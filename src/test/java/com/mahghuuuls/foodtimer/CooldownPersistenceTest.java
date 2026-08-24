package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.handler.CooldownPersistence;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CooldownPersistenceTest {

    private static final ResourceLocation GOLDEN_APPLE = new ResourceLocation("minecraft:golden_apple");

    @Test
    public void exactPersistedTimerIsActiveWithoutAnyConfigurationInput() {
        NBTTagCompound persisted = persistedCooldown("minecraft:golden_apple:1", 16000L);

        assertTrue(CooldownPersistence.hasActiveCooldown(persisted, GOLDEN_APPLE, 1, 10000L));
        assertFalse(CooldownPersistence.hasActiveCooldown(persisted, GOLDEN_APPLE, 0, 10000L));
    }

    @Test
    public void wildcardPersistedTimerCoversEveryMetadataWithoutConfigurationInput() {
        NBTTagCompound persisted = persistedCooldown("minecraft:golden_apple:-1", 16000L);

        assertTrue(CooldownPersistence.hasActiveCooldown(persisted, GOLDEN_APPLE, 0, 10000L));
        assertTrue(CooldownPersistence.hasActiveCooldown(persisted, GOLDEN_APPLE, 1, 10000L));
    }

    @Test
    public void legacyUnsuffixedTimerRemainsReadable() {
        NBTTagCompound persisted = persistedCooldown("minecraft:golden_apple", 16000L);

        assertTrue(CooldownPersistence.hasActiveCooldown(persisted, GOLDEN_APPLE, 7, 10000L));
    }

    @Test
    public void expiredPersistedTimerIsInactiveAndCleaned() {
        NBTTagCompound persisted = persistedCooldown("minecraft:golden_apple:0", 9000L);

        assertFalse(CooldownPersistence.hasActiveCooldown(persisted, GOLDEN_APPLE, 0, 10000L));
        assertFalse(persisted.hasKey(CooldownPersistence.NBT_KEY_ROOT));
    }

    @Test
    public void persistedCooldownStateIsIsolatedPerPlayerCompound() {
        NBTTagCompound firstPlayer = persistedCooldown("minecraft:golden_apple:0", 16000L);
        NBTTagCompound secondPlayer = new NBTTagCompound();

        assertTrue(CooldownPersistence.hasActiveCooldown(firstPlayer, GOLDEN_APPLE, 0, 10000L));
        assertFalse(CooldownPersistence.hasActiveCooldown(secondPlayer, GOLDEN_APPLE, 0, 10000L));
    }

    @Test
    public void activeExactLookupAlsoCleansExpiredLowerPrecedenceEntry() {
        NBTTagCompound persisted = new NBTTagCompound();
        NBTTagCompound cooldowns = new NBTTagCompound();
        cooldowns.setLong("minecraft:golden_apple:0", 16000L);
        cooldowns.setLong("minecraft:golden_apple:-1", 9000L);
        persisted.setTag(CooldownPersistence.NBT_KEY_ROOT, cooldowns);

        assertTrue(CooldownPersistence.hasActiveCooldown(persisted, GOLDEN_APPLE, 0, 10000L));
        assertFalse(persisted.getCompoundTag(CooldownPersistence.NBT_KEY_ROOT)
                .hasKey("minecraft:golden_apple:-1"));
    }

    private static NBTTagCompound persistedCooldown(String key, long expiry) {
        NBTTagCompound persisted = new NBTTagCompound();
        NBTTagCompound cooldowns = new NBTTagCompound();
        cooldowns.setLong(key, expiry);
        persisted.setTag(CooldownPersistence.NBT_KEY_ROOT, cooldowns);
        return persisted;
    }
}
