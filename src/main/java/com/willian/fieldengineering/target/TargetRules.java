package com.willian.fieldengineering.target;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

public class TargetRules {

    /*
     * Apenas ameaças que justificam gastar um míssil.
     */
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

    /*
     * A partir desta distância vertical do chão,
     * tratamos o alvo como aéreo.
     */
    private static final int AIR_TARGET_MIN_HEIGHT = 2;

    /*
     * Distância máxima que procuramos pelo chão.
     *
     * Se não encontrarmos nada abaixo dentro desse limite,
     * consideramos o alvo aéreo.
     */
    private static final int GROUND_SEARCH_DEPTH = 128;

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
        int height = getHeightAboveGround(
                level,
                entity
        );

        return height >= AIR_TARGET_MIN_HEIGHT;
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
        BlockPos entityPos = BlockPos.containing(
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );

        for (
                int distance = 1;
                distance <= GROUND_SEARCH_DEPTH;
                distance++
        ) {
            BlockPos checkPos =
                    entityPos.below(distance);

            if (!level.getBlockState(checkPos).isAir()) {
                return distance;
            }
        }

        /*
         * Não encontramos chão.
         * Então certamente está alto o bastante
         * para ser tratado como alvo aéreo.
         */
        return GROUND_SEARCH_DEPTH;
    }
}