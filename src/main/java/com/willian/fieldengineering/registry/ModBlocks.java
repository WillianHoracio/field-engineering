package com.willian.fieldengineering.registry;

import com.willian.fieldengineering.FieldEngineering;
import com.willian.fieldengineering.block.AATurretBlock;
import com.willian.fieldengineering.block.LauncherPartBlock;
import com.willian.fieldengineering.block.RadarBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static final Block AA_TURRET =
            Registry.register(
                    BuiltInRegistries.BLOCK,
                    FieldEngineering.id(
                            "aa_turret"
                    ),
                    new AATurretBlock(
                            BlockBehaviour.Properties
                                    .of()
                                    .strength(
                                            5.0f
                                    )
                    )
            );

    public static final Item AA_TURRET_ITEM =
            Registry.register(
                    BuiltInRegistries.ITEM,
                    FieldEngineering.id(
                            "aa_turret"
                    ),
                    new BlockItem(
                            AA_TURRET,
                            new Item.Properties()
                    )
            );

    /*
     * Parte técnica invisível do lançador.
     *
     * Não possui BlockItem.
     * Portanto não aparece no inventário.
     */
    public static final Block LAUNCHER_PART =
            Registry.register(
                    BuiltInRegistries.BLOCK,
                    FieldEngineering.id(
                            "launcher_part"
                    ),
                    new LauncherPartBlock(
                            BlockBehaviour.Properties
                                    .of()
                                    .strength(
                                            5.0f
                                    )
                                    .noOcclusion()
                    )
            );

    public static final Block RADAR =
            Registry.register(
                    BuiltInRegistries.BLOCK,
                    FieldEngineering.id(
                            "radar"
                    ),
                    new RadarBlock(
                            BlockBehaviour.Properties
                                    .of()
                                    .strength(
                                            5.0f
                                    )
                    )
            );

    public static final Item RADAR_ITEM =
            Registry.register(
                    BuiltInRegistries.ITEM,
                    FieldEngineering.id(
                            "radar"
                    ),
                    new BlockItem(
                            RADAR,
                            new Item.Properties()
                    )
            );

    public static void register() {
        FieldEngineering.LOGGER.info(
                "Registering blocks for {}",
                FieldEngineering.MOD_ID
        );
    }
}