package com.mahghuuuls.foodtimer.handler;

import com.mahghuuuls.foodtimer.network.FoodTimerPacketHandler;
import com.mahghuuuls.foodtimer.network.SPacketFoodCooldown;
import com.mahghuuuls.foodtimer.policy.CooldownDecision;
import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Manages server-side persistence and network synchronization of metadata-aware food cooldowns.
 */
public class CooldownPersistence {

    public static final String NBT_KEY_ROOT = "FoodTimerCooldowns";

    /**
     * Saves an active cooldown for a specific food rule on the player and sends the sync packet.
     */
    public static void saveCooldown(EntityPlayer player, CooldownDecision decision) {
        if (player == null || player.getEntityWorld().isRemote || decision == null || !decision.hasCooldown()) {
            return;
        }

        int durationTicks = decision.getDurationTicks();
        long currentWorldTime = player.getEntityWorld().getTotalWorldTime();
        long expireWorldTime = currentWorldTime + durationTicks;

        String key = formatKey(decision.getRegistryName().toString(), decision.getMetadata());

        NBTTagCompound persisted = getOrCreatePersistedTag(player);
        NBTTagCompound foodCooldowns = persisted.getCompoundTag(NBT_KEY_ROOT);
        foodCooldowns.setLong(key, expireWorldTime);
        persisted.setTag(NBT_KEY_ROOT, foodCooldowns);

        if (player instanceof EntityPlayerMP) {
            FoodTimerPacketHandler.INSTANCE.sendTo(
                    new SPacketFoodCooldown(
                            decision.getRegistryName().toString(),
                            decision.getMetadata(),
                            durationTicks,
                            expireWorldTime,
                            currentWorldTime
                    ),
                    (EntityPlayerMP) player
            );
        }

        LoggerHelper.debug("Saved persistent cooldown for '{}' on player '{}' (expires at world time {})",
                key, player.getName(), expireWorldTime);
    }

    /**
     * Checks if the given ItemStack is currently on active cooldown for the player.
     */
    public static boolean hasActiveCooldown(EntityPlayer player, ItemStack stack) {
        if (player == null || player.getEntityWorld().isRemote || stack == null || stack.isEmpty()) {
            return false;
        }

        NBTTagCompound persisted = getOrCreatePersistedTag(player);
        ResourceLocation reg = stack.getItem().getRegistryName();
        if (reg == null) {
            return false;
        }
        return hasActiveCooldown(
                persisted,
                reg,
                stack.getMetadata(),
                player.getEntityWorld().getTotalWorldTime()
        );
    }

    /**
     * Production persisted-state lookup. Current configuration is intentionally not an input.
     */
    public static boolean hasActiveCooldown(NBTTagCompound persisted, ResourceLocation registryName,
                                            int metadata, long currentWorldTime) {
        if (persisted == null || registryName == null || !persisted.hasKey(NBT_KEY_ROOT)) {
            return false;
        }

        NBTTagCompound foodCooldowns = persisted.getCompoundTag(NBT_KEY_ROOT);
        String itemKey = registryName.toString();
        List<String> expiredKeys = new ArrayList<>();
        boolean active = false;

        String exactKey = formatKey(itemKey, metadata);
        if (foodCooldowns.hasKey(exactKey)) {
            long expire = foodCooldowns.getLong(exactKey);
            if (expire > currentWorldTime) {
                active = true;
            } else {
                expiredKeys.add(exactKey);
            }
        }

        String wildcardKey = formatKey(itemKey, -1);
        if (foodCooldowns.hasKey(wildcardKey)) {
            long expire = foodCooldowns.getLong(wildcardKey);
            if (expire > currentWorldTime) {
                active = true;
            } else {
                expiredKeys.add(wildcardKey);
            }
        }

        if (foodCooldowns.hasKey(itemKey)) {
            long expire = foodCooldowns.getLong(itemKey);
            if (expire > currentWorldTime) {
                active = true;
            } else {
                expiredKeys.add(itemKey);
            }
        }

        removeExpiredKeys(persisted, foodCooldowns, expiredKeys);
        return active;
    }

