package com.mahghuuuls.foodtimer.handler;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CooldownPersistenceKeyTest {

    @Test
    public void activeLegacyNumericItemPathIsRecognizedBeforeMetadataSuffixParsing() {
        String legacyKey = "examplemod:123";
        NBTTagCompound cooldowns = new NBTTagCompound();
        cooldowns.setLong(legacyKey, 16000L);

        CooldownPersistence.ParsedKey parsed = CooldownPersistence.parseRegisteredKey(
                legacyKey,
                registryName -> registryName.equals(new ResourceLocation("examplemod", "123"))
        );

        assertTrue(cooldowns.getLong(legacyKey) > 10000L);
        assertNotNull(parsed);
        assertEquals(legacyKey, parsed.itemKey);
        assertEquals(-1, parsed.metadata);
        assertTrue(cooldowns.hasKey(legacyKey));
    }

    @Test
    public void metadataAwareNumericPathStillParsesItsFinalSuffix() {
        CooldownPersistence.ParsedKey parsed = CooldownPersistence.parseRegisteredKey(
                "examplemod:123:7",
                registryName -> registryName.equals(new ResourceLocation("examplemod", "123"))
        );

        assertNotNull(parsed);
        assertEquals("examplemod:123", parsed.itemKey);
        assertEquals(7, parsed.metadata);
    }
}
