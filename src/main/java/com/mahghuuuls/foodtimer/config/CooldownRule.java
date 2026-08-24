package com.mahghuuuls.foodtimer.config;

import net.minecraft.util.ResourceLocation;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
import java.util.Objects;

/**
 * Immutable value object representing a cooldown rule for an item or item variant.
 */
public final class CooldownRule {

    public static final int WILDCARD_META = -1;

    private final ResourceLocation registryName;
    private final int metadata;
    private final int durationSeconds;
    private final int durationTicks;

    public CooldownRule(ResourceLocation registryName, int metadata, int durationSeconds) {
        this.registryName = Objects.requireNonNull(registryName, "registryName cannot be null");
        if (metadata < WILDCARD_META) {
            throw new IllegalArgumentException("metadata out of range: " + metadata);
        }
        if (durationSeconds < 0 || durationSeconds > CooldownResolver.MAX_DURATION_SECONDS) {
            throw new IllegalArgumentException("durationSeconds out of range: " + durationSeconds);
        }
        this.metadata = metadata;
        this.durationSeconds = durationSeconds;
        this.durationTicks = Math.multiplyExact(durationSeconds, CooldownResolver.TICKS_PER_SECOND);
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

    public int getMetadata() {
        return metadata;
    }

    public boolean isWildcard() {
        return metadata == WILDCARD_META;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public boolean matches(ResourceLocation itemRegistryName, int itemMetadata) {
        if (!this.registryName.equals(itemRegistryName)) {
            return false;
        }
        return isWildcard() || this.metadata == itemMetadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CooldownRule that = (CooldownRule) o;
        return metadata == that.metadata &&
                durationSeconds == that.durationSeconds &&
                registryName.equals(that.registryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryName, metadata, durationSeconds);
    }

    @Override
    public String toString() {
        return "CooldownRule{" +
                "registryName=" + registryName +
                ", metadata=" + (isWildcard() ? "*" : metadata) +
                ", durationSeconds=" + durationSeconds +
                '}';
    }
}
