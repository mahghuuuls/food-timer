package com.mahghuuuls.foodtimer.network;

import com.mahghuuuls.foodtimer.config.CooldownConfigSnapshot;
import com.mahghuuuls.foodtimer.config.CooldownRule;
import com.mahghuuuls.foodtimer.policy.CooldownPolicy;
import com.mahghuuuls.foodtimer.policy.CooldownResolver;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Complete server-authored gameplay policy for one client connection.
 */
public final class SPacketCooldownPolicySnapshot implements IMessage {

    public static final int PROTOCOL_VERSION = 1;
    public static final int MAX_OVERRIDES = 32768;
    public static final int MAX_REGISTRY_NAME_BYTES = 256;
    public static final int FORGE_ONE_PART_PAYLOAD_LIMIT = 0x100000 - 0x50;

    /**
     * Leaves practical margin below Forge 1.12.2's inspected one-part FML proxy threshold.
     */
    public static final int MAX_ENCODED_BYTES = 1_000_000;

    private CooldownConfigSnapshot snapshot;
    private boolean valid;
    private String validationError;
    private int encodedSizeBytes;

    public SPacketCooldownPolicySnapshot() {
    }

    public SPacketCooldownPolicySnapshot(CooldownConfigSnapshot snapshot) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        validateSnapshot(snapshot);
        this.encodedSizeBytes = measureEncodedSize(snapshot);
        if (encodedSizeBytes > MAX_ENCODED_BYTES
                || encodedSizeBytes + FoodTimerPacketHandler.SIMPLE_IMPL_DISCRIMINATOR_BYTES
                >= FORGE_ONE_PART_PAYLOAD_LIMIT) {
            throw new IllegalArgumentException(
                    "Encoded policy snapshot exceeds " + MAX_ENCODED_BYTES + " bytes: " + encodedSizeBytes
            );
        }
        this.valid = true;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        snapshot = null;
        valid = false;
        validationError = null;
        encodedSizeBytes = buf.readableBytes();

        if (encodedSizeBytes > MAX_ENCODED_BYTES) {
            invalidateAndConsume(buf, "snapshot payload exceeds the accepted size limit");
            return;
        }

