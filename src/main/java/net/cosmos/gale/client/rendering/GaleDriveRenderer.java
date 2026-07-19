package net.cosmos.gale.client.rendering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.cosmos.gale.Gale;
import net.cosmos.gale.blockentities.GaleDriveBlockEntity;
import net.cosmos.gale.blocks.GaleDriveBlock;
import net.cosmos.gale.client.ModModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import javax.annotation.Nullable;

public class GaleDriveRenderer
        implements BlockEntityRenderer<GaleDriveBlockEntity> {

    private final ModelBlockRenderer modelRenderer;
    private static final float MAX_PLATE_TRAVEL = 4.0F / 16.0F;
    private static final float MAX_PETAL_ANGLE = 35.0F;
    private final EntityRenderDispatcher entityRenderDispatcher;

    private static final float WIND_TOTAL_LENGTH = 4.0F; // 4 blocks
    private static final float WIND_START_WIDTH = 0.8F;
    private static final float WIND_END_WIDTH = 2F;
    private static final int WIND_SEGMENTS = 4;

    private static final float SPRING_LENGTH = 9.0F / 16.0F;

    private static final ResourceLocation WIND_TEXTURE = ResourceLocation.fromNamespaceAndPath(Gale.MOD_ID, "textures/entity/gale_drive_wind.png");
    private static final float WIND_TEXTURE_SIZE = 128.0F;

    @Nullable
    private WindCharge renderedWindCharge;


    public GaleDriveRenderer(BlockEntityRendererProvider.Context context) {
        this.modelRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        this.entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    }

    @Nullable
    private WindCharge getRenderedWindCharge(GaleDriveBlockEntity blockEntity) {
        if (blockEntity.getLevel() == null) return null;

        if (renderedWindCharge == null || renderedWindCharge.level() != blockEntity.getLevel()) {
            renderedWindCharge = new WindCharge(EntityType.WIND_CHARGE, blockEntity.getLevel());
            renderedWindCharge.setNoGravity(true);
        }
        return renderedWindCharge;
    }


    @Override
    public void render(GaleDriveBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(GaleDriveBlock.FACING);

        float output = Mth.clamp(blockEntity.getRenderedOutput(partialTick), 0.0F, 1.0F);
        float petalAngle = output * MAX_PETAL_ANGLE;
        float compressionOffset = output * MAX_PLATE_TRAVEL;

        poseStack.pushPose();
        rotateToFacing(poseStack, facing);

        renderSpring(
                compressionOffset,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderMovingAssembly(
                blockEntity,
                partialTick,
                compressionOffset,
                petalAngle- 10,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }

    private void renderSpring(float compressionOffset, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();

        float compressedLength = SPRING_LENGTH - compressionOffset;
        float zScale = Mth.clamp(compressedLength / SPRING_LENGTH, 0.01F, 1.0F);
        float pivotX = 0.5F;
        float pivotY = 0.5F;
        float pivotZ = 0.5F;

        poseStack.translate(pivotX, pivotY, pivotZ);
        poseStack.scale(1.0F, 1.0F, zScale);
        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        renderModel(
                ModModels.galeDriveSpring,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }


    private void renderMovingAssembly(GaleDriveBlockEntity blockEntity, float partialTick, float compressionOffset, float petalAngle, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -compressionOffset);

        renderWindChargeEntity(
                blockEntity,
                partialTick,
                poseStack,
                bufferSource,
                packedLight
        );

        renderModel(
                ModModels.galeDrivePlate,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderTopPetal(
                petalAngle,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderBottomPetal(
                petalAngle,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderLeftPetal(
                petalAngle,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderRightPetal(
                petalAngle,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        renderWindCone(
                blockEntity,
                partialTick,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private void renderWindChargeEntity(GaleDriveBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float chargeRemaining = Mth.clamp(blockEntity.getRenderedChargeRemaining(partialTick), 0.0F, 1.0F
        );

        if (chargeRemaining <= 0.0F) return;
        WindCharge windCharge = getRenderedWindCharge(blockEntity);

        if (windCharge == null) return;

        poseStack.pushPose();
        poseStack.translate(8.0F / 16.0F, 8.0F / 16.0F, 23.0F / 16.0F);

        float visualCharge = Mth.sqrt(chargeRemaining);
        float minScale = 0.5F;
        float maxScale = 1F;
        float scale = Mth.lerp(minScale, maxScale, visualCharge);
        poseStack.scale(scale, -scale, scale);

        windCharge.tickCount = blockEntity.getLevel() == null ? 0 : (int) blockEntity.getLevel().getGameTime();

        windCharge.setPos(0.0, 0.0, 0.0);
        windCharge.setDeltaMovement(Vec3.ZERO);

        entityRenderDispatcher.render(windCharge, 0.0, 0.0, 0.0, 0.0F, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }


    private void renderTopPetal(float angle, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        renderRotatedPetal(
                ModModels.galeDrivePetalTop,
                8.0F / 16.0F,
                14.0F / 16.0F,
                20.0F / 16.0F,
                Axis.XP,
                -angle,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }

    private void renderBottomPetal(float angle, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderRotatedPetal(
                ModModels.galeDrivePetalBottom,
                8.0F / 16.0F,
                2.0F / 16.0F,
                20.0F / 16.0F,
                Axis.XP,
                angle,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }

    private void renderLeftPetal(float angle, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderRotatedPetal(
                ModModels.galeDrivePetalLeft,
                2.0F / 16.0F,
                8.0F / 16.0F,
                20.0F / 16.0F,
                Axis.YP,
                -angle,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }

    private void renderRightPetal(float angle, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderRotatedPetal(
                ModModels.galeDrivePetalRight,
                14.0F / 16.0F,
                8.0F / 16.0F,
                20.0F / 16.0F,
                Axis.YP,
                angle,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
    }


    private void renderRotatedPetal(BakedModel model, float pivotX, float pivotY, float pivotZ, Axis axis, float angleDegrees, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (model == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(pivotX, pivotY, pivotZ);
        poseStack.mulPose(axis.rotationDegrees(angleDegrees));
        poseStack.translate(-pivotX, -pivotY, -pivotZ);

        renderModel(
                model,
                state,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private void renderModel(BakedModel model, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (model == null) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.cutout());

        modelRenderer.renderModel(
                poseStack.last(),
                consumer,
                state,
                model,
                1.0F,
                1.0F,
                1.0F,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                RenderType.cutout()
        );
    }

    private void renderWindCone(GaleDriveBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        float output = Mth.clamp(blockEntity.getRenderedOutput(partialTick), 0.0F, 1.0F);
        if (output <= 0.001F || !blockEntity.hasLoadedCharge()) return;

        float time = level.getGameTime() + partialTick;
        float scrollSpeed = Mth.lerp(0.01F, 0.05F, output);

        float uOffset = 0F;
        float vOffset = time * scrollSpeed;

        RenderType renderType = RenderType.breezeWind(WIND_TEXTURE, uOffset, vOffset);

        VertexConsumer consumer = bufferSource.getBuffer(renderType);
        poseStack.pushPose();

        /*
         * Start from the center of the nozzle/plate area.
         */
        poseStack.translate(8.0F / 16.0F, 8.0F / 16.0F, 20.0F / 16.0F);

        renderWindSegments(
                poseStack,
                consumer,
                output,
                packedLight,
                packedOverlay,
                time
        );

        poseStack.popPose();
    }

    private void renderWindSegments(PoseStack poseStack, VertexConsumer consumer, float output, int packedLight, int packedOverlay, float time) {
        float segmentLength = WIND_TOTAL_LENGTH / WIND_SEGMENTS;

        for (int segment = 0; segment < WIND_SEGMENTS; segment++) {
            float t = (float) segment / (float) WIND_SEGMENTS;
            t = Mth.clamp(t,0f,1f);

            float width = Mth.lerp(t, WIND_START_WIDTH, WIND_END_WIDTH);
            float halfWidth = (width * 0.5f) * output;
            float z0 = segment * segmentLength * output;
            float z1 = (segment + 1.0F) * segmentLength * output;
            float fade = ((float) segment / (float) WIND_SEGMENTS);
            float alpha = Mth.clamp(output * fade, 0.2F, 0.4F);

            int alphaByte = Mth.clamp((int) (alpha * 255.0F), 0, 255);

            float secondtime = time + segment*17;

            renderWindBoxSegment(
                    poseStack,
                    consumer,
                    z0,
                    z1,
                    halfWidth,
                    width,
                    segment,
                    alphaByte,
                    packedLight,
                    packedOverlay,
                    secondtime
            );
        }
    }

    public static final float scale_amplitude = 0.01f;
    public static final float scale_freq = 0.5f;

    private void renderWindBoxSegment(PoseStack poseStack, VertexConsumer consumer, float z0, float z1, float halfWidth, float fullWidth, int segmentIndex, int alpha, int packedLight, int packedOverlay, float time) {
        poseStack.pushPose();
        poseStack.scale( 1 + Mth.sin(time*scale_freq)*scale_amplitude,1 + Mth.sin(time*scale_freq)*scale_amplitude,1 + Mth.sin(time*scale_freq)*scale_amplitude);
        // Top face
        renderWindFace(
                poseStack,
                consumer,
                new Vector3f(-halfWidth, -halfWidth, z0),
                new Vector3f( halfWidth, -halfWidth, z0),
                new Vector3f( halfWidth, -halfWidth, z1),
                new Vector3f(-halfWidth, -halfWidth, z1),
                0,
                segmentIndex,
                alpha,
                packedLight,
                packedOverlay
        );

        // Right face
        renderWindFace(
                poseStack,
                consumer,
                new Vector3f(halfWidth, -halfWidth, z0),
                new Vector3f(halfWidth,  halfWidth, z0),
                new Vector3f(halfWidth,  halfWidth, z1),
                new Vector3f(halfWidth, -halfWidth, z1),
                1,
                segmentIndex,
                alpha,
                packedLight,
                packedOverlay
        );

        // Bottom face
        renderWindFace(
                poseStack,
                consumer,
                new Vector3f( halfWidth, halfWidth, z0),
                new Vector3f(-halfWidth, halfWidth, z0),
                new Vector3f(-halfWidth, halfWidth, z1),
                new Vector3f( halfWidth, halfWidth, z1),
                2,
                segmentIndex,
                alpha,
                packedLight,
                packedOverlay
        );

        // Left face
        renderWindFace(
                poseStack,
                consumer,
                new Vector3f(-halfWidth,  halfWidth, z0),
                new Vector3f(-halfWidth, -halfWidth, z0),
                new Vector3f(-halfWidth, -halfWidth, z1),
                new Vector3f(-halfWidth,  halfWidth, z1),
                3,
                segmentIndex,
                alpha,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private void renderWindFace(PoseStack poseStack, VertexConsumer consumer, Vector3f a, Vector3f b, Vector3f c, Vector3f d, int faceIndex, int segmentIndex, int alpha, int packedLight, int packedOverlay) {
        float faceWidthPixels = new Vector3f(b).sub(a).length() * 16.0F;
        float faceLengthPixels = new Vector3f(c).sub(b).length() * 16.0F;
        float uSize = faceLengthPixels / WIND_TEXTURE_SIZE;
        float vSize = faceWidthPixels / WIND_TEXTURE_SIZE;

        float u0 = faceIndex * uSize;
        float u1 = u0 + uSize;
        float v0 = segmentIndex * vSize;
        float v1 = v0 + vSize;

        Vector3f normal = calculateNormal(a, b, c);

        // Front
        addWindVertex(
                consumer, poseStack,
                a,
                u0, v0,
                normal,
                alpha,
                packedLight,
                packedOverlay
        );

        addWindVertex(
                consumer, poseStack,
                b,
                u0, v1,
                normal,
                alpha,
                packedLight,
                packedOverlay
        );

        addWindVertex(
                consumer, poseStack,
                c,
                u1, v1,
                normal,
                alpha,
                packedLight,
                packedOverlay
        );

        addWindVertex(
                consumer, poseStack,
                d,
                u1, v0,
                normal,
                alpha,
                packedLight,
                packedOverlay
        );

        // Back
        Vector3f reverseNormal = new Vector3f(normal).negate();

        addWindVertex(
                consumer, poseStack,
                d,
                u1, v0,
                reverseNormal,
                alpha,
                packedLight,
                packedOverlay
        );

        addWindVertex(
                consumer, poseStack,
                c,
                u1, v1,
                reverseNormal,
                alpha,
                packedLight,
                packedOverlay
        );

        addWindVertex(
                consumer, poseStack,
                b,
                u0, v1,
                reverseNormal,
                alpha,
                packedLight,
                packedOverlay
        );

        addWindVertex(
                consumer, poseStack,
                a,
                u0, v0,
                reverseNormal,
                alpha,
                packedLight,
                packedOverlay
        );
    }

    private void addWindVertex(VertexConsumer consumer, PoseStack poseStack, Vector3f position, float u, float v, Vector3f normal, int alpha, int packedLight, int packedOverlay) {
        consumer.addVertex(poseStack.last(), position.x(), position.y(), position.z())
                .setColor(255, 255, 255, alpha)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(poseStack.last(), normal.x(), normal.y(), normal.z()
                );
    }

    private Vector3f calculateNormal(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f edgeAB = new Vector3f(b).sub(a);
        Vector3f edgeAC = new Vector3f(c).sub(a);
        return edgeAB.cross(edgeAC).normalize();
    }

    private static void rotateToFacing(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case NORTH -> {}
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP -> poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(270.0F));
        }
        poseStack.translate(-0.5F, -0.5F, -0.5F);
    }
}