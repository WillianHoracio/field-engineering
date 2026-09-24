package com.willian.fieldengineering.blockentity;

import com.willian.fieldengineering.FieldEngineering;
import com.willian.fieldengineering.entity.MissileEntity;
import com.willian.fieldengineering.registry.ModBlockEntities;
import com.willian.fieldengineering.registry.ModSounds;
import com.willian.fieldengineering.target.TargetRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class LauncherBlockEntity extends BlockEntity {

    private static final int PREPARATION_TICKS = 80;

    private static final int ASCENT_TICKS = 40;

    private static final int POST_ASCENT_WAIT_TICKS = 140;

    /*
     * Alvo terrestre:
     *
     * depois de travar a posição,
     * espera 1,1 segundo.
     */
    private static final int GROUND_LOCK_TO_DESCENT_TICKS = 22;

    /*
     * Alvo aéreo:
     *
     * o míssil começa a cair imediatamente
     * após o lock.
     */
    private static final int AIR_LOCK_TO_DESCENT_TICKS = 0;

    /*
     * 0,9 segundo de queda.
     */
    private static final int DESCENT_TICKS = 18;

    private static final int COOLDOWN_TICKS = 400;

    private static final double RADAR_SEARCH_RANGE = 128.0;

    private static final double DESCENT_HEIGHT = 90.0;

    private int preparationTimer = 0;

    private int cooldownTimer = 0;

    private int strikeTimer = -1;

    private boolean preparing = false;

    private RadarBlockEntity activeRadar;

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

    public LauncherBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlockEntities.LAUNCHER,
                pos,
                state
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
            launcher.tickStrikeSequence(
                    serverLevel
            );
        }

        if (launcher.preparing) {
            launcher.tickPreparation(
                    serverLevel
            );
            return;
        }

        if (launcher.cooldownTimer > 0) {
            launcher.cooldownTimer--;
            return;
        }

        launcher.tryStartPreparation(
                serverLevel,
                pos
        );
    }

    private void tryStartPreparation(
            ServerLevel level,
            BlockPos launcherPos
    ) {
        RadarBlockEntity radar =
                findNearestRadar(
                        level,
                        launcherPos
                );

        if (radar == null) {
            return;
        }

        LivingEntity target =
                radar.getTarget(level);

        if (target == null) {
            return;
        }

        if (!TargetRules.isValidTarget(
                level,
                target
        )) {
            return;
        }

        preparing = true;

        preparationTimer = 0;

        activeRadar = radar;

        activeTargetUuid =
                target.getUUID();

        updateLastKnownPosition(
                target
        );

        playStaticSound(level);

        FieldEngineering.LOGGER.info(
                "Launcher at {} started missile ignition against {}",
                launcherPos,
                target.getName()
                        .getString()
        );
    }

    private void tickPreparation(
            ServerLevel level
    ) {
        LivingEntity target =
                getActiveTarget(level);

        if (target != null) {
            updateLastKnownPosition(
                    target
            );
        }

        /*
         * Se morreu durante a preparação,
         * ainda conseguimos lançar contra a última posição.
         */
        if (
                target == null
                        && !hasLastTargetPosition
        ) {
            cancelPreparation();
            return;
        }

        preparationTimer++;

        spawnPreparationParticles(level);

        if (
                preparationTimer
                        < PREPARATION_TICKS
        ) {
            return;
        }

        launchAscendingMissile(level);
    }

    private void launchAscendingMissile(
            ServerLevel level
    ) {
        spawnLaunchBurst(level);

        playLaunchSound(level);

        spawnAscendingMissile(level);

        preparing = false;

        preparationTimer = 0;

        strikeTimer = 0;

        impactPositionLocked = false;

        descendingMissileSpawned = false;

        lockedTargetWasAirborne = false;

        cooldownTimer =
                COOLDOWN_TICKS;

        FieldEngineering.LOGGER.info(
                "Missile launched from {}",
                worldPosition
        );
    }

    private void tickStrikeSequence(
            ServerLevel level
    ) {
        /*
         * Até o momento do lock,
         * continuamos atualizando a posição.
         */
        if (!impactPositionLocked) {
            LivingEntity target =
                    getActiveTarget(level);

            if (target != null) {
                updateLastKnownPosition(
                        target
                );
            }
        }

        strikeTimer++;

        int lockTick =
                ASCENT_TICKS
                        + POST_ASCENT_WAIT_TICKS;

        if (
                !impactPositionLocked
                        && strikeTimer >= lockTick
        ) {
            lockImpactPosition(level);
        }

        if (!impactPositionLocked) {
            return;
        }

        int delayAfterLock =
                lockedTargetWasAirborne
                        ? AIR_LOCK_TO_DESCENT_TICKS
                        : GROUND_LOCK_TO_DESCENT_TICKS;

        int descentSpawnTick =
                lockTick
                        + delayAfterLock;

        if (
                !descendingMissileSpawned
                        && strikeTimer >= descentSpawnTick
        ) {
            descendingMissileSpawned = true;

            spawnDescendingMissile(level);

            playExplosionSequenceSound(level);
        }

        int sequenceEndTick =
                descentSpawnTick
                        + DESCENT_TICKS;

        if (
                descendingMissileSpawned
                        && strikeTimer >= sequenceEndTick
        ) {
            finishStrikeSequence();
        }
    }

    private void lockImpactPosition(
            ServerLevel level
    ) {
        if (!hasLastTargetPosition) {
            cancelStrikeSequence();
            return;
        }

        LivingEntity target =
                getActiveTarget(level);

        /*
         * Se ainda está vivo no momento do lock,
         * determinamos se ele é aéreo.
         *
         * Se morreu antes, usamos a última posição
         * e tratamos como terrestre por segurança.
         */
        lockedTargetWasAirborne =
                target != null
                        && TargetRules.isAirTarget(
                        level,
                        target
                );

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
            LivingEntity target
    ) {
        lastTargetX =
                target.getX();

        lastTargetY =
                target.getY()
                        + target.getBbHeight()
                        * 0.5;

        lastTargetZ =
                target.getZ();

        hasLastTargetPosition = true;
    }

    private void spawnDescendingMissile(
            ServerLevel level
    ) {
        double maxSpawnY =
                level.getMaxBuildHeight()
                        - 2;

        double spawnY =
                Math.min(
                        impactY
                                + DESCENT_HEIGHT,
                        maxSpawnY
                );

        MissileEntity missile =
                new MissileEntity(
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

        level.addFreshEntity(
                missile
        );

        FieldEngineering.LOGGER.info(
                "Descending missile spawned above X={} Y={} Z={}",
                impactX,
                impactY,
                impactZ
        );
    }

    private void playStaticSound(
            ServerLevel level
    ) {
        level.playSound(
                null,
                worldPosition,
                ModSounds.MISSILE_STATIC,
                SoundSource.BLOCKS,
                4.0f,
                1.0f
        );
    }

    private void playLaunchSound(
            ServerLevel level
    ) {
        level.playSound(
                null,
                worldPosition,
                ModSounds.MISSILE_LAUNCH,
                SoundSource.BLOCKS,
                6.0f,
                1.0f
        );
    }

    private void playExplosionSequenceSound(
            ServerLevel level
    ) {
        BlockPos soundPosition =
                BlockPos.containing(
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

    private void spawnAscendingMissile(
            ServerLevel level
    ) {
        double x =
                worldPosition.getX()
                        + 0.5;

        double y =
                worldPosition.getY()
                        + 1.2;

        double z =
                worldPosition.getZ()
                        + 0.5;

        MissileEntity missile =
                new MissileEntity(
                        level,
                        x,
                        y,
                        z
                );

        level.addFreshEntity(
                missile
        );
    }

    private void spawnPreparationParticles(
            ServerLevel level
    ) {
        double progress =
                (double) preparationTimer
                        / PREPARATION_TICKS;

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

        double x =
                worldPosition.getX()
                        + 0.5;

        double y =
                worldPosition.getY()
                        + 0.9;

        double z =
                worldPosition.getZ()
                        + 0.5;

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
                Math.max(
                        1,
                        particleCount / 2
                ),
                1.2,
                0.25,
                1.2,
                0.03
        );
    }

    private void spawnLaunchBurst(
            ServerLevel level
    ) {
        double x =
                worldPosition.getX()
                        + 0.5;

        double y =
                worldPosition.getY()
                        + 1.1;

        double z =
                worldPosition.getZ()
                        + 0.5;

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

    private LivingEntity getActiveTarget(
            ServerLevel level
    ) {
        if (activeTargetUuid == null) {
            return null;
        }

        Entity entity =
                level.getEntity(
                        activeTargetUuid
                );

        if (
                entity instanceof LivingEntity livingEntity
                        && livingEntity.isAlive()
        ) {
            return livingEntity;
        }

        return null;
    }

    private void cancelPreparation() {
        preparing = false;

        preparationTimer = 0;

        activeRadar = null;

        activeTargetUuid = null;

        hasLastTargetPosition = false;
    }

    private void cancelStrikeSequence() {
        strikeTimer = -1;

        impactPositionLocked = false;

        descendingMissileSpawned = false;

        activeRadar = null;

        activeTargetUuid = null;

        hasLastTargetPosition = false;
    }

    private void finishStrikeSequence() {
        strikeTimer = -1;

        impactPositionLocked = false;

        descendingMissileSpawned = false;

        activeRadar = null;

        activeTargetUuid = null;

        hasLastTargetPosition = false;
    }

    private RadarBlockEntity findNearestRadar(
            ServerLevel level,
            BlockPos launcherPos
    ) {
        AABB area =
                new AABB(launcherPos)
                        .inflate(
                                RADAR_SEARCH_RANGE
                        );

        List<RadarBlockEntity> radars =
                BlockEntitySearch.findBlockEntities(
                        level,
                        area,
                        RadarBlockEntity.class
                );

        return radars.stream()
                .filter(
                        radar ->
                                radar.getTarget(level)
                                        != null
                )
                .min(
                        Comparator.comparingDouble(
                                radar ->
                                        radar.getBlockPos()
                                                .distSqr(
                                                        launcherPos
                                                )
                        )
                )
                .orElse(null);
    }
}