        try {
            requireReadable(buf, 4);
            int protocol = buf.readInt();
            if (protocol != PROTOCOL_VERSION) {
                invalidateAndConsume(buf, "unsupported protocol " + protocol);
                return;
            }

            requireReadable(buf, 16);
            int policyOrdinal = buf.readInt();
            int fixedSeconds = buf.readInt();
            int secondsPerHungerPoint = buf.readInt();
            int overrideCount = buf.readInt();

            CooldownPolicy[] policies = CooldownPolicy.values();
            if (policyOrdinal < 0 || policyOrdinal >= policies.length) {
                throw new IllegalArgumentException("invalid policy value");
            }
            validatePositiveSeconds(fixedSeconds, "fixed cooldown");
            validatePositiveSeconds(secondsPerHungerPoint, "scaled cooldown");
            if (overrideCount < 0 || overrideCount > MAX_OVERRIDES) {
                throw new IllegalArgumentException("invalid override count");
            }

            List<CooldownRule> overrides = new ArrayList<>(overrideCount);
            for (int index = 0; index < overrideCount; index++) {
                String itemKey = readRegistryName(buf);
                requireReadable(buf, 8);
                int metadata = buf.readInt();
                int durationSeconds = buf.readInt();
                if (metadata < CooldownRule.WILDCARD_META) {
                    throw new IllegalArgumentException("invalid override metadata");
                }
                if (durationSeconds < 0 || durationSeconds > CooldownResolver.MAX_DURATION_SECONDS) {
                    throw new IllegalArgumentException("invalid override duration");
                }
                overrides.add(new CooldownRule(new ResourceLocation(itemKey), metadata, durationSeconds));
            }

            if (buf.isReadable()) {
                throw new IllegalArgumentException("trailing snapshot data");
            }

            snapshot = new CooldownConfigSnapshot(
                    policies[policyOrdinal],
                    fixedSeconds,
                    secondsPerHungerPoint,
                    overrides
            );
            valid = true;
        } catch (RuntimeException malformed) {
            snapshot = null;
            valid = false;
            validationError = malformed.getMessage() == null
                    ? malformed.getClass().getSimpleName()
                    : malformed.getMessage();
            if (buf.isReadable()) {
                buf.skipBytes(buf.readableBytes());
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        if (!valid || snapshot == null) {
            throw new IllegalStateException("Cannot encode an invalid policy snapshot");
        }
        writeSnapshot(buf, snapshot);
    }

    public boolean isValid() {
        return valid;
    }

    public CooldownConfigSnapshot getSnapshot() {
        return snapshot;
    }

    public String getValidationError() {
        return validationError;
    }

    public int getEncodedSizeBytes() {
        return encodedSizeBytes;
    }

    private static void writeSnapshot(ByteBuf buf, CooldownConfigSnapshot snapshot) {
        List<CooldownRule> overrides = snapshot.getOverrides();
        buf.writeInt(PROTOCOL_VERSION);
        buf.writeInt(snapshot.getPolicy().ordinal());
        buf.writeInt(snapshot.getFixedFoodCooldownSeconds());
        buf.writeInt(snapshot.getSecondsPerHungerPoint());
        buf.writeInt(overrides.size());
        for (CooldownRule override : overrides) {
            writeRegistryName(buf, override.getRegistryName().toString());
            buf.writeInt(override.getMetadata());
            buf.writeInt(override.getDurationSeconds());
        }
    }

    private static int measureEncodedSize(CooldownConfigSnapshot snapshot) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            writeSnapshot(buffer, snapshot);
            return buffer.readableBytes();
        } finally {
            buffer.release();
        }
    }

    private static void validateSnapshot(CooldownConfigSnapshot snapshot) {
        if (snapshot.getOverrides().size() > MAX_OVERRIDES) {
            throw new IllegalArgumentException("Too many cooldown overrides: " + snapshot.getOverrides().size());
        }
        for (CooldownRule override : snapshot.getOverrides()) {
            byte[] bytes = override.getRegistryName().toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 0 || bytes.length > MAX_REGISTRY_NAME_BYTES) {
                throw new IllegalArgumentException("Registry name is too long for policy synchronization");
            }
        }
    }

    private static void validatePositiveSeconds(int seconds, String field) {
        if (seconds <= 0 || seconds > CooldownResolver.MAX_DURATION_SECONDS) {
            throw new IllegalArgumentException("invalid " + field);
        }
    }

    private static void writeRegistryName(ByteBuf buf, String itemKey) {
        byte[] bytes = itemKey.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0 || bytes.length > MAX_REGISTRY_NAME_BYTES) {
            throw new IllegalArgumentException("Registry name is too long for policy synchronization");
        }
        ByteBufUtils.writeUTF8String(buf, itemKey);
    }

    private static String readRegistryName(ByteBuf buf) {
        int byteLength = ByteBufUtils.readVarInt(buf, 2);
        if (byteLength <= 0 || byteLength > MAX_REGISTRY_NAME_BYTES || buf.readableBytes() < byteLength) {
            throw new IllegalArgumentException("invalid registry-name length");
        }
        String itemKey = buf.toString(buf.readerIndex(), byteLength, StandardCharsets.UTF_8);
        buf.skipBytes(byteLength);
        if (!NetworkValidation.isCanonicalRegistryName(itemKey)) {
            throw new IllegalArgumentException("non-canonical registry name");
        }
        return itemKey;
    }

    private static void requireReadable(ByteBuf buf, int bytes) {
        if (buf.readableBytes() < bytes) {
            throw new IllegalArgumentException("truncated snapshot payload");
        }
    }

    private void invalidateAndConsume(ByteBuf buf, String reason) {
        validationError = reason;
        if (buf.isReadable()) {
            buf.skipBytes(buf.readableBytes());
        }
    }
}
