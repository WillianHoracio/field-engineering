package com.willian.fieldengineering.registry;

import com.willian.fieldengineering.FieldEngineering;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public class ModItems {

    public static final Item AA_MISSILE = Registry.register(
            BuiltInRegistries.ITEM,
            FieldEngineering.id("aa_missile"),
            new Item(new Item.Properties())
    );

    public static void register() {
        FieldEngineering.LOGGER.info("Registering items for {}", FieldEngineering.MOD_ID);
    }
}