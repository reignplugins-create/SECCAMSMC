package com.securitycams;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SecurityCamsMod.MODID)
public class SecurityCamsMod {
    public static final String MODID = "securitycams";

    public SecurityCamsMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        Registration.BLOCKS.register(bus);
        Registration.ITEMS.register(bus);
        Registration.BLOCK_ENTITIES.register(bus);
    }
}
