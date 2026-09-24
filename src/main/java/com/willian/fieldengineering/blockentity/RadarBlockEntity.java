package com.willian.fieldengineering.blockentity;

import com.willian.fieldengineering.registry.ModBlockEntities;
import com.willian.fieldengineering.target.TargetRules;
import com.willian.fieldengineering.tracking.RadarRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RadarBlockEntity extends BlockEntity {

    private static final double RANGE = 100.0;
    private static final double RANGE_SQR = RANGE * RANGE;
    private static final long SCAN_CACHE_TICKS = 20L;

    private final List<UUID> cachedTargetUuids = new ArrayList<>();

    private long lastScanTick = Long.MIN_VALUE;
    private boolean registered = false;

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

        radar.ensureRegistered(serverLevel);
    }

    public List<LivingEntity> getDetectedTargets(
            ServerLevel level
    ) {
        ensureRegistered(level);

        long gameTime = level.getGameTime();

        if (
                lastScanTick == Long.MIN_VALUE
                        || gameTime - lastScanTick >= SCAN_CACHE_TICKS
        ) {
            scan(level);
            lastScanTick = gameTime;
        }

        return resolveCachedTargets(level);
    }

    private void scan(ServerLevel level) {
        AABB area = new AABB(worldPosition).inflate(RANGE);

        List<LivingEntity> targets =
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        area,
                        entity -> isValidTarget(level, entity)
                );

        cachedTargetUuids.clear();

        LivingEntity nearestTarget = null;
        double nearestDistanceSqr = Double.MAX_VALUE;

        for (LivingEntity target : targets) {
            cachedTargetUuids.add(target.getUUID());

            double distanceSqr = target.distanceToSqr(
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5,
                    worldPosition.getZ() + 0.5
            );

            if (distanceSqr < nearestDistanceSqr) {
                nearestDistanceSqr = distanceSqr;
                nearestTarget = target;
            }
        }

        if (nearestTarget != null) {
            showTargetParticles(level, nearestTarget);
        }
    }

    private List<LivingEntity> resolveCachedTargets(
            ServerLevel level
    ) {
        if (cachedTargetUuids.isEmpty()) {
            return List.of();
        }

        List<LivingEntity> targets =
                new ArrayList<>(cachedTargetUuids.size());

        for (UUID targetUuid : cachedTargetUuids) {
            Entity entity = level.getEntity(targetUuid);

            if (
                    entity instanceof LivingEntity livingEntity
                            && isValidTarget(level, livingEntity)
            ) {
                targets.add(livingEntity);
            }
        }

        return targets;
    }

    private boolean isValidTarget(
            ServerLevel level,
            LivingEntity entity
    ) {
        if (!TargetRules.isValidTarget(level, entity)) {
            return false;
        }

        return entity.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5
        ) <= RANGE_SQR;
    }

    private void showTargetParticles(
            ServerLevel level,
            LivingEntity target
    ) {
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                target.getX(),
                target.getY() + target.getBbHeight(),
                target.getZ(),
                8,
                0.4,
                0.4,
                0.4,
                0.05
        );
    }

    private void ensureRegistered(ServerLevel level) {
        if (registered) {
            return;
        }

        RadarRegistry.register(level, worldPosition);
        registered = true;
    }

    @Override
    public void setRemoved() {
        if (
                registered
                        && getLevel() instanceof ServerLevel serverLevel
        ) {
            RadarRegistry.unregister(serverLevel, worldPosition);
        }

        registered = false;
        cachedTargetUuids.clear();
        lastScanTick = Long.MIN_VALUE;

        super.setRemoved();
    }
}
