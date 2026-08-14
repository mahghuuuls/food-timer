package com.mahghuuuls.foodtimer.util;

import com.mahghuuuls.foodtimer.FoodTimerMod;
import com.mahghuuuls.foodtimer.config.ModConfig;

/**
 * Diagnostic logger helper that filters debug messages based on configuration.
 */
public final class LoggerHelper {

    private static final String DEBUG_PREFIX = "[FoodTimer-Debug] ";

    private LoggerHelper() {
    }

    public static void debug(String message, Object... params) {
        if (ModConfig.enableDebugLogging) {
            FoodTimerMod.LOGGER.info(DEBUG_PREFIX + message, params);
        }
    }

    public static void info(String message, Object... params) {
        FoodTimerMod.LOGGER.info(message, params);
    }

    public static void warn(String message, Object... params) {
        FoodTimerMod.LOGGER.warn(message, params);
    }

    public static void error(String message, Object... params) {
        FoodTimerMod.LOGGER.error(message, params);
    }
}
