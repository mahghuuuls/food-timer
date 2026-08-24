package com.mahghuuuls.foodtimer.handler;

import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.policy.CooldownDecision;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Common event handler listening to food and item consumption lifecycle.
 */
public class FoodEatingHandler {

    @SubscribeEvent
    public void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (!event.getWorld().isRemote) {
            if (CooldownPersistence.hasActiveCooldown(event.getEntityPlayer(), event.getItemStack())) {
                event.setCancellationResult(net.minecraft.util.EnumActionResult.FAIL);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;
        ItemStack itemStack = event.getItem();
        if (itemStack.isEmpty()) {
            return;
        }

        if (CooldownPersistence.hasActiveCooldown(player, itemStack)) {
            event.setCanceled(true);
            player.resetActiveHand();
            LoggerHelper.debug("Blocked item use start for '{}' meta {} on player '{}' (cooldown active)",
                    itemStack.getItem().getRegistryName(), itemStack.getMetadata(), player.getName());
        }
    }

    @SubscribeEvent
    public void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer) entity;

        // Only enforce on logical server
        if (player.getEntityWorld().isRemote) {
            return;
        }

        ItemStack itemStack = event.getItem();
        if (itemStack.isEmpty()) {
            return;
        }

        CooldownDecision decision = CooldownResolver.resolve(ModConfig.getGameplaySnapshot(), itemStack);
        if (decision.hasCooldown()) {
            CooldownPersistence.saveCooldown(player, decision);
        }

        String persistenceScope = decision.hasCooldown() || decision.isExcluded()
                ? Integer.toString(decision.getMetadata())
                : "none";
        LoggerHelper.debug(
                "Resolved completed use for player '{}' item '{}' meta {}: outcome={}, source={}, duration={}s, scopeMeta={}",
                player.getName(),
                itemStack.getItem().getRegistryName(),
                itemStack.getMetadata(),
                decision.getOutcome(),
                decision.getSource(),
                decision.getDurationSeconds(),
                persistenceScope
        );
    }
}
