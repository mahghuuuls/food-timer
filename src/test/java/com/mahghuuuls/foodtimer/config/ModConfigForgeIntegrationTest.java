package com.mahghuuuls.foodtimer.config;

import com.mahghuuuls.foodtimer.FoodTimerMod;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import net.minecraft.init.Bootstrap;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModConfigForgeIntegrationTest {

    private static Field minecraftHomeField;
    private static Object originalMinecraftHome;

    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    public static void initializeForgeConfigurationContext() throws Exception {
        Bootstrap.register();
        minecraftHomeField = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHomeField.setAccessible(true);
        originalMinecraftHome = minecraftHomeField.get(null);
    }

    @AfterEach
    public void resetConfigSingleton() {
        ModConfig.config = null;
    }

    @AfterAll
    public static void restoreForgeConfigurationContext() throws Exception {
        minecraftHomeField.set(null, originalMinecraftHome);
    }

    @Test
    public void releasedStyleFileIsUpgradedWithoutChangingExistingRules() throws Exception {
        minecraftHomeField.set(null, temporaryDirectory.toFile());
        Path configPath = temporaryDirectory.resolve("foodtimer.cfg");
        Files.write(configPath, ("# Configuration file\n\n" +
                "general {\n" +
                "    B:enableTooltips=false\n" +
                "    S:foodCooldowns <\n" +
                "        minecraft:bread=17\n" +
                "     >\n" +
                "    S:tooltipPrefix=Wait: \n" +
                "}\n").getBytes(StandardCharsets.UTF_8));

        ModConfig.init(configPath.toFile());

        String migrated = new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8);
        assertEquals(CooldownPolicy.CONFIGURED_ONLY, ModConfig.cooldownPolicy);
        assertEquals(1, ModConfig.getGameplaySnapshot().getOverrides().size());
        assertEquals(17, ModConfig.getGameplaySnapshot().getOverrides().get(0).getDurationSeconds());
        assertFalse(ModConfig.enableTooltips);
        assertEquals("Wait:", ModConfig.tooltipPrefix.trim());
        assertTrue(migrated.contains("minecraft:bread=17"));
        assertTrue(migrated.contains("S:cooldownPolicy=CONFIGURED_ONLY"));
        assertTrue(migrated.contains("I:fixedFoodCooldownSeconds=30"));
        assertTrue(migrated.contains("I:secondsPerHungerPoint=5"));
    }

    @Test
    public void forgeConfigurationRawInvalidScalarsWarnAndUseDefaults() throws Exception {
        minecraftHomeField.set(null, temporaryDirectory.toFile());
        Path configPath = temporaryDirectory.resolve("foodtimer-invalid.cfg");
        Files.write(configPath, ("# Configuration file\n\n" +
                "general {\n" +
                "    I:fixedFoodCooldownSeconds=abc\n" +
                "    I:secondsPerHungerPoint=0\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        RecordingAppender appender = new RecordingAppender();
        org.apache.logging.log4j.core.Logger logger =
                (org.apache.logging.log4j.core.Logger) FoodTimerMod.LOGGER;
        appender.start();
        logger.addAppender(appender);

        try {
            ModConfig.init(configPath.toFile());
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }

        assertEquals(30, ModConfig.fixedFoodCooldownSeconds);
        assertEquals(5, ModConfig.secondsPerHungerPoint);
        assertTrue(appender.contains("Invalid fixedFoodCooldownSeconds 'abc'"));
        assertTrue(appender.contains("Invalid secondsPerHungerPoint '0'"));
    }

    private static final class RecordingAppender extends AbstractAppender {
        private final List<String> messages = new ArrayList<>();

        RecordingAppender() {
            super("FoodTimerConfigTest", null, PatternLayout.createDefaultLayout(), false);
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }

        boolean contains(String text) {
            for (String message : messages) {
                if (message.contains(text)) {
                    return true;
                }
            }
            return false;
        }
    }
}
