package de.jarexception.advancedsoundaddon.contentpack;

import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;

/** Registers vehicle-level AdvancedSoundAddon blocks with DynamX loaders. */
@RegisteredSubInfoType(
        name = "AdvancedSoundAddon",
        registries = {
                SubInfoTypeRegistries.WHEELED_VEHICLES,
                SubInfoTypeRegistries.HELICOPTER
        },
        strictName = true
)
@SuppressWarnings("rawtypes")
public final class VehicleAdvancedSoundInfo extends AdvancedSoundInfo {
    public VehicleAdvancedSoundInfo(ISubInfoTypeOwner owner) {
        super(owner);
    }
}
