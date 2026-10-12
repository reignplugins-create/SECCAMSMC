package com.securitycams.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientHooks {
    private ClientHooks() {}

    public static void openTablet(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new TabletScreen(hand));
    }
}
