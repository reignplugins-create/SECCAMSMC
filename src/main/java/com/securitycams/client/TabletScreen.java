package com.securitycams.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.securitycams.item.TabletItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** The tablet's home screen: a paged list of linked cameras. */
@OnlyIn(Dist.CLIENT)
public class TabletScreen extends Screen {
    private static final int PER_PAGE = 6;

    private final InteractionHand hand;
    private List<TabletItem.CameraLink> links = List.of();
    private int page = 0;

    public TabletScreen(InteractionHand hand) {
        super(Component.literal("Security Tablet"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        Minecraft mc = this.minecraft;
        links = TabletItem.readLinks(mc.player.getItemInHand(hand));

        int pages = Math.max(1, (links.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.min(page, pages - 1);

        int w = 240;
        int x = (width - w) / 2;
        int y = height / 2 - 82;
        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE && start + i < links.size(); i++) {
            final int index = start + i;
            TabletItem.CameraLink link = links.get(index);
            boolean online = CameraViewHandler.isOnline(link);
            String name = link.name().isEmpty() ? "Camera" : link.name();
            String label = (index + 1) + ". " + name + "  (" + link.pos().getX() + ", "
                    + link.pos().getY() + ", " + link.pos().getZ() + ")";
            Component text = Component.literal(label)
                    .append(online ? Component.literal("  ONLINE").withStyle(ChatFormatting.GREEN)
                                   : Component.literal("  OFFLINE").withStyle(ChatFormatting.RED));
            Button b = new Button(x, y + i * 22, w, 20, text,
                    btn -> mc.setScreen(new CameraViewScreen(this, hand, links, index)));
            b.active = online;
            addRenderableWidget(b);
        }

        int rowY = y + PER_PAGE * 22 + 4;
        if (pages > 1) {
            addRenderableWidget(new Button(x, rowY, 70, 20, Component.literal("< Page"), b -> {
                page = Math.floorMod(page - 1, pages);
                rebuildWidgets();
            }));
            addRenderableWidget(new Button(x + w - 70, rowY, 70, 20, Component.literal("Page >"), b -> {
                page = Math.floorMod(page + 1, pages);
                rebuildWidgets();
            }));
        }
        addRenderableWidget(new Button(x + 75, rowY, w - 150, 20, CommonComponents.GUI_DONE, b -> onClose()));
    }

    @Override
    public void render(PoseStack ps, int mouseX, int mouseY, float partialTick) {
        renderBackground(ps);
        drawCenteredString(ps, font, title, width / 2, height / 2 - 105, 0xFFFFFF);
        if (links.isEmpty()) {
            drawCenteredString(ps, font, Component.literal("No cameras linked yet."),
                    width / 2, height / 2 - 70, 0xAAAAAA);
            drawCenteredString(ps, font, Component.literal("Right-click a security camera with this tablet to link it."),
                    width / 2, height / 2 - 58, 0x888888);
        } else {
            drawCenteredString(ps, font,
                    Component.literal("Cameras only stream while within your render distance."),
                    width / 2, height / 2 - 93, 0x777777);
        }
        super.render(ps, mouseX, mouseY, partialTick);
    }
}
