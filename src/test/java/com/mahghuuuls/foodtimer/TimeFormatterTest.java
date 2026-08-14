package com.mahghuuuls.foodtimer;

import com.mahghuuuls.foodtimer.util.TimeFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeFormatterTest {

    @Test
    public void testShortDurations() {
        assertEquals("0s", TimeFormatter.format(0));
        assertEquals("15s", TimeFormatter.format(15));
        assertEquals("59s", TimeFormatter.format(59));
    }

    @Test
    public void testMinuteDurations() {
        assertEquals("1m 0s", TimeFormatter.format(60));
        assertEquals("1m 15s", TimeFormatter.format(75));
        assertEquals("5m 0s", TimeFormatter.format(300));
        assertEquals("59m 59s", TimeFormatter.format(3599));
    }

    @Test
    public void testHourDurations() {
        assertEquals("1h 0m 0s", TimeFormatter.format(3600));
        assertEquals("1h 1m 5s", TimeFormatter.format(3665));
        assertEquals("2h 30m 45s", TimeFormatter.format(9045));
    }

    @Test
    public void testNegativeClamping() {
        assertEquals("0s", TimeFormatter.format(-10));
    }
}
