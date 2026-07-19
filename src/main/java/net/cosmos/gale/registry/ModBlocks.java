package net.cosmos.gale.registry;

import net.cosmos.gale.Gale;
import net.cosmos.gale.content.gale_drive.GaleDriveBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Gale.MOD_ID);


    public static final DeferredBlock<Block> GALE_DRIVE = RegisterBlock("gale_drive", () ->
            new GaleDriveBlock(BlockBehaviour.Properties.of()
                    .sound(SoundType.NETHERITE_BLOCK)
                    .noOcclusion()
            )
    );


    private static <T extends Block> DeferredBlock<T> RegisterBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name,block);
        RegisterBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void RegisterBlockItem(String name, DeferredBlock<T> block){
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
