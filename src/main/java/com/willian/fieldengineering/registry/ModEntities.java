package com.willian.fieldengineering.registry;

import com.willian.fieldengineering.FieldEngineering;
import com.willian.fieldengineering.entity.MissileEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {

    public static final EntityType<MissileEntity> MISSILE =
            Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    FieldEngineering.id("missile"),
                    EntityType.Builder
                            .<MissileEntity>of(
                                    MissileEntity::new,
                                    MobCategory.MISC
                            )
                            .sized(
                                    0.25f,
                                    0.25f
                            )
                            .clientTrackingRange(10)
                            .updateInterval(1)
                            .build(
                                    FieldEngineering.MOD_ID + ":missile"
                            )
            );

    public static void register() {
        FieldEngineering.LOGGER.info(
                "Registering entities for {}",
                FieldEngineering.MOD_ID
        );
    }
}
