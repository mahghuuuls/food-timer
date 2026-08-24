package com.mahghuuuls.foodtimer.client;

import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

/**
 * Clears connection-owned policy and active presentation state at every client boundary.
 */
public final class ClientConnectionStateHandler {

    @SubscribeEvent
    public void onConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        clear("connect");
    }

    @SubscribeEvent
    public void onDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        clear("disconnect");
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // Dimension changes also unload a client world but retain the connection and its new snapshot.
        if (shouldClearOnWorldUnload(
                event.getWorld().isRemote,
                Minecraft.getMinecraft().getConnection() != null
        )) {
            clear("world unload");
        }
    }

    private static void clear(String reason) {
        long generation = ClientConnectionGeneration.advance();
        Minecraft.getMinecraft().addScheduledTask(() -> ClientConnectionGeneration.runIfCurrent(generation, () -> {
            ClientPolicyState.clear();
            ClientCooldownTracker.clear();
            LoggerHelper.debug("Cleared client connection state on {} (generation={}).", reason, generation);
        }));
    }

    static boolean shouldClearOnWorldUnload(boolean remoteWorld, boolean connectionPresent) {
        return remoteWorld && !connectionPresent;
    }
}
