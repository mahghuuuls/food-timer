package com.mahghuuuls.foodtimer.util;

/**
 * Utility class for formatting durations in seconds into human-readable time strings.
 */
public final class TimeFormatter {

    private TimeFormatter() {
    }

    /**
     * Formats a duration in seconds into a clean human-readable string.
     * Examples:
     * - 45 -> "45s"
     * - 60 -> "1m 0s"
     * - 75 -> "1m 15s"
     * - 300 -> "5m 0s"
     * - 3600 -> "1h 0m 0s"
     * - 3665 -> "1h 1m 5s"
     *
     * @param totalSeconds The total duration in seconds.
     * @return Formatted string.
     */
    public static String format(int totalSeconds) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }

        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }

        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else {
            return minutes + "m " + seconds + "s";
        }
    }
}
