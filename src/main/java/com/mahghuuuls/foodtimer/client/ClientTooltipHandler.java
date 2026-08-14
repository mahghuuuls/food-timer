package com.mahghuuuls.foodtimer.client;

import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.config.RuleRegistry;
import com.mahghuuuls.foodtimer.util.TimeFormatter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Client-only event handler for appending cooldown duration information to item tooltips.
 */
public class ClientTooltipHandler {

    @SubscribeEvent
    public void onItemTooltip(ItemTooltipEvent event) {
        if (!ModConfig.enableTooltips) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }

        CooldownRule rule = RuleRegistry.findRule(stack);
        if (rule == null) {
            return;
        }

        String formattedTime = TimeFormatter.format(rule.getDurationSeconds());
        String tooltipLine = TextFormatting.GRAY + ModConfig.tooltipPrefix + TextFormatting.GOLD + formattedTime;
        event.getToolTip().add(tooltipLine);
    }
}
