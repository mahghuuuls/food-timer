package com.mahghuuuls.foodtimer.proxy;

import com.mahghuuuls.foodtimer.client.ClientCooldownRenderer;
import com.mahghuuuls.foodtimer.client.ClientCooldownTracker;
import com.mahghuuuls.foodtimer.client.ClientConnectionStateHandler;
import com.mahghuuuls.foodtimer.client.ClientConnectionGeneration;
import com.mahghuuuls.foodtimer.client.ClientPolicyState;
import com.mahghuuuls.foodtimer.client.ClientTooltipHandler;
import com.mahghuuuls.foodtimer.network.SPacketCooldownPolicySnapshot;
import com.mahghuuuls.foodtimer.network.SPacketFoodCooldown;
import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        MinecraftForge.EVENT_BUS.register(new ClientTooltipHandler());
        MinecraftForge.EVENT_BUS.register(new ClientCooldownRenderer());
        MinecraftForge.EVENT_BUS.register(new ClientConnectionStateHandler());
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);
    }

    @Override
    public void handleFoodCooldown(SPacketFoodCooldown message) {
        long generation = ClientConnectionGeneration.current();
        Minecraft.getMinecraft().addScheduledTask(() -> ClientConnectionGeneration.runIfCurrent(generation, () -> {
            long remaining = message.getExpireWorldTime() - message.getCurrentWorldTime();
            int remainingTicks = remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
            ClientCooldownTracker.updateCooldown(
                    message.getItemKey(),
                    message.getMetadata(),
                    message.getDurationTicks(),
                    remainingTicks
            );
        }));
    }

    @Override
    public void handlePolicySnapshot(SPacketCooldownPolicySnapshot message) {
        long generation = ClientConnectionGeneration.current();
        Minecraft.getMinecraft().addScheduledTask(() -> ClientConnectionGeneration.runIfCurrent(generation, () -> {
            if (message != null && message.isValid()) {
                ClientPolicyState.apply(message);
                LoggerHelper.debug(
                        "Installed authoritative policy snapshot (policy={}, overrides={}, bytes={}).",
                        message.getSnapshot().getPolicy(),
                        message.getSnapshot().getOverrides().size(),
                        message.getEncodedSizeBytes()
                );
            } else {
                ClientPolicyState.apply(message);
            }
        }));
    }
}
