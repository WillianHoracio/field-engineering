package com.willian.fieldengineering.tracking;

import com.willian.fieldengineering.blockentity.RadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class RadarRegistry {

    private static final Map<ServerLevel, Map<Long, RadarBlockEntity>> RADARS_BY_LEVEL =
            new WeakHashMap<>();

    private RadarRegistry() {
    }

    public static void register(
            ServerLevel level,
            RadarBlockEntity radar
    ) {
        RADARS_BY_LEVEL
                .computeIfAbsent(level, ignored -> new HashMap<>())
                .put(
                        radar.getBlockPos().asLong(),
                        radar
                );
    }

    public static void unregister(
            ServerLevel level,
            RadarBlockEntity radar
    ) {
        Map<Long, RadarBlockEntity> radars =
                RADARS_BY_LEVEL.get(level);

        if (radars == null) {
            return;
        }

        radars.remove(
                radar.getBlockPos().asLong(),
                radar
        );

        if (radars.isEmpty()) {
            RADARS_BY_LEVEL.remove(level);
        }
    }

    public static List<RadarBlockEntity> findWithinHorizontalRange(
            ServerLevel level,
            BlockPos origin,
            double range
    ) {
        Map<Long, RadarBlockEntity> radars =
                RADARS_BY_LEVEL.get(level);

        if (radars == null || radars.isEmpty()) {
            return List.of();
        }

        double rangeSqr = range * range;

        List<RadarBlockEntity> result =
                new ArrayList<>();

        Iterator<Map.Entry<Long, RadarBlockEntity>> iterator =
                radars.entrySet().iterator();

        while (iterator.hasNext()) {
            RadarBlockEntity radar =
                    iterator.next().getValue();

            if (radar == null || radar.isRemoved()) {
                iterator.remove();
                continue;
            }

            BlockPos radarPos =
                    radar.getBlockPos();

            double deltaX =
                    radarPos.getX() - origin.getX();

            double deltaZ =
                    radarPos.getZ() - origin.getZ();

            double horizontalDistanceSqr =
                    deltaX * deltaX
                            + deltaZ * deltaZ;

            if (horizontalDistanceSqr <= rangeSqr) {
                result.add(radar);
            }
        }

        if (radars.isEmpty()) {
            RADARS_BY_LEVEL.remove(level);
        }

        return result;
    }
}
