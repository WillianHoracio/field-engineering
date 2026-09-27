package com.willian.fieldengineering.block;

import com.mojang.serialization.MapCodec;
import com.willian.fieldengineering.blockentity.LauncherBlockEntity;
import com.willian.fieldengineering.registry.ModBlockEntities;
import com.willian.fieldengineering.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class AATurretBlock extends BaseEntityBlock {

    public static final MapCodec<AATurretBlock> CODEC =
            simpleCodec(AATurretBlock::new);

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public AATurretBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(
                this.stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos masterPos = context.getClickedPos();

        if (!hasEnoughSpace(level, masterPos)) {
            return null;
        }

        return this.defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) {
            return;
        }

        createMultiblockParts(level, pos);
    }

    private boolean hasEnoughSpace(Level level, BlockPos masterPos) {
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    BlockPos checkPos = masterPos.offset(x, y, z);

                    if (checkPos.getY() < level.getMinBuildHeight()) {
                        return false;
                    }

                    if (checkPos.getY() >= level.getMaxBuildHeight()) {
                        return false;
                    }

                    if (!level.getBlockState(checkPos).canBeReplaced()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void createMultiblockParts(Level level, BlockPos masterPos) {
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    BlockPos partPos = masterPos.offset(x, y, z);

                    BlockState partState =
                            ModBlocks.LAUNCHER_PART
                                    .defaultBlockState()
                                    .setValue(
                                            LauncherPartBlock.OFFSET_X,
                                            LauncherPartBlock.encodeHorizontalOffset(x)
                                    )
                                    .setValue(
                                            LauncherPartBlock.OFFSET_Y,
                                            y
                                    )
                                    .setValue(
                                            LauncherPartBlock.OFFSET_Z,
                                            LauncherPartBlock.encodeHorizontalOffset(z)
                                    );

                    level.setBlock(
                            partPos,
                            partState,
                            Block.UPDATE_ALL
                    );
                }
            }
        }
    }

    private void removeMultiblockParts(Level level, BlockPos masterPos) {
        for (int y = 0; y <= 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    BlockPos partPos = masterPos.offset(x, y, z);

                    if (level.getBlockState(partPos).is(ModBlocks.LAUNCHER_PART)) {
                        level.setBlock(
                                partPos,
                                Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_ALL
                        );
                    }
                }
            }
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeMultiblockParts(level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LauncherBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }

        return createTickerHelper(
                type,
                ModBlockEntities.LAUNCHER,
                LauncherBlockEntity::tick
        );
    }
}
