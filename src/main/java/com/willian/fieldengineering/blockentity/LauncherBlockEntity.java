package com.willian.fieldengineering.blockentity;

import com.willian.fieldengineering.FieldEngineering;
import com.willian.fieldengineering.entity.MissileEntity;
import com.willian.fieldengineering.registry.ModBlockEntities;
import com.willian.fieldengineering.registry.ModSounds;
import com.willian.fieldengineering.target.TargetRules;
import com.willian.fieldengineering.tracking.RadarRegistry;
import com.willian.fieldengineering.tracking.TargetReservationRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LauncherBlockEntity extends BlockEntity {

    private static final int PREPARATION_TICKS = 80;
    private static final int ASCENT_TICKS = 40;
    private static final int POST_ASCENT_WAIT_TICKS = 140;
    private static final int GROUND_LOCK_TO_DESCENT_TICKS = 22;
    private static final int AIR_LOCK_TO_DESCENT_TICKS = 0;
    private static final int DESCENT_TICKS = 18;
    private static final int COOLDOWN_TICKS = 400;

    /*
     * Launchers ociosos reavaliam alvos a cada cinco segundos.
     * O offset inicial e derivado da posicao para evitar ciclos sincronizados.
     */
    private static final int ACQUISITION_INTERVAL_TICKS = 100;

    private static final double RADAR_SEARCH_RANGE = 128.0;
    private static final double DESCENT_HEIGHT = 90.0;

    private int preparationTimer = 0;
    private int cooldownTimer = 0;
    private int strikeTimer = -1;
    private int acquisitionTimer;

    private boolean preparing = false;
    private UUID activeTargetUuid;

    private boolean impactPositionLocked = false;
    private boolean lockedTargetWasAirborne = false;
    private boolean descendingMissileSpawned = false;

    private double impactX;
    private double impactY;
    private double impactZ;

    private double lastTargetX;
    private double lastTargetY;
    private double lastTargetZ;

    private boolean hasLastTargetPosition = false;
    private boolean lastTargetWasAirborne = false;

    public LauncherBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.LAUNCHER,
                pos,
                state
        );

        acquisitionTimer = Math.floorMod(
                Long.hashCode(pos.asLong()),
                ACQUISITION_INTERVAL_TICKS
        );
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            LauncherBlockEntity launcher
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (launcher.strikeTimer >= 0) {
            launcher.tickStrikeSequence(serverLevel);
        }

        if (launcher.preparing) {
            launcher.tickPreparation(serverLevel);
            return;
        }

        if (launcher.cooldownTimer > 0) {
            launcher.cooldownTimer--;
            return;
        }

        if (launcher.acquisitionTimer > 0) {
            launcher.acquisitionTimer--;
            return;
        }

        launcher.acquisitionTimer = ACQUISITION_INTERVAL_TICKS;
        launcher.tryStartPreparation(serverLevel, pos);
    }

    private void tryStartPreparation(
            ServerLevel level,
            BlockPos launcherPos
    ) {
        LivingEntity target = findBestAvailableTarget(
                level,
                launcherPos
        );

        if (target == null) {
            return;
        }

        if (
                !TargetReservationRegistry.tryReserve(
                        level,
                        target.getUUID(),
                        launcherPos
                )
        ) {
            return;
        }

        preparing = true;
        preparationTimer = 0;
        activeTargetUuid = target.getUUID();

        updateLastKnownPosition(level, target);
        playStaticSound(level);

        FieldEngineering.LOGGER.info(
                "Launcher at {} started missile ignition against {}",
                launcherPos,
                target.getName().getString()
        );
    }

    private LivingEntity findBestAvailableTarget(
            ServerLevel level,
            BlockPos launcherPos
    ) {
        List<RadarBlockEntity> radars =
                RadarRegistry.findWithinHorizontalRange(
                        level,
                        launcherPos,
                        RADAR_SEARCH_RANGE
                );

        if (radars.isEmpty()) {
            return null;
        }

        Set<UUID> seenTargets = new HashSet<>();

        LivingEntity nearestAirTarget = null;
        double nearestAirDistanceSqr = Double.MAX_VALUE;

        LivingEntity nearestGroundTarget = null;
        double nearestGroundDistanceSqr = Double.MAX_VALUE;

        double launcherX = launcherPos.getX() + 0.5;
        double launcherY = launcherPos.getY() + 0.5;
        double launcherZ = launcherPos.getZ() + 0.5;

        for (RadarBlockEntity radar : radars) {
            for (LivingEntity target : radar.getDetectedTargets(level)) {
                UUID targetUuid = target.getUUID();

                if (!seenTargets.add(targetUuid)) {
                    continue;
                }

                if (!TargetRules.isValidTarget(level, target)) {
                    continue;
                }

                if (
                        TargetReservationRegistry.isReservedByAnother(
                                level,
                                targetUuid,
                                launcherPos
                        )
                ) {
                    continue;
                }

                double distanceSqr = target.distanceToSqr(
                        launcherX,
                        launcherY,
                        launcherZ
                );

                if (TargetRules.isAirTarget(level, target)) {
                    if (distanceSqr < nearestAirDistanceSqr) {
                        nearestAirDistanceSqr = distanceSqr;
                        nearestAirTarget = target;
                    }
                } else if (distanceSqr < nearestGroundDistanceSqr) {
                    nearestGroundDistanceSqr = distanceSqr;
                    nearestGroundTarget = target;
                }
            }
        }

        return nearestAirTarget != null
                ? nearestAirTarget
                : nearestGroundTarget;
    }

    private void tickPreparation(ServerLevel level) {
        LivingEntity target = getActiveTarget(level);

        if (target != null) {
            updateLastKnownPosition(level, target);
        }

        if (target == null && !hasLastTargetPosition) {
            cancelPreparation();
            return;
        }

        preparationTimer++;
        spawnPreparationParticles(level);

        if (preparationTimer < PREPARATION_TICKS) {
            return;
        }

        launchAscendingMissile(level);
    }

    private void launchAscendingMissile(ServerLevel level) {
        spawnLaunchBurst(level);
        playLaunchSound(level);
        spawnAscendingMissile(level);

        preparing = false;
        preparationTimer = 0;
        strikeTimer = 0;
        impactPositionLocked = false;
        descendingMissileSpawned = false;
        lockedTargetWasAirborne = false;
        cooldownTimer = COOLDOWN_TICKS;

        FieldEngineering.LOGGER.info(
                "Missile launched from {}",
                worldPosition
        );
    }

    private void tickStrikeSequence(ServerLevel level) {
        if (!impactPositionLocked) {
            LivingEntity target = getActiveTarget(level);

            if (target != null) {
                updateLastKnownPosition(level, target);
            }
        }

        strikeTimer++;

        int lockTick = ASCENT_TICKS + POST_ASCENT_WAIT_TICKS;

        if (!impactPositionLocked && strikeTimer >= lockTick) {
            lockImpactPosition();
        }

        if (!impactPositionLocked) {
            return;
        }

        int delayAfterLock = lockedTargetWasAirborne
                ? AIR_LOCK_TO_DESCENT_TICKS
                : GROUND_LOCK_TO_DESCENT_TICKS;

        int descentSpawnTick = lockTick + delayAfterLock;

        if (!descendingMissileSpawned && strikeTimer >= descentSpawnTick) {
            descendingMissileSpawned = true;
            spawnDescendingMissile(level);
            playExplosionSequenceSound(level);
        }

        int sequenceEndTick = descentSpawnTick + DESCENT_TICKS;

        if (descendingMissileSpawned && strikeTimer >= sequenceEndTick) {
            finishStrikeSequence();
        }
    }

    private void lockImpactPosition() {
        if (!hasLastTargetPosition) {
            cancelStrikeSequence();
            return;
        }

        lockedTargetWasAirborne = lastTargetWasAirborne;

        impactX = lastTargetX;
        impactY = lastTargetY;
        impactZ = lastTargetZ;
        impactPositionLocked = true;

        FieldEngineering.LOGGER.info(
                "Impact locked at X={} Y={} Z={} | airborne={}",
                impactX,
                impactY,
                impactZ,
                lockedTargetWasAirborne
        );
    }

    private void updateLastKnownPosition(
            ServerLevel level,
            LivingEntity target
    ) {
        lastTargetX = target.getX();
        lastTargetY = target.getY() + target.getBbHeight() * 0.5;
        lastTargetZ = target.getZ();
        lastTargetWasAirborne = TargetRules.isAirTarget(level, target);
        hasLastTargetPosition = true;
    }

    private void spawnDescendingMissile(ServerLevel level) {
        double maxSpawnY = level.getMaxBuildHeight() - 2;
        double spawnY = Math.min(
                impactY + DESCENT_HEIGHT,
                maxSpawnY
        );

        MissileEntity missile = new MissileEntity(
                level,
                impactX,
                spawnY,
                impactZ
        );

        missile.configureDescending(
                impactX,
                spawnY,
                impactZ,
                impactX,
                impactY,
                impactZ
        );

        level.addFreshEntity(missile);

        FieldEngineering.LOGGER.info(
                "Descending missile spawned above X={} Y={} Z={}",
                impactX,
                impactY,
                impactZ
        );
    }

    private void playStaticSound(ServerLevel level) {
        level.playSound(
                null,
                worldPosition,
                ModSounds.MISSILE_STATIC,
                SoundSource.BLOCKS,
                4.0f,
                1.0f
        );
    }

    private void playLaunchSound(ServerLevel level) {
        level.playSound(
                null,
                worldPosition,
                ModSounds.MISSILE_LAUNCH,
                SoundSource.BLOCKS,
                6.0f,
                1.0f
        );
    }

    private void playExplosionSequenceSound(ServerLevel level) {
        BlockPos soundPosition = BlockPos.containing(
                impactX,
                impactY,
                impactZ
        );

        level.playSound(
                null,
                soundPosition,
                ModSounds.MISSILE_EXPLOSION,
                SoundSource.HOSTILE,
                8.0f,
                1.0f
        );
    }

    private void spawnAscendingMissile(ServerLevel level) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.2;
        double z = worldPosition.getZ() + 0.5;

        MissileEntity missile = new MissileEntity(
                level,
                x,
                y,
                z
        );

        level.addFreshEntity(missile);
    }

    private void spawnPreparationParticles(ServerLevel level) {
        double progress = (double) preparationTimer / PREPARATION_TICKS;

        int particleCount;

        if (progress < 0.25) {
            particleCount = 8;
        } else if (progress < 0.50) {
            particleCount = 18;
        } else if (progress < 0.75) {
            particleCount = 35;
        } else {
            particleCount = 60;
        }

        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.9;
        double z = worldPosition.getZ() + 0.5;

        level.sendParticles(
                ParticleTypes.FIREWORK,
                x,
                y,
                z,
                particleCount,
                0.9,
                0.35,
                0.9,
                0.06
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                Math.max(1, particleCount / 2),
                1.2,
                0.25,
                1.2,
                0.03
        );
    }

    private void spawnLaunchBurst(ServerLevel level) {
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.1;
        double z = worldPosition.getZ() + 0.5;

        level.sendParticles(
                ParticleTypes.FIREWORK,
                x,
                y,
                z,
                220,
                1.2,
                0.7,
                1.2,
                0.18
        );

        level.sendParticles(
                ParticleTypes.SMOKE,
                x,
                y,
                z,
                140,
                1.4,
                0.4,
                1.4,
                0.08
        );

        level.sendParticles(
                ParticleTypes.CAMPFIRE_COSY_SMOKE,
                x,
                y,
                z,
                70,
                1.0,
                0.3,
                1.0,
                0.04
        );

        level.sendParticles(
                ParticleTypes.FLAME,
                x,
                y,
                z,
                45,
                0.6,
                0.4,
                0.6,
                0.10
        );
    }

    private LivingEntity getActiveTarget(ServerLevel level) {
        if (activeTargetUuid == null) {
            return null;
        }

        Entity entity = level.getEntity(activeTargetUuid);

        if (
                entity instanceof LivingEntity livingEntity
                        && livingEntity.isAlive()
        ) {
            return livingEntity;
        }

        return null;
    }

    private void cancelPreparation() {
        releaseActiveReservation();

        preparing = false;
        preparationTimer = 0;
        activeTargetUuid = null;
        hasLastTargetPosition = false;
        lastTargetWasAirborne = false;
    }

    private void cancelStrikeSequence() {
        releaseActiveReservation();

        strikeTimer = -1;
        impactPositionLocked = false;
        descendingMissileSpawned = false;
        activeTargetUuid = null;
        hasLastTargetPosition = false;
        lastTargetWasAirborne = false;
    }

    private void finishStrikeSequence() {
        releaseActiveReservation();

        strikeTimer = -1;
        impactPositionLocked = false;
        descendingMissileSpawned = false;
        activeTargetUuid = null;
        hasLastTargetPosition = false;
        lastTargetWasAirborne = false;
    }

    private void releaseActiveReservation() {
        if (
                activeTargetUuid != null
                        && getLevel() instanceof ServerLevel serverLevel
        ) {
            TargetReservationRegistry.release(
                    serverLevel,
                    activeTargetUuid,
                    worldPosition
            );
        }
    }

    @Override
    public void setRemoved() {
        if (getLevel() instanceof ServerLevel serverLevel) {
            TargetReservationRegistry.releaseAllForLauncher(
                    serverLevel,
                    worldPosition
            );
        }

        super.setRemoved();
    }
}
