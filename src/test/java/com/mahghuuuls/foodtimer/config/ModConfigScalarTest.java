package com.mahghuuuls.foodtimer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModConfigScalarTest {

    @Test
    public void rawNonnumericAndOutOfRangeValuesUseShippedDefaults() {
        assertEquals(30, ModConfig.parseBoundedPositiveInt("abc", "fixedFoodCooldownSeconds", 30));
        assertEquals(30, ModConfig.parseBoundedPositiveInt("0", "fixedFoodCooldownSeconds", 30));
        assertEquals(5, ModConfig.parseBoundedPositiveInt("-1", "secondsPerHungerPoint", 5));
        assertEquals(5, ModConfig.parseBoundedPositiveInt("107374183", "secondsPerHungerPoint", 5));
    }

    @Test
    public void rawValidValuesArePreserved() {
        assertEquals(45, ModConfig.parseBoundedPositiveInt("45", "fixedFoodCooldownSeconds", 30));
        assertEquals(7, ModConfig.parseBoundedPositiveInt("7", "secondsPerHungerPoint", 5));
        assertEquals(107374182, ModConfig.parseBoundedPositiveInt(
                "107374182", "fixedFoodCooldownSeconds", 30
        ));
    }
}
