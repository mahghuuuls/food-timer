package com.mahghuuuls.foodtimer.handler;

import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages persistence of active food cooldowns across player disconnections,
 * world saves/reloads, and respawns using player NBT.
 */
public class CooldownPersistence {

    public static final String NBT_KEY_ROOT = "FoodTimerCooldowns";

    /**
     * Records an active cooldown for an item on the player's persistent NBT.
     */
    public static void saveCooldown(EntityPlayer player, Item item, int durationTicks) {
        if (player == null || player.getEntityWorld().isRemote || item == null || item.getRegistryName() == null || durationTicks <= 0) {
            return;
        }

        long currentWorldTime = player.getEntityWorld().getTotalWorldTime();
        long expireWorldTime = currentWorldTime + durationTicks;

        NBTTagCompound persisted = getOrCreatePersistedTag(player);
        NBTTagCompound foodCooldowns = persisted.getCompoundTag(NBT_KEY_ROOT);
        foodCooldowns.setLong(item.getRegistryName().toString(), expireWorldTime);
        persisted.setTag(NBT_KEY_ROOT, foodCooldowns);
    }

    /**
     * Restores any active cooldowns for the player upon login or respawn.
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

        for (String itemKey : foodCooldowns.getKeySet()) {
            long expireWorldTime = foodCooldowns.getLong(itemKey);
            long remainingTicks = expireWorldTime - currentWorldTime;

            if (remainingTicks > 0) {
                Item item = Item.REGISTRY.getObject(new ResourceLocation(itemKey));
                if (item != null) {
                    player.getCooldownTracker().setCooldown(item, (int) remainingTicks);
                    LoggerHelper.debug("Restored cooldown for '{}' on player '{}' ({} ticks remaining)",
                            itemKey, player.getName(), remainingTicks);
                } else {
                    expiredKeys.add(itemKey);
                }
            } else {
                expiredKeys.add(itemKey);
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
    public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        restoreCooldowns(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        restoreCooldowns(event.player);
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

    private static NBTTagCompound getOrCreatePersistedTag(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }
}
