package com.mahghuuuls.foodtimer.handler;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.network.FoodTimerPacketHandler;
import com.mahghuuuls.foodtimer.network.SPacketCooldownPolicySnapshot;
import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.Objects;

/**
 * Publishes the complete gameplay policy whenever a server establishes a new client world state.
 */
public final class PolicySnapshotSyncHandler {

    private static final AtomicBoolean SIZE_WARNING_EMITTED = new AtomicBoolean();
    private final Consumer<EntityPlayer> snapshotSender;

    public PolicySnapshotSyncHandler() {
        this(PolicySnapshotSyncHandler::sendSnapshot);
    }

    PolicySnapshotSyncHandler(Consumer<EntityPlayer> snapshotSender) {
        this.snapshotSender = Objects.requireNonNull(snapshotSender, "snapshotSender");
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        snapshotSender.accept(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        snapshotSender.accept(event.player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        snapshotSender.accept(event.player);
    }

    static void sendSnapshot(EntityPlayer player) {
        if (!(player instanceof EntityPlayerMP) || player.getEntityWorld().isRemote) {
            return;
        }

        CooldownConfigSnapshot snapshot = ModConfig.getGameplaySnapshot();
        try {
            SPacketCooldownPolicySnapshot message = new SPacketCooldownPolicySnapshot(snapshot);
            FoodTimerPacketHandler.INSTANCE.sendTo(message, (EntityPlayerMP) player);
            LoggerHelper.debug(
                    "Sent policy snapshot to '{}' (policy={}, overrides={}, bytes={}).",
                    player.getName(),
                    snapshot.getPolicy(),
                    snapshot.getOverrides().size(),
                    message.getEncodedSizeBytes()
            );
        } catch (IllegalArgumentException tooLarge) {
            if (SIZE_WARNING_EMITTED.compareAndSet(false, true)) {
                LoggerHelper.warn(
                        "Food Timer policy snapshot could not be synchronized; client tooltips will remain unavailable: {}",
                        tooLarge.getMessage()
                );
            }
        }
    }
}
