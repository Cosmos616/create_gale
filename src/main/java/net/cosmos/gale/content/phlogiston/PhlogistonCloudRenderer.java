package net.cosmos.gale.content.phlogiston;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;


public class PhlogistonCloudRenderer {
    private static final long CLOUD_SEED = 829374982374L;

    public static void renderClouds(RenderLevelStageEvent event, PoseStack poseStack, Vec3 cameraPosition){
    }

    public static void renderCloudGrid(RenderLevelStageEvent event, Vec3 cameraPosition, double cloudY) {
        final int radius = Minecraft.getInstance().options.renderDistance().get();
        final int spacing = 16;

        final int centerGridX = Math.floorDiv(Mth.floor(cameraPosition.x), spacing);
        final int centerGridZ = Math.floorDiv(Mth.floor(cameraPosition.z), spacing);

        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
            for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                final int gridX = centerGridX + offsetX;
                final int gridZ = centerGridZ + offsetZ;
                final double worldX = gridX * spacing;
                final double worldZ = gridZ * spacing;

                RandomSource random = randomSource(CLOUD_SEED, gridX, gridZ);
                BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

                AABB cloudBox = new AABB(worldX + random.nextFloat()*8, cloudY + random.nextFloat()*8, worldZ+ random.nextFloat()*8, worldX + 16.0 + random.nextFloat()*10, cloudY + 4.0 + random.nextFloat()*10, worldZ + 16.0 + random.nextFloat()*10);

                renderBox(event, cloudBox, buffer, 1f,1f,1f,0.5f);
            }
        }
    }

    private static RandomSource randomSource(long worldSeed, int gridX, int gridZ) {
        long seed = worldSeed;

        seed ^= (long) gridX * 341873128712L;
        seed ^= (long) gridZ * 132897987541L;

        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdl;
        seed ^= seed >>> 33;
        seed *= 0xc4ceb9fe1a85ec53l;
        seed ^= seed >>> 33;

        return RandomSource.create(seed);
    }

    public static void renderBox(RenderLevelStageEvent event, AABB worldBox, BufferBuilder buffer, float red, float green, float blue, float alpha) {
        PoseStack poseStack = event.getPoseStack();

        if (poseStack == null) return;

        Vec3 camera = event.getCamera().getPosition();

        final float minX = (float) (worldBox.minX - camera.x);
        final float minY = (float) (worldBox.minY - camera.y);
        final float minZ = (float) (worldBox.minZ - camera.z);

        final float maxX = (float) (worldBox.maxX - camera.x);
        final float maxY = (float) (worldBox.maxY - camera.y);
        final float maxZ = (float) (worldBox.maxZ - camera.z);

        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        // Needed so the box remains visible from inside.
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);



        // Bottom
        addFace(
                buffer,
                matrix,
                minX, minY, minZ,
                minX, minY, maxZ,
                maxX, minY, maxZ,
                maxX, minY, minZ,
                red, green, blue, alpha
        );

        // Top
        addFace(
                buffer,
                matrix,
                minX, maxY, minZ,
                maxX, maxY, minZ,
                maxX, maxY, maxZ,
                minX, maxY, maxZ,
                red, green, blue, alpha
        );

        // North
        addFace(
                buffer,
                matrix,
                minX, minY, minZ,
                maxX, minY, minZ,
                maxX, maxY, minZ,
                minX, maxY, minZ,
                red, green, blue, alpha
        );

        // South
        addFace(
                buffer,
                matrix,
                minX, minY, maxZ,
                minX, maxY, maxZ,
                maxX, maxY, maxZ,
                maxX, minY, maxZ,
                red, green, blue, alpha
        );

        // West
        addFace(
                buffer,
                matrix,
                minX, minY, minZ,
                minX, maxY, minZ,
                minX, maxY, maxZ,
                minX, minY, maxZ,
                red, green, blue, alpha
        );

        // East
        addFace(
                buffer,
                matrix,
                maxX, minY, minZ,
                maxX, minY, maxZ,
                maxX, maxY, maxZ,
                maxX, maxY, minZ,
                red, green, blue, alpha
        );

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void addFace(BufferBuilder buffer, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float red, float green, float blue, float alpha) {
        buffer.addVertex(matrix, x0, y0, z0).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, x1, y1, z1).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, x2, y2, z2).setColor(red, green, blue, alpha);
        buffer.addVertex(matrix, x3, y3, z3).setColor(red, green, blue, alpha);
    }



}
