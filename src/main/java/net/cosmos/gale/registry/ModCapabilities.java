package net.cosmos.gale.registry;

import net.cosmos.gale.Gale;
import net.cosmos.gale.content.gale_drive.GaleDriveBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = Gale.MOD_ID)
public final class ModCapabilities {

    private ModCapabilities() { }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event ) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.GALE_DRIVE.get(),
                ModCapabilities::getGaleDriveItemHandler
        );
    }

    private static net.neoforged.neoforge.items.IItemHandler
    getGaleDriveItemHandler(GaleDriveBlockEntity blockEntity, Direction side) {return blockEntity.getInventory();}
}