package com.securitycams.client;

import com.securitycams.block.CameraBlock;
import com.securitycams.item.TabletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client-side state shared between the view screen and the render event hooks. */
@OnlyIn(Dist.CLIENT)
public final class CameraViewHandler {
    /** True while a camera feed is on screen. */
    public static boolean active = false;
    /** Multiplier applied to the field of view (lower = more zoom). */
    public static double fovScale = 1.0;

    private static boolean addedNightVision = false;

    private CameraViewHandler() {}

    public static void begin() {
        active = true;
        fovScale = 1.0;
        Minecraft.getInstance().gameRenderer.setRenderBlockOutline(false);
    }

    public static void end() {
        active = false;
        fovScale = 1.0;
        setNightVision(false);
        Minecraft.getInstance().gameRenderer.setRenderBlockOutline(true);
    }

    /**
     * Night vision is purely visual: the light texture checks the local player for the effect, so a
     * client-only effect brightens the feed without the server ever knowing about it.
     * If the player already has a real night-vision effect we leave it alone.
     */
    public static void setNightVision(boolean on) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            addedNightVision = false;
            return;
        }
        if (on) {
            if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
                addedNightVision = true;
            }
        } else if (addedNightVision) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            addedNightVision = false;
        }
    }

    /** A camera is "online" if it is in your dimension, its chunk is loaded on your client, and the block exists. */
    public static boolean isOnline(TabletItem.CameraLink link) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !level.dimension().location().toString().equals(link.dim())) {
            return false;
        }
        return level.hasChunkAt(link.pos()) && level.getBlockState(link.pos()).getBlock() instanceof CameraBlock;
    }
}
