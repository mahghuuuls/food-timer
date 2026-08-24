package com.mahghuuuls.foodtimer.client;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.network.SPacketCooldownPolicySnapshot;

import java.util.Objects;

/**
 * Atomic connection-scoped client view of the server's complete gameplay policy.
 */
public final class ClientPolicyState {

    private static volatile CooldownConfigSnapshot snapshot;

    private ClientPolicyState() {
    }

    public static CooldownConfigSnapshot getSnapshot() {
        return snapshot;
    }

    public static boolean isAvailable() {
        return snapshot != null;
    }

    public static void install(CooldownConfigSnapshot completeSnapshot) {
        snapshot = Objects.requireNonNull(completeSnapshot, "completeSnapshot");
    }

    /**
     * Applies one fully decoded message or makes policy state unavailable. Call on the client thread.
     */
    public static void apply(SPacketCooldownPolicySnapshot message) {
        if (message == null || !message.isValid()) {
            clear();
            return;
        }
        install(message.getSnapshot());
    }

    public static void clear() {
        snapshot = null;
    }
}
