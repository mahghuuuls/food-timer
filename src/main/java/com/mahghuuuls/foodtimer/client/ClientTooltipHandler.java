package com.mahghuuuls.foodtimer.client;

import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.policy.CooldownDecision;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
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

        CooldownDecision decision = resolveAuthoritativeDecision(stack);
        if (!decision.hasCooldown()) {
            return;
        }

        String formattedTime = TimeFormatter.format(decision.getDurationSeconds());
        String tooltipLine = TextFormatting.GRAY + ModConfig.tooltipPrefix + TextFormatting.GOLD + formattedTime;
        event.getToolTip().add(tooltipLine);
    }

    static CooldownDecision resolveAuthoritativeDecision(ItemStack stack) {
        CooldownConfigSnapshot snapshot = ClientPolicyState.getSnapshot();
        return snapshot == null ? CooldownDecision.none() : CooldownResolver.resolve(snapshot, stack);
    }
}
