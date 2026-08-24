package com.mahghuuuls.foodtimer.policy;

import net.minecraft.util.ResourceLocation;

import java.util.Objects;

/**
 * Complete result of resolving one stack against one gameplay snapshot.
 */
public final class CooldownDecision {

    public enum Outcome {
        COOLDOWN,
        EXCLUDED,
        NONE
    }

    public enum Source {
        EXACT_OVERRIDE,
        WILDCARD_OVERRIDE,
        FIXED_GLOBAL,
        SCALED_GLOBAL,
        NONE
    }

    private static final CooldownDecision NONE = new CooldownDecision(
            Outcome.NONE, Source.NONE, null, 0, 0, 0
    );

    private final Outcome outcome;
    private final Source source;
    private final ResourceLocation registryName;
    private final int metadata;
    private final int durationSeconds;
    private final int durationTicks;

    private CooldownDecision(Outcome outcome, Source source, ResourceLocation registryName,
                             int metadata, int durationSeconds, int durationTicks) {
        this.outcome = Objects.requireNonNull(outcome, "outcome");
        this.source = Objects.requireNonNull(source, "source");
        this.registryName = registryName;
        this.metadata = metadata;
        this.durationSeconds = durationSeconds;
        this.durationTicks = durationTicks;
    }

    public static CooldownDecision cooldown(Source source, ResourceLocation registryName,
                                             int metadata, int durationSeconds) {
        Objects.requireNonNull(registryName, "registryName");
        if (durationSeconds <= 0 || durationSeconds > CooldownResolver.MAX_DURATION_SECONDS) {
            throw new IllegalArgumentException("durationSeconds out of range: " + durationSeconds);
        }
        return new CooldownDecision(
                Outcome.COOLDOWN,
                source,
                registryName,
                metadata,
                durationSeconds,
                Math.multiplyExact(durationSeconds, CooldownResolver.TICKS_PER_SECOND)
        );
    }

    public static CooldownDecision excluded(Source source, ResourceLocation registryName, int metadata) {
        return new CooldownDecision(
                Outcome.EXCLUDED,
                source,
                Objects.requireNonNull(registryName, "registryName"),
                metadata,
                0,
                0
        );
    }

    public static CooldownDecision none() {
        return NONE;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public Source getSource() {
        return source;
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

    public int getMetadata() {
        return metadata;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public boolean hasCooldown() {
        return outcome == Outcome.COOLDOWN;
    }

    public boolean isExcluded() {
        return outcome == Outcome.EXCLUDED;
    }
}
