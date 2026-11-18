package com.realgore;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RealGore.MODID)
public class RealGore {
    public static final String MODID = "realgore";

    public RealGore() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.ENTITIES.register(modBus);
        ModRegistry.PARTICLES.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, GoreConfig.SPEC);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(GoreNetwork::register);
    }
}
