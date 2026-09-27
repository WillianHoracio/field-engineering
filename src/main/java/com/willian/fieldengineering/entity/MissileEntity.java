package com.willian.fieldengineering.entity;

import com.willian.fieldengineering.registry.ModEntities;
import com.willian.fieldengineering.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MissileEntity extends ThrowableItemProjectile {

    private static final double ASCENT_SPEED = 1.8;
    private static final double MAX_RENDER_DISTANCE = 1024.0;

    // 0,9 segundos.
    private static final int DESCENT_TICKS = 18;

    private boolean descending = false;
    private double ascentCeilingY = Double.POSITIVE_INFINITY;

    private double impactX;
    private double impactY;
    private double impactZ;

    private double descentStartX;
    private double descentStartY;
    private double descentStartZ;

    public MissileEntity(
            EntityType<? extends ThrowableItemProjectile> type,
            Level level
    ) {
        super(type, level);
        configureVisual();
    }

    public MissileEntity(
            Level level,
            double x,
            double y,
            double z
    ) {
        super(ModEntities.MISSILE, x, y, z, level);

        configureVisual();
        this.descending = false;
        this.setNoGravity(true);
        this.setDeltaMovement(0.0, ASCENT_SPEED, 0.0);
    }

    private void configureVisual() {
        this.setItem(new ItemStack(ModItems.AA_MISSILE));
    }

    public void configureAscendingCeiling(double maxY) {
        this.ascentCeilingY = maxY;
    }

    public void configureDescending(
            double startX,
            double startY,
            double startZ,
            double impactX,
            double impactY,
            double impactZ
    ) {
        this.descending = true;

        double highestSpawnY = level().getMaxBuildHeight() - 2.0;

        this.descentStartX = startX;
        this.descentStartY = Math.max(startY, highestSpawnY);
        this.descentStartZ = startZ;

        this.impactX = impactX;
        this.impactY = impactY;
        this.impactZ = impactZ;

        this.setPos(descentStartX, descentStartY, descentStartZ);
        this.setNoGravity(true);
        updateDescendingVelocity();
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.AA_MISSILE;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance <= MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel) {
            if (descending) {
                updateDescendingVelocity();
            } else {
                updateAscendingVelocity();
            }
        }

        super.tick();

        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (descending) {
            tickDescending(serverLevel);
        } else {
            tickAscending(serverLevel);
        }
    }

    private void updateAscendingVelocity() {
        this.setNoGravity(true);

        if (getY() >= ascentCeilingY) {
            this.setPos(getX(), ascentCeilingY, getZ());
            this.setDeltaMovement(0.0, 0.0, 0.0);
            return;
        }

        double remainingHeight = ascentCeilingY - getY();
        double velocityY = Math.min(ASCENT_SPEED, remainingHeight);

        this.setDeltaMovement(0.0, velocityY, 0.0);
    }

    private void tickAscending(ServerLevel level) {
        if (getY() > ascentCeilingY) {
            this.setPos(getX(), ascentCeilingY, getZ());
            this.setDeltaMovement(0.0, 0.0, 0.0);
        }

        spawnTrail(level);
    }

    private void tickDescending(ServerLevel level) {
        spawnTrail(level);

        if (this.tickCount < DESCENT_TICKS) {
            return;
        }

        this.setPos(impactX, impactY, impactZ);
        explode(level);
    }

    private void updateDescendingVelocity() {
        double velocityX = (impactX - descentStartX) / DESCENT_TICKS;
        double velocityY = (impactY - descentStartY) / DESCENT_TICKS;
        double velocityZ = (impactZ - descentStartZ) / DESCENT_TICKS;

        this.setNoGravity(true);
        this.setDeltaMovement(velocityX, velocityY, velocityZ);
    }

    private void explode(ServerLevel level) {
        level.explode(
                this,
                impactX,
                impactY,
                impactZ,
                4.0f,
                false,
                Level.ExplosionInteraction.NONE
        );

        this.discard();
    }

    private void spawnTrail(ServerLevel level) {
        double x = getX();
        double y = getY();
        double z = getZ();

        level.sendParticles(
                ParticleTypes.FIREWORK,
                x,
                y,
                z,
                40,
                0.24,
                0.32,
                0.24,
                0.045
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                30,
                0.30,
                0.38,
                0.30,
                0.03
        );

        level.sendParticles(
                ParticleTypes.FLAME,
                x,
                y,
                z,
                10,
                0.12,
                0.18,
                0.12,
                0.035
        );
    }
}
