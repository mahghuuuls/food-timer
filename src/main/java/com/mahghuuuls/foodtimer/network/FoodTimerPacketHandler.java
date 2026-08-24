package com.mahghuuuls.foodtimer.network;

import com.mahghuuuls.foodtimer.Tags;
import com.mahghuuuls.foodtimer.FoodTimerMod;
import com.mahghuuuls.foodtimer.util.LoggerHelper;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.concurrent.atomic.AtomicBoolean;

public class FoodTimerPacketHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    public static final int ACTIVE_COOLDOWN_DISCRIMINATOR = 0;
    public static final int POLICY_SNAPSHOT_DISCRIMINATOR = 1;
    public static final int SIMPLE_IMPL_DISCRIMINATOR_BYTES = 1;

    private static final AtomicBoolean INVALID_ACTIVE_WARNING = new AtomicBoolean();
    private static final AtomicBoolean INVALID_POLICY_WARNING = new AtomicBoolean();
    private static boolean initialized;

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        INSTANCE.registerMessage(
                ActiveCooldownHandler.class,
                SPacketFoodCooldown.class,
                ACTIVE_COOLDOWN_DISCRIMINATOR,
                Side.CLIENT
        );
        INSTANCE.registerMessage(
                PolicySnapshotHandler.class,
                SPacketCooldownPolicySnapshot.class,
                POLICY_SNAPSHOT_DISCRIMINATOR,
                Side.CLIENT
        );
        initialized = true;
    }

    public static final class ActiveCooldownHandler implements IMessageHandler<SPacketFoodCooldown, IMessage> {

        @Override
        public IMessage onMessage(SPacketFoodCooldown message, MessageContext ctx) {
            if (message == null || !message.isValid()) {
                warnInvalidOnce(INVALID_ACTIVE_WARNING, "active cooldown", message == null ? null : message.getValidationError());
                return null;
            }
            if (FoodTimerMod.proxy != null) {
                FoodTimerMod.proxy.handleFoodCooldown(message);
            }
            return null;
        }
    }

    public static final class PolicySnapshotHandler
            implements IMessageHandler<SPacketCooldownPolicySnapshot, IMessage> {

        @Override
        public IMessage onMessage(SPacketCooldownPolicySnapshot message, MessageContext ctx) {
            if (message == null || !message.isValid()) {
                warnInvalidOnce(INVALID_POLICY_WARNING, "policy snapshot", message == null ? null : message.getValidationError());
            }
            if (FoodTimerMod.proxy != null) {
                FoodTimerMod.proxy.handlePolicySnapshot(message);
            }
            return null;
        }
    }

    private static void warnInvalidOnce(AtomicBoolean warning, String packetType, String reason) {
        if (warning.compareAndSet(false, true)) {
            LoggerHelper.warn(
                    "Ignored invalid Food Timer {} packet{}.",
                    packetType,
                    reason == null ? "" : ": " + reason
            );
        }
    }
}
