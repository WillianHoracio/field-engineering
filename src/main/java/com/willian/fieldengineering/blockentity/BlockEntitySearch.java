package com.willian.fieldengineering.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public class BlockEntitySearch {

    public static <T extends BlockEntity> List<T> findBlockEntities(
            ServerLevel level,
            AABB area,
            Class<T> type
    ) {
        List<T> result = new ArrayList<>();

        int minX = (int) Math.floor(area.minX);
        int minY = (int) Math.floor(area.minY);
        int minZ = (int) Math.floor(area.minZ);

        int maxX = (int) Math.ceil(area.maxX);
        int maxY = (int) Math.ceil(area.maxY);
        int maxZ = (int) Math.ceil(area.maxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockEntity blockEntity = level.getBlockEntity(
                            new BlockPos(x, y, z)
                    );

                    if (type.isInstance(blockEntity)) {
                        result.add(type.cast(blockEntity));
                    }
                }
            }
        }

        return result;
    }
}