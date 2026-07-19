package net.cosmos.gale.registry;

import net.cosmos.gale.Gale;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(
        modid = Gale.MOD_ID,
        value = Dist.CLIENT
)
public final class ModModelEvents {

    private ModModelEvents() { }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        ModModels.registerAdditionalModels(event);
    }

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        ModModels.collectBakedModels(event);
    }
}