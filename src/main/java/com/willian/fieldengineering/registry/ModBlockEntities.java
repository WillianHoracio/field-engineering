package com.willian.fieldengineering.registry;

import com.willian.fieldengineering.FieldEngineering;
import com.willian.fieldengineering.blockentity.LauncherBlockEntity;
import com.willian.fieldengineering.blockentity.RadarBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {

    public static final BlockEntityType<RadarBlockEntity> RADAR =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    FieldEngineering.id("radar"),
                    BlockEntityType.Builder.of(
                            RadarBlockEntity::new,
                            ModBlocks.RADAR
                    ).build(null)
            );

    public static final BlockEntityType<LauncherBlockEntity> LAUNCHER =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    FieldEngineering.id("launcher"),
                    BlockEntityType.Builder.of(
                            LauncherBlockEntity::new,
                            ModBlocks.AA_TURRET
                    ).build(null)
            );

    public static void register() {
        FieldEngineering.LOGGER.info(
                "Registering block entities for {}",
                FieldEngineering.MOD_ID
        );
    }
}