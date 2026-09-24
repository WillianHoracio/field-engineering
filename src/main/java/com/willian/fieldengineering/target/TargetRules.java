package com.willian.fieldengineering.target;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Set;

public class TargetRules {

    private static final Set<ResourceLocation> ALLOWED_TARGETS = Set.of(
            ResourceLocation.fromNamespaceAndPath("minecraft", "pillager"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "vindicator"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "evoker"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "witch"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "ravager"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "wither"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "ender_dragon"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "warden"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "elder_guardian"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "piglin_brute")
    );

    private static final int AIR_TARGET_MIN_HEIGHT = 2;

    private TargetRules() {
    }

    public static boolean isValidTarget(
            ServerLevel level,
            LivingEntity entity
    ) {
        if (!entity.isAlive()) {
            return false;
        }

        if (!isAllowedType(entity)) {
            return false;
        }

        return isUnderOpenSky(level, entity);
    }

    public static boolean isAllowedType(
            LivingEntity entity
    ) {
        ResourceLocation entityId =
                BuiltInRegistries.ENTITY_TYPE.getKey(
                        entity.getType()
                );

        return ALLOWED_TARGETS.contains(entityId);
    }

    public static boolean isAirTarget(
            ServerLevel level,
            LivingEntity entity
    ) {
        return getHeightAboveGround(level, entity)
                >= AIR_TARGET_MIN_HEIGHT;
    }

    public static boolean isUnderOpenSky(
            ServerLevel level,
            LivingEntity entity
    ) {
        BlockPos entityPos = BlockPos.containing(
                entity.getX(),
                entity.getY() + entity.getBbHeight(),
                entity.getZ()
        );

        return level.canSeeSky(entityPos);
    }

    public static int getHeightAboveGround(
            ServerLevel level,
            LivingEntity entity
    ) {
        int x = entity.getBlockX();
        int z = entity.getBlockZ();

        /*
         * Valid targets must already be under open sky, so the motion-blocking
         * heightmap gives us the relevant surface directly. This replaces the
         * previous loop that could read up to 128 blocks for every check.
         */
        int surfaceY = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                x,
                z
        ) - 1;

        return Math.max(
                0,
                entity.getBlockY() - surfaceY
        );
    }
}
