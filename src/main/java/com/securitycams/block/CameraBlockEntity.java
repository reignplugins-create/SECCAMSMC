package com.securitycams.block;

import java.util.UUID;
import javax.annotation.Nullable;

import com.securitycams.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CameraBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String cameraName = "";

    public CameraBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.CAMERA_BE.get(), pos, state);
    }

    /** Only the owner (or an op) may link this camera to a tablet. */
    public boolean canLink(Player player) {
        return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraName(String name) {
        this.cameraName = name;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("Name", cameraName);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        cameraName = tag.getString("Name");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", cameraName);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
