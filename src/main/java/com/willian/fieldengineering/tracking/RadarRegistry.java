package com.willian.fieldengineering.tracking;

import com.willian.fieldengineering.blockentity.RadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map;

public final class RadarRegistry {

    private static final Map<ServerLevel, Set<Long>> RADARS_BY_LEVEL =
            new WeakHashMap<>();

    private RadarRegistry() {
    }

    public static void register(
            ServerLevel level,
            BlockPos radarPos
    ) {
        RADARS_BY_LEVEL
                .computeIfAbsent(level, ignored -> new HashSet<>())
                .add(radarPos.asLong());
    }

    public static void unregister(
            ServerLevel level,
            BlockPos radarPos
    ) {
        Set<Long> radars = RADARS_BY_LEVEL.get(level);

        if (radars == null) {
            return;
        }

        radars.remove(radarPos.asLong());

        if (radars.isEmpty()) {
            RADARS_BY_LEVEL.remove(level);
        }
    }

    public static List<RadarBlockEntity> findWithinHorizontalRange(
            ServerLevel level,
            BlockPos origin,
            double range
    ) {
        Set<Long> radars = RADARS_BY_LEVEL.get(level);

        if (radars == null || radars.isEmpty()) {
            return List.of();
        }

        double rangeSqr = range * range;
        List<RadarBlockEntity> result = new ArrayList<>();

        Iterator<Long> iterator = radars.iterator();

        while (iterator.hasNext()) {
            BlockPos radarPos = BlockPos.of(iterator.next());

            double deltaX = radarPos.getX() - origin.getX();
            double deltaZ = radarPos.getZ() - origin.getZ();

            double horizontalDistanceSqr =
                    deltaX * deltaX + deltaZ * deltaZ;

            if (horizontalDistanceSqr > rangeSqr) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(radarPos);

            if (blockEntity instanceof RadarBlockEntity radar && !radar.isRemoved()) {
                result.add(radar);
            } else {
                iterator.remove();
            }
        }

        if (radars.isEmpty()) {
            RADARS_BY_LEVEL.remove(level);
        }

        return result;
    }
}
