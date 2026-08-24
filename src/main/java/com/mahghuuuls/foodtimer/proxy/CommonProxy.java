package com.mahghuuuls.foodtimer.proxy;

import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.handler.CooldownPersistence;
import com.mahghuuuls.foodtimer.handler.FoodEatingHandler;
import com.mahghuuuls.foodtimer.handler.PolicySnapshotSyncHandler;
import com.mahghuuuls.foodtimer.network.FoodTimerPacketHandler;
import com.mahghuuuls.foodtimer.network.SPacketCooldownPolicySnapshot;
import com.mahghuuuls.foodtimer.network.SPacketFoodCooldown;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import java.io.File;

public class CommonProxy {

    private File suggestedConfigurationFile;

    public void preInit(FMLPreInitializationEvent event) {
        suggestedConfigurationFile = event.getSuggestedConfigurationFile();
        registerInfrastructure();
    }

    protected void registerInfrastructure() {
        FoodTimerPacketHandler.init();
        MinecraftForge.EVENT_BUS.register(new FoodEatingHandler());
        MinecraftForge.EVENT_BUS.register(new CooldownPersistence());
        MinecraftForge.EVENT_BUS.register(new PolicySnapshotSyncHandler());
    }

    public void init(FMLInitializationEvent event) {
        if (suggestedConfigurationFile == null) {
            throw new IllegalStateException("Food Timer configuration path was not captured during pre-initialization");
        }
        loadConfiguration(suggestedConfigurationFile);
    }

    protected void loadConfiguration(File configurationFile) {
        ModConfig.init(configurationFile);
    }

    public void postInit(FMLPostInitializationEvent event) {
    }

    /**
     * Physical-client dispatch seam. The common implementation intentionally does nothing.
     */
    public void handleFoodCooldown(SPacketFoodCooldown message) {
    }

    /**
     * Physical-client dispatch seam. The common implementation intentionally does nothing.
     */
    public void handlePolicySnapshot(SPacketCooldownPolicySnapshot message) {
    }
}
