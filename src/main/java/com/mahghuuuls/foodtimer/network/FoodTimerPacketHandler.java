package com.mahghuuuls.foodtimer.network;

import com.mahghuuuls.foodtimer.Tags;
import com.mahghuuuls.foodtimer.client.ClientCooldownTracker;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FoodTimerPacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    private static int packetId = 0;

    public static void init() {
        INSTANCE.registerMessage(Handler.class, SPacketFoodCooldown.class, packetId++, Side.CLIENT);
    }

    public static class Handler implements IMessageHandler<SPacketFoodCooldown, IMessage> {

        @Override
        public IMessage onMessage(SPacketFoodCooldown message, MessageContext ctx) {
            handleClientMessage(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void handleClientMessage(SPacketFoodCooldown message) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                long remaining = message.getExpireWorldTime() - message.getCurrentWorldTime();
                ClientCooldownTracker.updateCooldown(
                        message.getItemKey(),
                        message.getMetadata(),
                        message.getDurationTicks(),
                        (int) remaining
                );
            });
        }
    }
}
