package com.securitycams.client;

import java.util.List;
import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.securitycams.block.CameraBlock;
import com.securitycams.item.TabletItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

/**
 * Live camera feed. The game's render camera is moved to the camera block (a throwaway client-side
 * Marker entity is used as the "camera entity"), so you see the real world from that spot. The screen
 * itself only draws the overlay (REC light, clock, night-vision tint, controls).
 */
@OnlyIn(Dist.CLIENT)
public class CameraViewScreen extends Screen {
    private final Screen parent;
    private final InteractionHand hand;
    private final List<TabletItem.CameraLink> links;
    private int index;

    private boolean started = false;
    private boolean nightVision = false;
    private Marker camera;
    private Button nvButton;

    private boolean vertical;
    private float baseYaw, basePitch, yawOff, pitchOff, curYaw, curPitch;
    private int openTicks;

    public CameraViewScreen(Screen parent, InteractionHand hand, List<TabletItem.CameraLink> links, int index) {
        super(Component.literal("Camera"));
        this.parent = parent;
        this.hand = hand;
        this.links = links;
        this.index = index;
    }

    @Override
    protected void init() {
        Minecraft mc = this.minecraft;
        if (!started) { // init() also runs on window resize; only set up once
            started = true;
            CameraViewHandler.begin();
            camera = new Marker(EntityType.MARKER, mc.level);
            mc.setCameraEntity(camera);
            aim();
        }

        int bw = 82, gap = 4;
        int total = bw * 4 + gap * 3;
        int x = (width - total) / 2;
        int y = height - 26;
        addRenderableWidget(new Button(x, y, bw, 20, Component.literal("< Prev"), b -> cycle(-1)));
        nvButton = addRenderableWidget(new Button(x + (bw + gap), y, bw, 20, nvLabel(), b -> toggleNightVision()));
        addRenderableWidget(new Button(x + (bw + gap) * 2, y, bw, 20, Component.literal("Next >"), b -> cycle(1)));
        addRenderableWidget(new Button(x + (bw + gap) * 3, y, bw, 20, Component.literal("Back"), b -> onClose()));
    }

    private Component nvLabel() {
        return Component.literal("Night Vision: " + (nightVision ? "ON" : "OFF"));
    }

    private void toggleNightVision() {
        nightVision = !nightVision;
        CameraViewHandler.setNightVision(nightVision);
        if (nvButton != null) {
            nvButton.setMessage(nvLabel());
        }
    }

    /** Snap the render camera to the current link and reset panning. */
    private void aim() {
        Minecraft mc = Minecraft.getInstance();
        TabletItem.CameraLink link = links.get(index);

        Direction facing = Direction.NORTH;
        BlockState state = mc.level.getBlockState(link.pos());
        if (state.getBlock() instanceof CameraBlock) {
            facing = state.getValue(CameraBlock.FACING);
        }
        vertical = facing.getAxis() == Direction.Axis.Y;
        if (vertical) {                       // floor / ceiling mounts: free 360 pan
            baseYaw = mc.player.getYRot();
            basePitch = facing == Direction.UP ? -50f : 50f;
        } else {                              // wall mounts: look straight out, slightly down
            baseYaw = facing.toYRot();
            basePitch = 12f;
        }
        yawOff = 0f;
        pitchOff = 0f;
        curYaw = baseYaw;
        curPitch = basePitch;

        Vec3 p = Vec3.atCenterOf(link.pos())
                .add(facing.getStepX() * 0.4, facing.getStepY() * 0.4, facing.getStepZ() * 0.4);
        camera.moveTo(p.x, p.y, p.z, curYaw, curPitch);
        openTicks = 0;
    }

    private void cycle(int dir) {
        for (int i = 1; i <= links.size(); i++) {
            int next = Math.floorMod(index + dir * i, links.size());
            if (next != index && CameraViewHandler.isOnline(links.get(next))) {
                index = next;
                aim();
                return;
            }
        }
    }

