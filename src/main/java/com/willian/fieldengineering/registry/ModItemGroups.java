package com.willian.fieldengineering.registry;

import com.willian.fieldengineering.FieldEngineering;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {

    public static final CreativeModeTab FIELD_ENGINEERING_TAB =
            Registry.register(
                    BuiltInRegistries.CREATIVE_MODE_TAB,
                    FieldEngineering.id("field_engineering"),

                    FabricItemGroup.builder()
                            .title(
                                    Component.translatable(
                                            "itemGroup.field-engineering"
                                    )
                            )
                            .icon(
                                    () -> new ItemStack(
                                            ModItems.AA_MISSILE
                                    )
                            )
                            .displayItems(
                                    (parameters, output) -> {
                                        output.accept(ModItems.AA_MISSILE);
                                        output.accept(ModBlocks.AA_TURRET_ITEM);
                                        output.accept(ModBlocks.RADAR_ITEM);
                                    }
                            )
                            .build()
            );

    public static void register() {
        FieldEngineering.LOGGER.info(
                "Registering item groups for {}",
                FieldEngineering.MOD_ID
        );
    }
}