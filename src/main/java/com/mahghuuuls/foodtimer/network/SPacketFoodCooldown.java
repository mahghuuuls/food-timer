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

    public SPacketFoodCooldown() {
    }

    public SPacketFoodCooldown(String itemKey, int metadata, int durationTicks, long expireWorldTime, long currentWorldTime) {
        this.itemKey = itemKey;
        this.metadata = metadata;
        this.durationTicks = durationTicks;
        this.expireWorldTime = expireWorldTime;
        this.currentWorldTime = currentWorldTime;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.itemKey = ByteBufUtils.readUTF8String(buf);
        this.metadata = buf.readInt();
        this.durationTicks = buf.readInt();
        this.expireWorldTime = buf.readLong();
        this.currentWorldTime = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
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
}
