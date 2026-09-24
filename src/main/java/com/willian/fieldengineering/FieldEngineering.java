package com.willian.fieldengineering;

import com.willian.fieldengineering.registry.ModBlockEntities;
import com.willian.fieldengineering.registry.ModBlocks;
import com.willian.fieldengineering.registry.ModEntities;
import com.willian.fieldengineering.registry.ModItemGroups;
import com.willian.fieldengineering.registry.ModItems;
import com.willian.fieldengineering.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FieldEngineering implements ModInitializer {

	public static final String MOD_ID =
			"field-engineering";

	public static final Logger LOGGER =
			LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();
		ModBlocks.register();
		ModBlockEntities.register();

		ModEntities.register();
		ModSounds.register();

		ModItemGroups.register();

		LOGGER.info(
				"Field Engineering initialized"
		);
	}

	public static ResourceLocation id(
			String path
	) {
		return ResourceLocation
				.fromNamespaceAndPath(
						MOD_ID,
						path
				);
	}
}