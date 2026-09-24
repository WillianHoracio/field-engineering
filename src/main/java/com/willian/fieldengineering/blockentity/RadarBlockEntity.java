package com.willian.fieldengineering.blockentity;

import com.willian.fieldengineering.registry.ModBlockEntities;
import com.willian.fieldengineering.target.TargetRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class RadarBlockEntity extends BlockEntity {

    private static final double RANGE = 100.0;

    private static final int SCAN_INTERVAL_TICKS = 10;

    private UUID targetUuid;

    private int scanTimer = 0;

    public RadarBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.RADAR,
                pos,
                state
        );
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            RadarBlockEntity radar
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        radar.scanTimer++;

        if (radar.scanTimer < SCAN_INTERVAL_TICKS) {
            return;
        }

        radar.scanTimer = 0;

        LivingEntity target =
                radar.findTarget(
                        serverLevel,
                        pos
                );

        if (target == null) {
            radar.targetUuid = null;
            return;
        }

        radar.targetUuid =
                target.getUUID();

        radar.showTargetParticles(
                serverLevel,
                target
        );
    }

    private LivingEntity findTarget(
            ServerLevel level,
            BlockPos radarPos
    ) {
        AABB area =
                new AABB(radarPos)
                        .inflate(RANGE);

        List<LivingEntity> targets =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity ->
                                isValidTarget(
                                        level,
                                        radarPos,
                                        entity
                                )
                );

        return targets.stream()
                .min(
                        Comparator.comparingDouble(
                                entity ->
                                        entity.distanceToSqr(
                                                radarPos.getX() + 0.5,
                                                radarPos.getY() + 0.5,
                                                radarPos.getZ() + 0.5
                                        )
                        )
                )
                .orElse(null);
    }

    private boolean isValidTarget(
            ServerLevel level,
            BlockPos radarPos,
            LivingEntity entity
    ) {
        if (!TargetRules.isValidTarget(
                level,
                entity
        )) {
            return false;
        }

        double distanceSqr =
                entity.distanceToSqr(
                        radarPos.getX() + 0.5,
                        radarPos.getY() + 0.5,
                        radarPos.getZ() + 0.5
                );

        return distanceSqr <= RANGE * RANGE;
    }

    private void showTargetParticles(
            ServerLevel level,
            LivingEntity target
    ) {
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                target.getX(),
                target.getY()
                        + target.getBbHeight(),
                target.getZ(),
                8,
                0.4,
                0.4,
                0.4,
                0.05
        );
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public LivingEntity getTarget(
            ServerLevel level
    ) {
        if (targetUuid == null) {
            return null;
        }

        var entity =
                level.getEntity(targetUuid);

        if (
                entity instanceof LivingEntity livingEntity
                        && TargetRules.isValidTarget(
                        level,
                        livingEntity
                )
        ) {
            return livingEntity;
        }

        return null;
    }
}