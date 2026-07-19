package net.cosmos.gale.client;

import net.cosmos.gale.Gale;
import net.cosmos.gale.blockentities.ModBlockEntities;
import net.cosmos.gale.client.rendering.GaleDriveRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(
        modid = Gale.MOD_ID,
        value = Dist.CLIENT
)
public final class ModRendererEvents {

    private ModRendererEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(
            EntityRenderersEvent.RegisterRenderers event
    ) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.GALE_DRIVE.get(),
                GaleDriveRenderer::new
        );
    }
}