    private void pan(float dYaw, float dPitch) {
        float maxYaw = vertical ? 180f : 75f;
        yawOff = Mth.clamp(yawOff + dYaw, -maxYaw, maxYaw);
        pitchOff = Mth.clamp(pitchOff + dPitch, -60f, 60f);
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        // Leave the feed if you get hurt or the tablet leaves your hand.
        if (p == null || p.hurtTime > 0 || !(p.getItemInHand(hand).getItem() instanceof TabletItem)) {
            mc.setScreen(null);
            return;
        }
        openTicks++;
        float targetYaw = baseYaw + yawOff;
        float targetPitch = Mth.clamp(basePitch + pitchOff, -90f, 90f);
        camera.yRotO = camera.getYRot();
        camera.xRotO = camera.getXRot();
        curYaw += (targetYaw - curYaw) * 0.5f;
        curPitch += (targetPitch - curPitch) * 0.5f;
        camera.setYRot(curYaw);
        camera.setXRot(curPitch);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case GLFW.GLFW_KEY_N -> { toggleNightVision(); return true; }
            case GLFW.GLFW_KEY_Q -> { cycle(-1); return true; }
            case GLFW.GLFW_KEY_E -> { cycle(1); return true; }
            case GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT -> { pan(-4f, 0f); return true; }
            case GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT -> { pan(4f, 0f); return true; }
            case GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP -> { pan(0f, -3f); return true; }
            case GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN -> { pan(0f, 3f); return true; }
            default -> { }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (super.mouseDragged(mx, my, button, dx, dy)) {
            return true;
        }
        float k = 0.2f * (float) CameraViewHandler.fovScale;
        pan((float) dx * k, (float) dy * k);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        CameraViewHandler.fovScale = Mth.clamp(CameraViewHandler.fovScale - delta * 0.1, 0.15, 1.0);
        return true;
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        TabletItem.CameraLink link = links.get(index);
        boolean online = CameraViewHandler.isOnline(link);

        if (!online) {
            renderNoSignal(ps);
        } else {
            if (nightVision) {
                fill(ps, 0, 0, width, height, 0x2A00FF55); // green phosphor tint
            }
            fillGradient(ps, 0, 0, width, 44, 0xB0000000, 0x00000000);
            fillGradient(ps, 0, height - 44, width, height, 0x00000000, 0xB0000000);
        }

        // Blinking REC light
        if ((level.getGameTime() / 10) % 2 == 0) {
            fill(ps, 10, 10, 18, 18, 0xFFE02020);
        }
        drawString(ps, font, "REC", 22, 10, 0xFFFFFFFF);

        // In-world clock
        long t = level.getDayTime();
        String clock = String.format("DAY %d  %02d:%02d", t / 24000L + 1,
                (int) ((t / 1000L + 6L) % 24L), (int) ((t % 1000L) * 60L / 1000L));
        drawString(ps, font, clock, width - 10 - font.width(clock), 10, 0xFFFFFFFF);

        String name = link.name().isEmpty() ? "Camera" : link.name();
        drawString(ps, font, "CAM " + (index + 1) + "/" + links.size() + "  " + name, 10, 24, 0xFFDDDDDD);
        drawString(ps, font, link.pos().getX() + ", " + link.pos().getY() + ", " + link.pos().getZ(),
                10, 36, 0xFF999999);

        if (nightVision) {
            drawCenteredString(ps, font, Component.literal("NIGHT VISION"), width / 2, 10, 0xFF55FF77);
        }
        if (CameraViewHandler.fovScale < 0.99) {
            drawCenteredString(ps, font,
                    Component.literal(String.format("ZOOM x%.1f", 1.0 / CameraViewHandler.fovScale)),
                    width / 2, 22, 0xFFFFFFFF);
        }
        drawCenteredString(ps, font,
                Component.literal("Drag / WASD: pan    Scroll: zoom    N: night vision    Q/E: switch    Esc: back"),
                width / 2, height - 40, 0xFFB0B0B0);

        // "Connecting..." fade from black when a feed opens
        float fade = 1f - Math.min(1f, (openTicks + partialTick) / 14f);
        if (fade > 0f) {
            fill(ps, 0, 0, width, height, ((int) (fade * 255f)) << 24);
        }
        super.render(ps, mouseX, mouseY, partialTick);
    }

    private void renderNoSignal(PoseStack ps) {
        fill(ps, 0, 0, width, height, 0xFF0A0A0A);
        Random r = new Random(openTicks * 31L + minecraft.level.getGameTime() / 2L);
        for (int i = 0; i < 140; i++) {
            int x0 = r.nextInt(width);
            int y0 = r.nextInt(height);
            int g = 40 + r.nextInt(120);
            fill(ps, x0, y0, x0 + 10 + r.nextInt(Math.max(1, width / 3)), y0 + 1 + r.nextInt(2),
                    0xFF000000 | (g << 16) | (g << 8) | g);
        }
        drawCenteredString(ps, font, Component.literal("NO SIGNAL"), width / 2, height / 2 - 4, 0xFFFFFFFF);
    }

    @Override
    public void removed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        CameraViewHandler.end();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // keep the world running so the feed is live (even in singleplayer)
    }
}
