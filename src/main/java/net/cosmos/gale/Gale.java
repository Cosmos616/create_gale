package net.cosmos.gale;

import net.cosmos.gale.registry.ModBlockEntities;
import net.cosmos.gale.registry.ModBlocks;
import net.cosmos.gale.registry.ModCreativeModeTabs;
import net.cosmos.gale.registry.ModItems;
import net.cosmos.gale.registry.ModSoundEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Gale.MOD_ID)
public class Gale {

    public static final String MOD_ID = "gale";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Gale(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModBlockEntities.register(modEventBus);
        ModSoundEvents.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        LOGGER.info("Gale Drive max thrust: {} pN", Config.MAX_THRUST.get());
        LOGGER.info("Gale Drive max airflow: {} m/s", Config.MAX_AIRFLOW.get());
        LOGGER.info("Gale Drive charge capacity: {}", Config.CHARGE_CAPACITY.get());
        LOGGER.info("Gale Drive charge drain divisor: {}", Config.CHARGE_DRAIN_DIVISOR.get());
    }


    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    public static ResourceLocation GetResource(String value) {return ResourceLocation.fromNamespaceAndPath(Gale.MOD_ID, value);}
}
