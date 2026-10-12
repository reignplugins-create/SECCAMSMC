package com.securitycams.item;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import com.securitycams.block.CameraBlock;
import com.securitycams.block.CameraBlockEntity;
import com.securitycams.client.ClientHooks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * The viewing tablet.
 * - Right-click a camera you own: link it. Sneak + right-click a linked camera: unlink it.
 * - Right-click anywhere else: open the camera list / live view.
 * Links live in the item's NBT under "Cameras".
 */
public class TabletItem extends Item {
    public static final int MAX_CAMERAS = 16;

    public record CameraLink(String dim, BlockPos pos, String name) {}

    public TabletItem(Properties properties) {
        super(properties);
    }

    public static List<CameraLink> readLinks(ItemStack stack) {
        List<CameraLink> out = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return out;
        }
        ListTag list = tag.getList("Cameras", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            out.add(new CameraLink(t.getString("Dim"),
                    new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z")),
                    t.getString("Name")));
        }
        return out;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof CameraBlock)) {
            return InteractionResult.PASS; // falls through to use(): open the tablet
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = ctx.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        if (!(level.getBlockEntity(pos) instanceof CameraBlockEntity cam) || !cam.canLink(player)) {
            player.displayClientMessage(Component.literal("You don't own this camera."), true);
            return InteractionResult.CONSUME;
        }

        CompoundTag tag = ctx.getItemInHand().getOrCreateTag();
        ListTag list = tag.getList("Cameras", Tag.TAG_COMPOUND);
        String dim = level.dimension().location().toString();

        int existing = -1;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            if (t.getString("Dim").equals(dim)
                    && t.getInt("X") == pos.getX() && t.getInt("Y") == pos.getY() && t.getInt("Z") == pos.getZ()) {
                existing = i;
                break;
            }
        }

        if (existing >= 0) {
            if (player.isShiftKeyDown()) {
                list.remove(existing);
                player.displayClientMessage(Component.literal("Camera unlinked."), true);
            } else {
                player.displayClientMessage(
                        Component.literal("Already linked. Sneak + right-click to unlink."), true);
            }
        } else if (list.size() >= MAX_CAMERAS) {
            player.displayClientMessage(Component.literal("Tablet is full (" + MAX_CAMERAS + " cameras)."), true);
        } else {
            CompoundTag t = new CompoundTag();
            t.putString("Dim", dim);
            t.putInt("X", pos.getX());
            t.putInt("Y", pos.getY());
            t.putInt("Z", pos.getZ());
            String name = cam.getCameraName();
            t.putString("Name", name.isEmpty() ? "Camera" : name);
            list.add(t);
            player.displayClientMessage(Component.literal("Camera linked."), true);
        }
        tag.put("Cameras", list);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openTablet(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Every couple of seconds, drop links whose camera block no longer exists (only checks loaded chunks). */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || entity.tickCount % 40 != 0 || !stack.hasTag()) {
            return;
        }
        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }
        ListTag list = stack.getTag().getList("Cameras", Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int i = list.size() - 1; i >= 0; i--) {
            CompoundTag t = list.getCompound(i);
            ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY,
                    new ResourceLocation(t.getString("Dim")));
            ServerLevel target = server.getLevel(key);
            BlockPos p = new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z"));
            if (target != null && target.isLoaded(p)
                    && !(target.getBlockState(p).getBlock() instanceof CameraBlock)) {
                list.remove(i);
                changed = true;
            }
        }
        if (changed) {
            stack.getTag().put("Cameras", list);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Linked cameras: " + readLinks(stack).size() + "/" + MAX_CAMERAS)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click a camera to link it.").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Sneak + right-click it to unlink.").withStyle(ChatFormatting.DARK_GRAY));
    }
}
