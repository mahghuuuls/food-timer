package com.mahghuuuls.foodtimer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side tracker for metadata-aware food cooldowns.
 */
public class ClientCooldownTracker {

    public static class Entry {
        public final int durationTicks;
        public final long clientSetWorldTime;
        public final int remainingTicksAtReceive;

        public Entry(int durationTicks, long clientSetWorldTime, int remainingTicksAtReceive) {
            this.durationTicks = durationTicks;
            this.clientSetWorldTime = clientSetWorldTime;
            this.remainingTicksAtReceive = remainingTicksAtReceive;
        }

        public float getFraction(long currentClientWorldTime, float partialTicks) {
            float elapsed = (currentClientWorldTime - clientSetWorldTime) + partialTicks;
            float remaining = remainingTicksAtReceive - elapsed;
            if (remaining <= 0.0F) {
                return 0.0F;
            }
            float total = (durationTicks > 0) ? (float) durationTicks : (float) remainingTicksAtReceive;
            float fraction = remaining / total;
            return Math.max(0.0F, Math.min(1.0F, fraction));
        }

        public boolean isExpired(long currentClientWorldTime) {
            return (currentClientWorldTime - clientSetWorldTime) >= remainingTicksAtReceive;
        }
    }

    // Key format: "modid:item:meta" or "modid:item:-1"
    private static final Map<String, Entry> ACTIVE_COOLDOWNS = new ConcurrentHashMap<>();

    public static void updateCooldown(String itemKey, int metadata, int durationTicks, int remainingTicks) {
        long currentClientWorldTime = (Minecraft.getMinecraft().world != null) ? Minecraft.getMinecraft().world.getTotalWorldTime() : 0L;
        updateCooldownAtTime(itemKey, metadata, durationTicks, remainingTicks, currentClientWorldTime);
    }

    static void updateCooldownAtTime(String itemKey, int metadata, int durationTicks,
                                     int remainingTicks, long currentClientWorldTime) {
        String key = itemKey + ":" + metadata;
        if (remainingTicks <= 0) {
            ACTIVE_COOLDOWNS.remove(key);
        } else {
            ACTIVE_COOLDOWNS.put(key, new Entry(durationTicks, currentClientWorldTime, remainingTicks));
        }
    }

    public static void clear() {
        ACTIVE_COOLDOWNS.clear();
    }

    public static Entry getEntry(ItemStack stack) {
        long worldTime = (Minecraft.getMinecraft().world != null) ? Minecraft.getMinecraft().world.getTotalWorldTime() : 0L;
        return getEntry(stack, worldTime);
    }

    static Entry getEntry(ItemStack stack, long worldTime) {
        if (stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            return null;
        }
        ResourceLocation reg = stack.getItem().getRegistryName();
        String itemKey = reg.toString();
        int meta = stack.getMetadata();

        // Check exact metadata first
        String exactKey = itemKey + ":" + meta;
        Entry exact = ACTIVE_COOLDOWNS.get(exactKey);
        if (exact != null) {
            if (exact.isExpired(worldTime)) {
                ACTIVE_COOLDOWNS.remove(exactKey);
            } else {
                return exact;
            }
        }

        // Check wildcard (-1)
        String wildcardKey = itemKey + ":-1";
        Entry wildcard = ACTIVE_COOLDOWNS.get(wildcardKey);
        if (wildcard != null) {
            if (wildcard.isExpired(worldTime)) {
                ACTIVE_COOLDOWNS.remove(wildcardKey);
                return null;
            }
            return wildcard;
        }

        return null;
    }

    public static boolean hasCooldown(ItemStack stack) {
        return getEntry(stack) != null;
    }

    public static float getCooldownFraction(ItemStack stack, float partialTicks) {
        Entry entry = getEntry(stack);
        if (entry == null) {
            return 0.0F;
        }
        long worldTime = (Minecraft.getMinecraft().world != null) ? Minecraft.getMinecraft().world.getTotalWorldTime() : 0L;
        return entry.getFraction(worldTime, partialTicks);
    }
}
