package net.cosmos.gale.registry;

import net.cosmos.gale.Gale;
import net.cosmos.gale.content.gale_drive.GaleDriveBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Gale.MOD_ID);

    public static final Supplier<BlockEntityType<GaleDriveBlockEntity>>
            GALE_DRIVE =
            BLOCK_ENTITIES.register(
                    "gale_drive",
                    () -> BlockEntityType.Builder.of(
                            GaleDriveBlockEntity::new,
                            ModBlocks.GALE_DRIVE.get()
                    ).build(null)
            );

    public static void register(IEventBus eventBus){
        BLOCK_ENTITIES.register(eventBus);
    }
}
