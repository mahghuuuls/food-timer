package com.mahghuuuls.foodtimer.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

public class SPacketFoodCooldown implements IMessage {

    private String itemKey;
    private int metadata;
    private int durationTicks;
    private long expireWorldTime;
    private long currentWorldTime;
    private boolean valid;
    private String validationError;

    public SPacketFoodCooldown() {
    }

    public SPacketFoodCooldown(String itemKey, int metadata, int durationTicks, long expireWorldTime, long currentWorldTime) {
        this.itemKey = itemKey;
        this.metadata = metadata;
        this.durationTicks = durationTicks;
        this.expireWorldTime = expireWorldTime;
        this.currentWorldTime = currentWorldTime;
        validate();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        valid = false;
        validationError = null;
        try {
            this.itemKey = ByteBufUtils.readUTF8String(buf);
            this.metadata = buf.readInt();
            this.durationTicks = buf.readInt();
            this.expireWorldTime = buf.readLong();
            this.currentWorldTime = buf.readLong();
            if (buf.isReadable()) {
                throw new IllegalArgumentException("trailing active-cooldown data");
            }
            validate();
        } catch (RuntimeException malformed) {
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
        if (!valid) {
            throw new IllegalStateException("Cannot encode an invalid active cooldown");
        }
        ByteBufUtils.writeUTF8String(buf, itemKey);
        buf.writeInt(metadata);
        buf.writeInt(durationTicks);
        buf.writeLong(expireWorldTime);
        buf.writeLong(currentWorldTime);
    }

    public String getItemKey() {
        return itemKey;
    }

    public int getMetadata() {
        return metadata;
    }

    public int getDurationTicks() {
        return durationTicks;
    }

    public long getExpireWorldTime() {
        return expireWorldTime;
    }

    public long getCurrentWorldTime() {
        return currentWorldTime;
    }

    public boolean isValid() {
        return valid;
    }

    public String getValidationError() {
        return validationError;
    }

    private void validate() {
        if (!NetworkValidation.isCanonicalRegistryName(itemKey)) {
            throw new IllegalArgumentException("non-canonical item key");
        }
        if (metadata < -1) {
            throw new IllegalArgumentException("invalid metadata");
        }
        if (durationTicks <= 0) {
            throw new IllegalArgumentException("invalid duration");
        }
        if (expireWorldTime <= currentWorldTime) {
            throw new IllegalArgumentException("expired active cooldown");
        }
        valid = true;
    }
}
