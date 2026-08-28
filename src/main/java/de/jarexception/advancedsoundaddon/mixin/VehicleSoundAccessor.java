package de.jarexception.advancedsoundaddon.mixin;

import fr.dynamx.client.sound.VehicleSound;
import fr.dynamx.common.entities.BaseVehicleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VehicleSound.class, remap = false)
public interface VehicleSoundAccessor {
    @Accessor("vehicleEntity")
    BaseVehicleEntity<?> advancedsoundaddon$getVehicleEntity();
}
