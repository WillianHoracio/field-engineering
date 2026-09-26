package com.willian.fieldengineering.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.willian.fieldengineering.entity.MissileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class MissileRenderer extends ThrownItemRenderer<MissileEntity> {

    private static final float MISSILE_SCALE = 2.0f;
    private static final float TRAIL_LENGTH = 8.0f;

    public MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
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

        poseStack.pushPose();
        poseStack.scale(
                MISSILE_SCALE,
                MISSILE_SCALE,
                MISSILE_SCALE
        );

        super.render(
                missile,
                entityYaw,
                partialTick,
                poseStack,
                buffer,
                packedLight
        );

        poseStack.popPose();
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
