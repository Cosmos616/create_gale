package net.cosmos.gale.content.phlogiston;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(
        modid = "gale",
        value = Dist.CLIENT
)
public final class PhlogistonRenderEvents {
    private PhlogistonRenderEvents() {}

    @SubscribeEvent
    public static void renderPhlogistonClouds(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        PoseStack poseStack = event.getPoseStack();

        if (poseStack == null) {return;}

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();

        PhlogistonCloudRenderer.renderCloudGrid(event, cameraPosition,400);
    }

}
