package com.mahghuuuls.foodtimer.proxy;

import com.mahghuuuls.foodtimer.config.ModConfig;
import com.mahghuuuls.foodtimer.handler.CooldownPersistence;
import com.mahghuuuls.foodtimer.handler.FoodEatingHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
        MinecraftForge.EVENT_BUS.register(new FoodEatingHandler());
        MinecraftForge.EVENT_BUS.register(new CooldownPersistence());
    }

    public void init(FMLInitializationEvent event) {
    }

    public void postInit(FMLPostInitializationEvent event) {
    }
}
