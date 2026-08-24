package com.mahghuuuls.foodtimer.client;

/**
 * Orders queued client work against connection replacement and teardown.
 */
public final class ClientConnectionGeneration {

    private static long current;

    private ClientConnectionGeneration() {
    }

    public static synchronized long current() {
        return current;
    }

    public static synchronized long advance() {
        return ++current;
    }

    public static synchronized boolean isCurrent(long generation) {
        return current == generation;
    }

    public static synchronized void runIfCurrent(long generation, Runnable action) {
        if (current == generation) {
            action.run();
        }
    }
}
