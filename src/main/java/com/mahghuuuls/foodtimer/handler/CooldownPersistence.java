package com.mahghuuuls.foodtimer.handler;

import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.RuleRegistry;
import com.mahghuuuls.foodtimer.network.FoodTimerPacketHandler;
import com.mahghuuuls.foodtimer.network.SPacketFoodCooldown;
import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages server-side persistence and network synchronization of metadata-aware food cooldowns.
 */
public class CooldownPersistence {

    public static final String NBT_KEY_ROOT = "FoodTimerCooldowns";

    /**
     * Saves an active cooldown for a specific food rule on the player and sends the sync packet.
     */
    public static void saveCooldown(EntityPlayer player, CooldownRule rule, int durationTicks) {
        if (player == null || player.getEntityWorld().isRemote || rule == null || durationTicks <= 0) {
            return;
        }

        long currentWorldTime = player.getEntityWorld().getTotalWorldTime();
        long expireWorldTime = currentWorldTime + durationTicks;

        String key = formatKey(rule.getRegistryName().toString(), rule.isWildcard() ? -1 : rule.getMetadata());

        NBTTagCompound persisted = getOrCreatePersistedTag(player);
        NBTTagCompound foodCooldowns = persisted.getCompoundTag(NBT_KEY_ROOT);
        foodCooldowns.setLong(key, expireWorldTime);
        persisted.setTag(NBT_KEY_ROOT, foodCooldowns);

        if (player instanceof EntityPlayerMP) {
            FoodTimerPacketHandler.INSTANCE.sendTo(
                    new SPacketFoodCooldown(
                            rule.getRegistryName().toString(),
                            rule.isWildcard() ? -1 : rule.getMetadata(),
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

        CooldownRule rule = RuleRegistry.findRule(stack);
        if (rule == null) {
            return false;
        }

        NBTTagCompound persisted = getOrCreatePersistedTag(player);
        if (!persisted.hasKey(NBT_KEY_ROOT)) {
            return false;
        }

        NBTTagCompound foodCooldowns = persisted.getCompoundTag(NBT_KEY_ROOT);
        long currentWorldTime = player.getEntityWorld().getTotalWorldTime();

        ResourceLocation reg = stack.getItem().getRegistryName();
        if (reg == null) {
            return false;
        }

        String itemKey = reg.toString();
        int meta = stack.getMetadata();

        // Check exact key first
        String exactKey = formatKey(itemKey, meta);
        if (foodCooldowns.hasKey(exactKey)) {
            long expire = foodCooldowns.getLong(exactKey);
            if (expire > currentWorldTime) {
                return true;
            }
        }

        // Check wildcard key
        String wildcardKey = formatKey(itemKey, -1);
        if (foodCooldowns.hasKey(wildcardKey)) {
            long expire = foodCooldowns.getLong(wildcardKey);
            if (expire > currentWorldTime) {
                return true;
            }
        }

        // Check legacy un-suffixed key for backwards compatibility
        if (foodCooldowns.hasKey(itemKey)) {
            long expire = foodCooldowns.getLong(itemKey);
            return expire > currentWorldTime;
        }

        return false;
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
                ParsedKey parsed = parseKey(entryKey);
                if (player instanceof EntityPlayerMP) {
                    FoodTimerPacketHandler.INSTANCE.sendTo(
                            new SPacketFoodCooldown(
                                    parsed.itemKey,
                                    parsed.metadata,
                                    (int) remainingTicks,
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

    private static String formatKey(String itemKey, int metadata) {
        return itemKey + ":" + metadata;
    }

    private static class ParsedKey {
        final String itemKey;
        final int metadata;

        ParsedKey(String itemKey, int metadata) {
            this.itemKey = itemKey;
            this.metadata = metadata;
        }
    }

    private static ParsedKey parseKey(String key) {
        int lastColon = key.lastIndexOf(':');
        if (lastColon > 0) {
            String suffix = key.substring(lastColon + 1);
            try {
                int meta = Integer.parseInt(suffix);
                String itemKey = key.substring(0, lastColon);
                return new ParsedKey(itemKey, meta);
            } catch (NumberFormatException ignored) {
            }
        }
        return new ParsedKey(key, -1);
    }

    private static NBTTagCompound getOrCreatePersistedTag(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }
}
