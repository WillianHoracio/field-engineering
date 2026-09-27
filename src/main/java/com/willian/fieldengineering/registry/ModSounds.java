package com.willian.fieldengineering.registry;

import com.willian.fieldengineering.FieldEngineering;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {

    public static final SoundEvent MISSILE_STATIC =
            registerSound("missile_static");

    public static final SoundEvent MISSILE_LAUNCH =
            registerSound("missile_launch");

    public static final SoundEvent MISSILE_EXPLOSION =
            registerSound("missile_explosion");

    public static final SoundEvent RADAR_BEEP =
            registerSound("radar_beep");

    private static SoundEvent registerSound(String name) {
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                FieldEngineering.id(name),
                SoundEvent.createVariableRangeEvent(
                        FieldEngineering.id(name)
                )
        );
    }

    public static void register() {
        FieldEngineering.LOGGER.info(
                "Registering sounds for {}",
                FieldEngineering.MOD_ID
        );
    }
}
