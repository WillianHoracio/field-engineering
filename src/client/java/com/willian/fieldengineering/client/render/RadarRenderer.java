package com.willian.fieldengineering.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.willian.fieldengineering.blockentity.RadarBlockEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class RadarRenderer implements BlockEntityRenderer<RadarBlockEntity> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    private static final float ROTATION_SPEED = 0.045f;

    private final ModelPart antenna;

    public RadarRenderer(BlockEntityRendererProvider.Context context) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "antenna",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.0f, -4.0f, -1.0f, 2.0f, 4.0f, 2.0f)
                        .texOffs(0, 0)
                        .addBox(-6.0f, -1.0f, -0.75f, 12.0f, 2.0f, 1.5f)
                        .texOffs(0, 0)
                        .addBox(-5.5f, -4.0f, -0.5f, 1.5f, 3.0f, 1.0f)
                        .texOffs(0, 0)
                        .addBox(4.0f, -4.0f, -0.5f, 1.5f, 3.0f, 1.0f)
                        .texOffs(0, 0)
                        .addBox(-2.0f, -2.25f, -1.0f, 4.0f, 1.5f, 2.0f),
                PartPose.ZERO
        );

        this.antenna = LayerDefinition
                .create(mesh, 16, 16)
                .bakeRoot()
                .getChild("antenna");
    }

    @Override
    public void render(
            RadarBlockEntity radar,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        if (radar.getLevel() == null) {
            return;
        }

        float rotation = (
                radar.getLevel().getGameTime()
                        + partialTick
        ) * ROTATION_SPEED;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.96, 0.5);
        poseStack.mulPose(Axis.YP.rotation(rotation));

        VertexConsumer consumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(TEXTURE)
        );

        antenna.render(
                poseStack,
                consumer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }
}
