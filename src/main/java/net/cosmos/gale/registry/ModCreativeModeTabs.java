package net.cosmos.gale.registry;

import net.cosmos.gale.Gale;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Gale.MOD_ID);

    public static final Supplier<CreativeModeTab> GALE_TAB = TABS.register("gale_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gale"))
                    .icon(() -> ModBlocks.GALE_DRIVE.get().asItem().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModBlocks.GALE_DRIVE.get());
                        output.accept(ModBlocks.PNEUMATIC_PIPE);
                    })
                    .build()
    );

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
