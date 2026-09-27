package com.willian.fieldengineering.block;

import com.mojang.serialization.MapCodec;
import com.willian.fieldengineering.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class LauncherPartBlock extends Block {

    public static final MapCodec<LauncherPartBlock> CODEC =
            simpleCodec(LauncherPartBlock::new);

    /*
     * IntegerProperty não aceita valores negativos.
     *
     * X/Z são armazenados como 0..2 e representam
     * offsets reais -1..1 através de encode/decode.
     */
    public static final IntegerProperty OFFSET_X =
            IntegerProperty.create("offset_x", 0, 2);

    public static final IntegerProperty OFFSET_Y =
            IntegerProperty.create("offset_y", 0, 2);

    public static final IntegerProperty OFFSET_Z =
            IntegerProperty.create("offset_z", 0, 2);

    public LauncherPartBlock(
            Properties properties
    ) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(OFFSET_X, 1)
                        .setValue(OFFSET_Y, 0)
                        .setValue(OFFSET_Z, 1)
        );
    }

    public static int encodeHorizontalOffset(int offset) {
        return offset + 1;
    }

    public static int decodeHorizontalOffset(int encodedOffset) {
        return encodedOffset - 1;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                OFFSET_X,
                OFFSET_Y,
                OFFSET_Z
        );
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player
    ) {
        if (!level.isClientSide) {
            BlockPos masterPos =
                    findMasterBlock(
                            level,
                            pos,
                            state
                    );

            if (masterPos != null) {
                level.destroyBlock(
                        masterPos,
                        !player.isCreative(),
                        player
                );
            }
        }

        return super.playerWillDestroy(
                level,
                pos,
                state,
                player
        );
    }

    private BlockPos findMasterBlock(
            Level level,
            BlockPos partPos,
            BlockState state
    ) {
        int offsetX = decodeHorizontalOffset(
                state.getValue(OFFSET_X)
        );

        int offsetZ = decodeHorizontalOffset(
                state.getValue(OFFSET_Z)
        );

        BlockPos expectedMasterPos =
                partPos.offset(
                        -offsetX,
                        -state.getValue(OFFSET_Y),
                        -offsetZ
                );

        if (
                level.getBlockState(expectedMasterPos)
                        .is(ModBlocks.AA_TURRET)
        ) {
            return expectedMasterPos;
        }

        for (int yOffset = -2; yOffset <= 0; yOffset++) {
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int zOffset = -1; zOffset <= 1; zOffset++) {
                    BlockPos checkPos =
                            partPos.offset(
                                    xOffset,
                                    yOffset,
                                    zOffset
                            );

                    if (
                            level.getBlockState(checkPos)
                                    .is(ModBlocks.AA_TURRET)
                    ) {
                        return checkPos;
                    }
                }
            }
        }

        return null;
    }
}
