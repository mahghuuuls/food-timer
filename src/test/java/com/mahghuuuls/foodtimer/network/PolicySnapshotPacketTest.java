package com.mahghuuuls.foodtimer.network;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicySnapshotPacketTest {

    @Test
    void roundTripPreservesEveryPolicyAndPositiveZeroExactWildcardOverrides() {
        List<CooldownRule> overrides = Arrays.asList(
                new CooldownRule(new ResourceLocation("test:apple"), 0, 17),
                new CooldownRule(new ResourceLocation("test:apple"), -1, 0),
                new CooldownRule(new ResourceLocation("test:bread"), -1, CooldownResolver.MAX_DURATION_SECONDS)
        );

        for (CooldownPolicy policy : CooldownPolicy.values()) {
            CooldownConfigSnapshot original = new CooldownConfigSnapshot(policy, 31, 7, overrides);
            SPacketCooldownPolicySnapshot decoded = roundTrip(original);

            assertTrue(decoded.isValid());
            assertEquals(policy, decoded.getSnapshot().getPolicy());
            assertEquals(31, decoded.getSnapshot().getFixedFoodCooldownSeconds());
            assertEquals(7, decoded.getSnapshot().getSecondsPerHungerPoint());
            assertEquals(overrides, decoded.getSnapshot().getOverrides());
        }
    }

    @Test
    void malformedAndUnsupportedPayloadsNeverPublishPartialSnapshots() {
        List<ByteBuf> malformed = new ArrayList<>();
        malformed.add(bufferWithInts(999));
        malformed.add(bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.CONFIGURED_ONLY.ordinal(),
                30,
                5,
                -1
        ));
        malformed.add(bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.values().length,
                30,
                5,
                0
        ));
        malformed.add(bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.CONFIGURED_ONLY.ordinal(),
                0,
                5,
                0
        ));
        malformed.add(bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.CONFIGURED_ONLY.ordinal(),
                30,
                5,
                SPacketCooldownPolicySnapshot.MAX_OVERRIDES + 1
        ));

        ByteBuf invalidIdentifier = bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.CONFIGURED_ONLY.ordinal(),
                30,
                5,
                1
        );
        ByteBufUtils.writeUTF8String(invalidIdentifier, "test:bad path");
        invalidIdentifier.writeInt(-1);
        invalidIdentifier.writeInt(10);
        malformed.add(invalidIdentifier);

        ByteBuf invalidMetadata = validHeaderWithOneOverride();
        ByteBufUtils.writeUTF8String(invalidMetadata, "test:apple");
        invalidMetadata.writeInt(-2);
        invalidMetadata.writeInt(10);
        malformed.add(invalidMetadata);

        ByteBuf invalidDuration = validHeaderWithOneOverride();
        ByteBufUtils.writeUTF8String(invalidDuration, "test:apple");
        invalidDuration.writeInt(-1);
        invalidDuration.writeInt(CooldownResolver.MAX_DURATION_SECONDS + 1);
        malformed.add(invalidDuration);

        ByteBuf duplicateScope = bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.CONFIGURED_ONLY.ordinal(),
                30,
                5,
                2
        );
        for (int index = 0; index < 2; index++) {
            ByteBufUtils.writeUTF8String(duplicateScope, "test:apple");
            duplicateScope.writeInt(0);
            duplicateScope.writeInt(10 + index);
        }
        malformed.add(duplicateScope);

        ByteBuf truncated = bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.CONFIGURED_ONLY.ordinal()
        );
        malformed.add(truncated);

        ByteBuf trailingData = bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.CONFIGURED_ONLY.ordinal(),
                30,
                5,
                0,
                1234
        );
        malformed.add(trailingData);

        for (int malformedIndex = 0; malformedIndex < malformed.size(); malformedIndex++) {
            ByteBuf payload = malformed.get(malformedIndex);
            try {
                SPacketCooldownPolicySnapshot decoded = new SPacketCooldownPolicySnapshot();
                decoded.fromBytes(payload);
                assertFalse(decoded.isValid(), "malformed payload index " + malformedIndex);
                assertNull(decoded.getSnapshot());
                assertNotNull(decoded.getValidationError());
            } finally {
                payload.release();
            }
        }
    }

    @Test
    void representativeLargeSnapshotFitsOnePartEnvelopeWithWideMargin() {
        int representativeOverrides = 10_000;
        List<CooldownRule> overrides = new ArrayList<>(representativeOverrides);
        for (int index = 0; index < representativeOverrides; index++) {
            overrides.add(new CooldownRule(new ResourceLocation("example:item_" + index), -1, 5));
        }

        SPacketCooldownPolicySnapshot message = new SPacketCooldownPolicySnapshot(
                new CooldownConfigSnapshot(CooldownPolicy.SCALED_ALL_FOODS, 30, 5, overrides)
        );

        assertTrue(
                SPacketCooldownPolicySnapshot.MAX_ENCODED_BYTES
                        + FoodTimerPacketHandler.SIMPLE_IMPL_DISCRIMINATOR_BYTES
                        < SPacketCooldownPolicySnapshot.FORGE_ONE_PART_PAYLOAD_LIMIT
        );
        assertTrue(message.getEncodedSizeBytes() < SPacketCooldownPolicySnapshot.MAX_ENCODED_BYTES / 2);
        System.out.println(
                "Food Timer policy snapshot measurement: overrides=" + representativeOverrides
                        + ", encodedBytes=" + message.getEncodedSizeBytes()
                        + ", acceptedLimit=" + SPacketCooldownPolicySnapshot.MAX_ENCODED_BYTES
                        + ", forgeOnePartLimit=" + SPacketCooldownPolicySnapshot.FORGE_ONE_PART_PAYLOAD_LIMIT
        );
    }

    @Test
    void senderRefusesCompleteSnapshotsThatExceedAcceptedOnePartLimit() {
        List<CooldownRule> overrides = new ArrayList<>();
        String longPrefix = "example:" + repeat('x', 230);
        for (int index = 0; index < 5_000; index++) {
            overrides.add(new CooldownRule(new ResourceLocation(longPrefix + index), -1, 5));
        }
        CooldownConfigSnapshot snapshot = new CooldownConfigSnapshot(
                CooldownPolicy.FIXED_ALL_FOODS,
                30,
                5,
                overrides
        );

        assertThrows(IllegalArgumentException.class, () -> new SPacketCooldownPolicySnapshot(snapshot));
    }

    @Test
    void activeCooldownPacketRoundTripIsDefensiveAndMetadataAware() {
        SPacketFoodCooldown original = new SPacketFoodCooldown("example:123", 7, 40, 120L, 100L);
        ByteBuf encoded = Unpooled.buffer();
        try {
            original.toBytes(encoded);
            SPacketFoodCooldown decoded = new SPacketFoodCooldown();
            decoded.fromBytes(encoded);
            assertTrue(decoded.isValid());
            assertEquals("example:123", decoded.getItemKey());
            assertEquals(7, decoded.getMetadata());
            assertEquals(40, decoded.getDurationTicks());
        } finally {
            encoded.release();
        }

        ByteBuf malformed = Unpooled.buffer();
        try {
            ByteBufUtils.writeUTF8String(malformed, "example:food");
            malformed.writeInt(-2);
            malformed.writeInt(40);
            malformed.writeLong(120L);
            malformed.writeLong(100L);
            SPacketFoodCooldown decoded = new SPacketFoodCooldown();
            decoded.fromBytes(malformed);
            assertFalse(decoded.isValid());
        } finally {
            malformed.release();
        }
    }

    private static SPacketCooldownPolicySnapshot roundTrip(CooldownConfigSnapshot snapshot) {
        SPacketCooldownPolicySnapshot original = new SPacketCooldownPolicySnapshot(snapshot);
        ByteBuf encoded = Unpooled.buffer();
        try {
            original.toBytes(encoded);
            SPacketCooldownPolicySnapshot decoded = new SPacketCooldownPolicySnapshot();
            decoded.fromBytes(encoded);
            return decoded;
        } finally {
            encoded.release();
        }
    }

    private static ByteBuf bufferWithInts(int... values) {
        ByteBuf buffer = Unpooled.buffer();
        for (int value : values) {
            buffer.writeInt(value);
        }
        return buffer;
    }

    private static ByteBuf validHeaderWithOneOverride() {
        return bufferWithInts(
                SPacketCooldownPolicySnapshot.PROTOCOL_VERSION,
                CooldownPolicy.CONFIGURED_ONLY.ordinal(),
                30,
                5,
                1
        );
    }

    private static String repeat(char value, int count) {
        char[] characters = new char[count];
        Arrays.fill(characters, value);
        return new String(characters);
    }
}
