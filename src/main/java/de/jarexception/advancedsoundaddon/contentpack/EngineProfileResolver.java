package de.jarexception.advancedsoundaddon.contentpack;

import de.jarexception.advancedsoundaddon.sound.EngineLayout;
import de.jarexception.advancedsoundaddon.sound.EngineProfile;
import de.jarexception.advancedsoundaddon.sound.AirBrakeProfile;
import de.jarexception.advancedsoundaddon.sound.AfterfireProfile;
import de.jarexception.advancedsoundaddon.sound.BrakeSquealProfile;
import de.jarexception.advancedsoundaddon.sound.ReverseWarningProfile;
import de.jarexception.advancedsoundaddon.sound.RotorProfile;
import de.jarexception.advancedsoundaddon.sound.TireSquealProfile;
import de.jarexception.advancedsoundaddon.sound.HornProfile;
import de.jarexception.advancedsoundaddon.sound.IndicatorProfile;
import de.jarexception.advancedsoundaddon.sound.SirenProfile;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.contentpack.type.vehicle.ModularVehicleInfo;
import fr.dynamx.common.entities.BaseVehicleEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Resolves preset, engine overrides, then vehicle overrides in that order. */
public final class EngineProfileResolver {
    private static final Logger LOGGER = LogManager.getLogger("AdvancedSoundAddon/ContentPack");

    private EngineProfileResolver() {
    }

    public static EngineProfile resolve(BaseVehicleEntity<?> entity, BaseEngineInfo engineInfo) {
        EngineProfile profile = EngineProfile.forLayout(EngineLayout.I4);
        profile = applySafely(profile, find(engineInfo), "engine " + engineInfo.getFullName());

        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        if (vehicleInfo != null) {
            profile = applySafely(profile, find(vehicleInfo), "vehicle " + entity.getInfoName());
        }
        return profile;
    }

    public static RotorProfile resolveRotor(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) {
            return null;
        }
        try {
            return definition.resolveRotorProfile();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level RotorPreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return null;
        }
    }

    public static AirBrakeProfile resolveAirBrake(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) {
            return null;
        }
        try {
            return definition.resolveAirBrakeProfile();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level AirBrakePreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return null;
        }
    }

    public static ReverseWarningProfile resolveReverseWarning(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) {
            return null;
        }
        try {
            return definition.resolveReverseWarningProfile();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level ReverseWarningPreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return null;
        }
    }

    public static IndicatorProfile resolveIndicator(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) {
            return IndicatorProfile.defaultProfile();
        }
        try {
            return definition.resolveIndicatorProfile();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level IndicatorPreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return IndicatorProfile.defaultProfile();
        }
    }

    public static BrakeSquealProfile resolveBrakeSqueal(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) {
            return null;
        }
        try {
            return definition.resolveBrakeSquealProfile();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level BrakeSquealPreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return null;
        }
    }

    public static AfterfireProfile resolveAfterfire(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) {
            return null;
        }
        try {
            return definition.resolveAfterfireProfile();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level AfterfirePreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return null;
        }
    }

    public static TireSquealProfile resolveTireSqueal(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) {
            return TireSquealProfile.defaultProfile();
        }
        try {
            TireSquealProfile configured = definition.resolveTireSquealProfile();
            return configured == null ? TireSquealProfile.defaultProfile() : configured;
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level TireSquealPreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return TireSquealProfile.defaultProfile();
        }
    }

    public static HornProfile resolveHorn(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) return null;
        try {
            return definition.resolveHornProfile();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level HornPreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return null;
        }
    }

    public static SirenProfile resolveSiren(BaseVehicleEntity<?> entity) {
        ModularVehicleInfo vehicleInfo = entity.getPackInfo();
        AdvancedSoundInfo definition = vehicleInfo == null ? null : find(vehicleInfo);
        if (definition == null) return null;
        try {
            return definition.resolveSirenProfile();
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid vehicle-level SirenPreset on {}: {}",
                    entity.getInfoName(), exception.getMessage());
            return null;
        }
    }

    private static EngineProfile applySafely(EngineProfile inherited, AdvancedSoundInfo definition, String source) {
        if (definition == null) {
            return inherited;
        }
        try {
            return definition.applyTo(inherited);
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Invalid AdvancedSoundAddon block on {}; keeping inherited {} profile: {}",
                    source, inherited.getPresetName(), exception.getMessage());
            return inherited;
        }
    }

    private static AdvancedSoundInfo find(ISubInfoTypeOwner<?> owner) {
        if (owner == null) {
            return null;
        }
        for (ISubInfoType<?> property : owner.getSubProperties()) {
            if (property instanceof AdvancedSoundInfo) {
                return (AdvancedSoundInfo) property;
            }
        }
        return null;
    }
}
