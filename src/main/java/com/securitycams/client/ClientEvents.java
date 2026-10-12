package com.securitycams.client;

import com.securitycams.SecurityCamsMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SecurityCamsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /** Hide the first-person hand while watching a feed. */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (CameraViewHandler.active) {
            event.setCanceled(true);
        }
    }

    /** Hide the vanilla HUD (hotbar, crosshair, health...) while watching a feed. */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (CameraViewHandler.active) {
            event.setCanceled(true);
        }
    }

    /** Scroll-wheel zoom. */
    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (CameraViewHandler.active) {
            event.setFOV(event.getFOV() * CameraViewHandler.fovScale);
        }
    }
}
