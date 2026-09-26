package com.willian.fieldengineering.client;

import com.willian.fieldengineering.client.render.MissileRenderer;
import com.willian.fieldengineering.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class FieldEngineeringClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(
				ModEntities.MISSILE,
				MissileRenderer::new
		);
	}
}
