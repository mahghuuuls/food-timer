package com.mahghuuuls.foodtimer.network;

import com.mahghuuuls.foodtimer.handler.PolicySnapshotSyncHandler;
import com.mahghuuuls.foodtimer.proxy.CommonProxy;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommonNetworkSideSafetyTest {

    @Test
    void commonNetworkClassesContainNoPhysicalClientReferences() throws IOException {
        assertEquals(0, FoodTimerPacketHandler.ACTIVE_COOLDOWN_DISCRIMINATOR);
        assertEquals(1, FoodTimerPacketHandler.POLICY_SNAPSHOT_DISCRIMINATOR);
        List<Class<?>> commonClasses = Arrays.asList(
                FoodTimerPacketHandler.class,
                FoodTimerPacketHandler.ActiveCooldownHandler.class,
                FoodTimerPacketHandler.PolicySnapshotHandler.class,
                SPacketFoodCooldown.class,
                SPacketCooldownPolicySnapshot.class,
                NetworkValidation.class,
                PolicySnapshotSyncHandler.class,
                CommonProxy.class
        );

        for (Class<?> commonClass : commonClasses) {
            String constants = new String(readClassBytes(commonClass), StandardCharsets.ISO_8859_1);
            assertFalse(constants.contains("net/minecraft/client/"), commonClass.getName());
            assertFalse(constants.contains("com/mahghuuuls/foodtimer/client/"), commonClass.getName());
        }
    }

    private static byte[] readClassBytes(Class<?> type) throws IOException {
        String resourceName = '/' + type.getName().replace('.', '/') + ".class";
        InputStream input = type.getResourceAsStream(resourceName);
        assertNotNull(input, resourceName);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }
}
