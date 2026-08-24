package com.mahghuuuls.foodtimer.proxy;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommonProxyLifecycleTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    public void configurationLoadsOnlyDuringInitializationAfterPreInitCapture() {
        File expectedConfig = temporaryDirectory.resolve("foodtimer.cfg").toFile();
        RecordingProxy proxy = new RecordingProxy();
        FMLPreInitializationEvent preInit = new FMLPreInitializationEvent(null, temporaryDirectory.toFile()) {
            @Override
            public File getSuggestedConfigurationFile() {
                return expectedConfig;
            }
        };

        proxy.preInit(preInit);

        assertEquals(1, proxy.infrastructureRegistrations);
        assertEquals(0, proxy.configurationLoads);

        proxy.init(new FMLInitializationEvent());

        assertEquals(1, proxy.configurationLoads);
        assertEquals(expectedConfig, proxy.loadedFile);
    }

    private static final class RecordingProxy extends CommonProxy {
        int infrastructureRegistrations;
        int configurationLoads;
        File loadedFile;

        @Override
        protected void registerInfrastructure() {
            infrastructureRegistrations++;
        }

        @Override
        protected void loadConfiguration(File configurationFile) {
            configurationLoads++;
            loadedFile = configurationFile;
        }
    }
}
