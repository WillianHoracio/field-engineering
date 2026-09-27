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

    private static final ResourceLocation DISH_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");

    private static final ResourceLocation FRAME_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/polished_deepslate.png");

    private static final ResourceLocation DETAIL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/block/weathered_copper.png");

    private static final float ROTATION_SPEED = 0.025f;

    private final ModelPart mechanism;
    private final ModelPart dish;
    private final ModelPart details;

    public RadarRenderer(BlockEntityRendererProvider.Context context) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(
                "mechanism",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-1.5f, 0.0f, -1.5f, 3.0f, 5.0f, 3.0f)
                        .texOffs(0, 0)
                        .addBox(-3.0f, 5.0f, -3.0f, 6.0f, 2.0f, 6.0f)
                        .texOffs(0, 0)
                        .addBox(-5.0f, 7.0f, -1.0f, 2.0f, 4.0f, 2.0f)
                        .texOffs(0, 0)
                        .addBox(3.0f, 7.0f, -1.0f, 2.0f, 4.0f, 2.0f)
                        .texOffs(0, 0)
                        .addBox(-5.0f, 10.0f, -1.25f, 10.0f, 2.0f, 2.5f)
                        .texOffs(0, 0)
                        .addBox(-1.0f, 12.0f, -1.0f, 2.0f, 2.0f, 2.0f),
                PartPose.ZERO
        );

        PartDefinition dishRoot = root.addOrReplaceChild(
                "dish",
                CubeListBuilder.create(),
                PartPose.offset(0.0f, 14.0f, 0.0f)
        );

        dishRoot.addOrReplaceChild(
                "center",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-6.0f, -4.0f, -0.75f, 12.0f, 8.0f, 1.5f),
                PartPose.ZERO
        );

        dishRoot.addOrReplaceChild(
                "left_wing",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0f, -4.0f, -0.75f, 4.0f, 8.0f, 1.5f),
                PartPose.offsetAndRotation(-6.0f, 0.0f, 0.0f, 0.0f, -0.35f, 0.0f)
        );

        dishRoot.addOrReplaceChild(
                "right_wing",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(0.0f, -4.0f, -0.75f, 4.0f, 8.0f, 1.5f),
                PartPose.offsetAndRotation(6.0f, 0.0f, 0.0f, 0.0f, 0.35f, 0.0f)
        );

        root.addOrReplaceChild(
                "details",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0f, 10.5f, -3.0f, 4.0f, 3.0f, 3.0f)
                        .texOffs(0, 0)
                        .addBox(-0.75f, 14.0f, -2.0f, 1.5f, 3.0f, 2.0f)
                        .texOffs(0, 0)
                        .addBox(6.0f, 12.5f, -1.5f, 2.0f, 2.0f, 2.0f),
                PartPose.ZERO
        );

        ModelPart rootPart = LayerDefinition
                .create(mesh, 32, 32)
                .bakeRoot();

        this.mechanism = rootPart.getChild("mechanism");
        this.dish = rootPart.getChild("dish");
        this.details = rootPart.getChild("details");
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

        // The fixed pedestal ends at 12/16 of the lower block. The rotating assembly
        // starts there and extends upward into the second block.
        poseStack.translate(0.5, 0.75, 0.5);
        poseStack.mulPose(Axis.YP.rotation(rotation));

        VertexConsumer frameConsumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(FRAME_TEXTURE)
        );

        mechanism.render(
                poseStack,
                frameConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        VertexConsumer dishConsumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(DISH_TEXTURE)
        );

        dish.render(
                poseStack,
                dishConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        VertexConsumer detailConsumer = buffer.getBuffer(
                RenderType.entityCutoutNoCull(DETAIL_TEXTURE)
        );

        details.render(
                poseStack,
                detailConsumer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );

        poseStack.popPose();
    }
}
