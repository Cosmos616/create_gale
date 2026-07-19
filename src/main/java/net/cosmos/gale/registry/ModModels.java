package net.cosmos.gale.registry;

import net.cosmos.gale.Gale;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;

public final class ModModels {

    private static final ModelResourceLocation GALE_DRIVE_PLATE_ID =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            Gale.MOD_ID,
                            "block/gale_drive/plate"
                    )
            );

    private static final ModelResourceLocation GALE_DRIVE_PETAL_TOP_ID =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            Gale.MOD_ID,
                            "block/gale_drive/petal_top"
                    )
            );

    private static final ModelResourceLocation GALE_DRIVE_PETAL_BOTTOM_ID =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            Gale.MOD_ID,
                            "block/gale_drive/petal_bottom"
                    )
            );

    private static final ModelResourceLocation GALE_DRIVE_PETAL_LEFT_ID =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            Gale.MOD_ID,
                            "block/gale_drive/petal_left"
                    )
            );

    private static final ModelResourceLocation GALE_DRIVE_PETAL_RIGHT_ID =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            Gale.MOD_ID,
                            "block/gale_drive/petal_right"
                    )
            );

    private static final ModelResourceLocation GALE_DRIVE_SPRING_ID =
            ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath(
                            Gale.MOD_ID,
                            "block/gale_drive/spring"
                    )
            );

    @Nullable
    public static BakedModel galeDrivePlate;

    @Nullable
    public static BakedModel galeDrivePetalTop;

    @Nullable
    public static BakedModel galeDrivePetalBottom;

    @Nullable
    public static BakedModel galeDrivePetalLeft;

    @Nullable
    public static BakedModel galeDrivePetalRight;
    @Nullable
    public static BakedModel galeDriveSpring;

    private ModModels() {
    }

    public static void registerAdditionalModels(
            ModelEvent.RegisterAdditional event
    ) {
        event.register(GALE_DRIVE_PLATE_ID);
        event.register(GALE_DRIVE_PETAL_TOP_ID);
        event.register(GALE_DRIVE_PETAL_BOTTOM_ID);
        event.register(GALE_DRIVE_PETAL_LEFT_ID);
        event.register(GALE_DRIVE_PETAL_RIGHT_ID);
        event.register(GALE_DRIVE_SPRING_ID);
    }

    public static void collectBakedModels(
            ModelEvent.ModifyBakingResult event
    ) {
        galeDrivePlate =
                event.getModels().get(GALE_DRIVE_PLATE_ID);

        galeDrivePetalTop =
                event.getModels().get(GALE_DRIVE_PETAL_TOP_ID);

        galeDrivePetalBottom =
                event.getModels().get(GALE_DRIVE_PETAL_BOTTOM_ID);

        galeDrivePetalLeft =
                event.getModels().get(GALE_DRIVE_PETAL_LEFT_ID);

        galeDrivePetalRight =
                event.getModels().get(GALE_DRIVE_PETAL_RIGHT_ID);
        galeDriveSpring =
                event.getModels().get(GALE_DRIVE_SPRING_ID);
    }
}