package com.willian.fieldengineering.block;

import com.mojang.serialization.MapCodec;
import com.willian.fieldengineering.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class LauncherPartBlock extends Block {

    public static final MapCodec<LauncherPartBlock> CODEC =
            simpleCodec(LauncherPartBlock::new);

    public LauncherPartBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        /*
         * Esse bloco existe apenas para reservar
         * fisicamente o espaço da torre.
         *
         * O modelo visual inteiro é renderizado
         * pelo bloco central.
         */
        return RenderShape.INVISIBLE;
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
                            pos
                    );

            if (masterPos != null) {
                /*
                 * Quebrar qualquer parte quebra
                 * o bloco principal.
                 *
                 * Se não estiver no criativo,
                 * o bloco principal dropa o item.
                 */
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
            BlockPos partPos
    ) {
        /*
         * O master sempre fica:
         *
         * - no centro do 3x3;
         * - na camada inferior.
         *
         * Como não armazenamos BlockEntity
         * nas partes auxiliares, procuramos
         * o master nessa pequena região.
         */

        for (int yOffset = -1; yOffset <= 0; yOffset++) {

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