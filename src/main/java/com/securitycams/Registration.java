package com.securitycams;

import com.securitycams.block.CameraBlock;
import com.securitycams.block.CameraBlockEntity;
import com.securitycams.item.TabletItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class Registration {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, SecurityCamsMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SecurityCamsMod.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SecurityCamsMod.MODID);

    public static final CreativeModeTab TAB = new CreativeModeTab(SecurityCamsMod.MODID) {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(CAMERA_ITEM.get());
        }
    };

    public static final RegistryObject<Block> CAMERA_BLOCK = BLOCKS.register("security_camera",
            () -> new CameraBlock(BlockBehaviour.Properties.of(Material.METAL)
                    .strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryObject<Item> CAMERA_ITEM = ITEMS.register("security_camera",
            () -> new BlockItem(CAMERA_BLOCK.get(), new Item.Properties().tab(TAB)));

    public static final RegistryObject<Item> TABLET = ITEMS.register("security_tablet",
            () -> new TabletItem(new Item.Properties().tab(TAB).stacksTo(1)));

    public static final RegistryObject<BlockEntityType<CameraBlockEntity>> CAMERA_BE =
            BLOCK_ENTITIES.register("security_camera",
                    () -> BlockEntityType.Builder.of(CameraBlockEntity::new, CAMERA_BLOCK.get()).build(null));

    private Registration() {}
}
