package com.mahghuuuls.foodtimer.handler;

import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.RuleRegistry;
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

        CooldownRule rule = RuleRegistry.findRule(itemStack);
        if (rule == null) {
            return;
        }

        int durationTicks = rule.getDurationTicks();
        if (durationTicks > 0) {
            CooldownPersistence.saveCooldown(player, rule, durationTicks);
            LoggerHelper.debug("Set cooldown of {} ticks ({}s) for item '{}' meta {} on player '{}'",
                    durationTicks, rule.getDurationSeconds(), rule.getRegistryName(),
                    rule.isWildcard() ? "*" : rule.getMetadata(), player.getName());
        }
    }
}
