package com.willian.fieldengineering.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.willian.fieldengineering.entity.MissileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class MissileRenderer extends EntityRenderer<MissileEntity> {

    private static final float MISSILE_SCALE = 1.6f;
    private static final float TRAIL_LENGTH = 8.0f;

    private final ItemRenderer itemRenderer;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            MissileEntity missile,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        renderTrail(missile, poseStack, buffer);

        Vec3 movement = missile.getDeltaMovement();
        Vec3 direction = movement.lengthSqr() < 0.0001
                ? new Vec3(0.0, 1.0, 0.0)
                : movement.normalize();

        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f flightDirection = new Vector3f(
                (float) direction.x,
                (float) direction.y,
                (float) direction.z
        );

        Quaternionf orientation = new Quaternionf()
                .rotationTo(
                        up,
                        flightDirection
                );

        poseStack.pushPose();
        poseStack.mulPose(orientation);
        poseStack.scale(
                MISSILE_SCALE,
                MISSILE_SCALE,
                MISSILE_SCALE
        );

        itemRenderer.renderStatic(
                missile.getItem(),
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                missile.level(),
                missile.getId()
        );

        poseStack.popPose();

        super.render(
                missile,
                entityYaw,
                partialTick,
                poseStack,
                buffer,
                packedLight
        );
    }

    @Override
    public ResourceLocation getTextureLocation(MissileEntity missile) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private void renderTrail(
            MissileEntity missile,
            PoseStack poseStack,
            MultiBufferSource buffer
    ) {
        Vec3 movement = missile.getDeltaMovement();
        Vec3 direction = movement.lengthSqr() < 0.0001
                ? new Vec3(0.0, 1.0, 0.0)
                : movement.normalize();

        Vec3 tail = direction.scale(-TRAIL_LENGTH);

        drawTrailLayer(
                poseStack,
                buffer,
                tail,
                7.0,
                0.72f,
                0.72f,
                0.72f,
                0.42f
        );

        drawTrailLayer(
                poseStack,
                buffer,
                tail.scale(0.70),
                4.0,
                1.0f,
                0.62f,
                0.18f,
                0.78f
        );

        drawTrailLayer(
                poseStack,
                buffer,
                tail.scale(0.42),
                2.0,
                1.0f,
                0.94f,
                0.72f,
                1.0f
        );
    }

    private void drawTrailLayer(
            PoseStack poseStack,
            MultiBufferSource buffer,
            Vec3 tail,
            double width,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        VertexConsumer consumer = buffer.getBuffer(
                RenderType.debugLineStrip(width)
        );

        Matrix4f pose = poseStack.last().pose();

        float normalX = (float) -tail.x;
        float normalY = (float) -tail.y;
        float normalZ = (float) -tail.z;

        float normalLength = (float) Math.sqrt(
                normalX * normalX
                        + normalY * normalY
                        + normalZ * normalZ
        );

        if (normalLength > 0.0f) {
            normalX /= normalLength;
            normalY /= normalLength;
            normalZ /= normalLength;
        }

        consumer
                .addVertex(pose, 0.0f, 0.0f, 0.0f)
                .setColor(red, green, blue, alpha)
                .setNormal(
                        poseStack.last(),
                        normalX,
                        normalY,
                        normalZ
                );

        consumer
                .addVertex(
                        pose,
                        (float) tail.x,
                        (float) tail.y,
                        (float) tail.z
                )
                .setColor(red, green, blue, 0.0f)
                .setNormal(
                        poseStack.last(),
                        normalX,
                        normalY,
                        normalZ
                );
    }
}