    /**
     * Restores active cooldowns for the player upon login or respawn and syncs to client.
     */
    public static void restoreCooldowns(EntityPlayer player) {
        if (player == null || player.getEntityWorld().isRemote) {
            return;
        }

        NBTTagCompound persisted = getOrCreatePersistedTag(player);
        if (!persisted.hasKey(NBT_KEY_ROOT)) {
            return;
        }

        NBTTagCompound foodCooldowns = persisted.getCompoundTag(NBT_KEY_ROOT);
        long currentWorldTime = player.getEntityWorld().getTotalWorldTime();
        List<String> expiredKeys = new ArrayList<>();

        for (String entryKey : foodCooldowns.getKeySet()) {
            long expireWorldTime = foodCooldowns.getLong(entryKey);
            long remainingTicks = expireWorldTime - currentWorldTime;

            if (remainingTicks > 0) {
                ParsedKey parsed = parseRegisteredKey(entryKey, Item.REGISTRY::containsKey);
                if (parsed == null) {
                    expiredKeys.add(entryKey);
                    LoggerHelper.warn("Discarding invalid persisted cooldown key '{}' for player '{}'",
                            entryKey, player.getName());
                    continue;
                }
                if (player instanceof EntityPlayerMP) {
                    FoodTimerPacketHandler.INSTANCE.sendTo(
                            new SPacketFoodCooldown(
                                    parsed.itemKey,
                                    parsed.metadata,
                                    clampTicks(remainingTicks),
                                    expireWorldTime,
                                    currentWorldTime
                            ),
                            (EntityPlayerMP) player
                    );
                }
                LoggerHelper.debug("Restored cooldown for '{}' on player '{}' ({} ticks remaining)",
                        entryKey, player.getName(), remainingTicks);
            } else {
                expiredKeys.add(entryKey);
            }
        }

        // Clean up expired entries
        for (String key : expiredKeys) {
            foodCooldowns.removeTag(key);
        }
        if (foodCooldowns.isEmpty()) {
            persisted.removeTag(NBT_KEY_ROOT);
        } else {
            persisted.setTag(NBT_KEY_ROOT, foodCooldowns);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        restoreCooldowns(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent event) {
        restoreCooldowns(event.player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        restoreCooldowns(event.player);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.getEntityWorld().isRemote) {
            if (event.player.ticksExisted == 1) {
                restoreCooldowns(event.player);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            NBTTagCompound originalPersisted = getOrCreatePersistedTag(event.getOriginal());
            if (originalPersisted.hasKey(NBT_KEY_ROOT)) {
                NBTTagCompound clonePersisted = getOrCreatePersistedTag(event.getEntityPlayer());
                clonePersisted.setTag(NBT_KEY_ROOT, originalPersisted.getCompoundTag(NBT_KEY_ROOT).copy());
            }
        }
    }

    public static String formatKey(String itemKey, int metadata) {
        return itemKey + ":" + metadata;
    }

    private static int clampTicks(long ticks) {
        return ticks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) ticks;
    }

    private static void removeExpiredKeys(NBTTagCompound persisted, NBTTagCompound cooldowns,
                                          List<String> expiredKeys) {
        for (String key : expiredKeys) {
            cooldowns.removeTag(key);
        }
        if (cooldowns.isEmpty()) {
            persisted.removeTag(NBT_KEY_ROOT);
        } else if (!expiredKeys.isEmpty()) {
            persisted.setTag(NBT_KEY_ROOT, cooldowns);
        }
    }

    static class ParsedKey {
        final String itemKey;
        final int metadata;

        ParsedKey(String itemKey, int metadata) {
            this.itemKey = itemKey;
            this.metadata = metadata;
        }

    }

    static ParsedKey parseRegisteredKey(String key, Predicate<ResourceLocation> registeredItem) {
        if (key == null || registeredItem == null) {
            return null;
        }

        try {
            ResourceLocation completeKey = new ResourceLocation(key);
            if (registeredItem.test(completeKey)) {
                return new ParsedKey(key, -1);
            }
        } catch (RuntimeException invalidCompleteKey) {
            // Metadata-aware keys contain a second colon and are parsed below.
        }

        int lastColon = key.lastIndexOf(':');
        if (lastColon > 0) {
            String suffix = key.substring(lastColon + 1);
            try {
                int meta = Integer.parseInt(suffix);
                if (meta < -1) {
                    return null;
                }
                String itemKey = key.substring(0, lastColon);
                ResourceLocation registryName = new ResourceLocation(itemKey);
                return registeredItem.test(registryName) ? new ParsedKey(itemKey, meta) : null;
            } catch (NumberFormatException ignored) {
                return null;
            } catch (RuntimeException invalidItemKey) {
                return null;
            }
        }
        return null;
    }

    private static NBTTagCompound getOrCreatePersistedTag(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }
}
