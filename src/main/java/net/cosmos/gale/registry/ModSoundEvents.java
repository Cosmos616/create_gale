package net.cosmos.gale.registry;

import net.cosmos.gale.Gale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, Gale.MOD_ID);

    public static final Supplier<SoundEvent> GALE_DRIVE_LOOP = SOUND_EVENTS.register("block.gale_drive.loop", () -> SoundEvent.createVariableRangeEvent(Gale.GetResource("block.gale_drive.loop")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
