package de.jarexception.advancedsoundaddon.contentpack;

import de.jarexception.advancedsoundaddon.AdvancedSoundAddon;
import de.jarexception.advancedsoundaddon.signal.ModuleListBuilderCompat;
import de.jarexception.advancedsoundaddon.sound.EnginePowertrain;
import de.jarexception.advancedsoundaddon.sound.EngineProfile;
import de.jarexception.advancedsoundaddon.sound.AirBrakeProfile;
import de.jarexception.advancedsoundaddon.sound.AfterfireProfile;
import de.jarexception.advancedsoundaddon.sound.BrakeSquealProfile;
import de.jarexception.advancedsoundaddon.sound.RotorProfile;
import de.jarexception.advancedsoundaddon.sound.TireSquealProfile;
import de.jarexception.advancedsoundaddon.sound.HornProfile;
import de.jarexception.advancedsoundaddon.sound.SirenProfile;
import de.jarexception.advancedsoundaddon.signal.VehicleSignalModule;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoType;
import fr.dynamx.api.contentpack.object.subinfo.ISubInfoTypeOwner;
import fr.dynamx.api.contentpack.registry.DefinitionType;
import fr.dynamx.api.contentpack.registry.PackFileProperty;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import fr.dynamx.api.entities.modules.ModuleListBuilder;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.PackPhysicsEntity;

import java.util.Locale;

/** Defines procedural vehicle audio with inheritable engine and vehicle fields. */
@RegisteredSubInfoType(
        name = "AdvancedSoundAddon",
        registries = {SubInfoTypeRegistries.CAR_ENGINES},
        strictName = true
)
@SuppressWarnings({"rawtypes", "unchecked"})
public class AdvancedSoundInfo implements ISubInfoType {
    private final ISubInfoTypeOwner owner;

    @PackFileProperty(configNames = {"Preset"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String preset;

    @PackFileProperty(configNames = {"RotorPreset"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String rotorPreset;

    @PackFileProperty(configNames = {"AirBrakePreset", "PneumaticPreset"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String airBrakePreset;

    @PackFileProperty(configNames = {"BrakeSquealPreset", "BrakeNoisePreset"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String brakeSquealPreset;

    @PackFileProperty(configNames = {"AfterfirePreset", "OverrunPreset"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String afterfirePreset;

    @PackFileProperty(configNames = {"TireSquealPreset", "TyreSquealPreset", "SkidSoundPreset"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String tireSquealPreset;

    @PackFileProperty(configNames = {"HornPreset"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String hornPreset;

    @PackFileProperty(configNames = {"HornSource"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String hornSource;

    @PackFileProperty(configNames = {"HornFrequenciesHz"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT_ARRAY, required = false)
    float[] hornFrequenciesHz;

    @PackFileProperty(configNames = {"HornRelativeGains"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT_ARRAY, required = false)
    float[] hornRelativeGains;

    @PackFileProperty(configNames = {"HornAttackSeconds"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float hornAttackSeconds = Float.NaN;

    @PackFileProperty(configNames = {"HornHoldSeconds", "HornDurationSeconds"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float hornHoldSeconds = Float.NaN;

    @PackFileProperty(configNames = {"HornReleaseSeconds"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float hornReleaseSeconds = Float.NaN;

    @PackFileProperty(configNames = {"HornGain", "HornVolume"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float hornGain = Float.NaN;

    @PackFileProperty(configNames = {"HornBrightness"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float hornBrightness = Float.NaN;

    @PackFileProperty(configNames = {"HornRasp"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float hornRasp = Float.NaN;

    @PackFileProperty(configNames = {"HornAudibleDistance"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float hornAudibleDistance = Float.NaN;

    @PackFileProperty(configNames = {"SirenPreset"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String sirenPreset;

    @PackFileProperty(configNames = {"SirenSource"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String sirenSource;

    @PackFileProperty(configNames = {"SirenPattern"}, type = DefinitionType.DynamXDefinitionTypes.STRING, required = false)
    String sirenPattern;

    @PackFileProperty(configNames = {"SirenFrequenciesHz"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT_ARRAY, required = false)
    float[] sirenFrequenciesHz;

    @PackFileProperty(configNames = {"SirenSecondaryFrequenciesHz"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT_ARRAY, required = false)
    float[] sirenSecondaryFrequenciesHz;

    @PackFileProperty(configNames = {"SirenDurationsSeconds"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT_ARRAY, required = false)
    float[] sirenDurationsSeconds;

    @PackFileProperty(configNames = {"SirenHarmonics"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT_ARRAY, required = false)
    float[] sirenHarmonics;

    @PackFileProperty(configNames = {"SirenGain", "SirenVolume"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float sirenGain = Float.NaN;

    @PackFileProperty(configNames = {"SirenRasp"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float sirenRasp = Float.NaN;

    @PackFileProperty(configNames = {"SirenFlutterHz"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float sirenFlutterHz = Float.NaN;

    @PackFileProperty(configNames = {"SirenFlutterDepth"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float sirenFlutterDepth = Float.NaN;

    @PackFileProperty(configNames = {"SirenSubharmonicGain"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float sirenSubharmonicGain = Float.NaN;

    @PackFileProperty(configNames = {"SirenAudibleDistance"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float sirenAudibleDistance = Float.NaN;

    @PackFileProperty(configNames = {"FiringOrder"}, type = DefinitionType.DynamXDefinitionTypes.INT_ARRAY, required = false)
    int[] firingOrder;

    @PackFileProperty(configNames = {"FiringBanks"}, type = DefinitionType.DynamXDefinitionTypes.INT_ARRAY, required = false)
    int[] firingBanks;

    @PackFileProperty(configNames = {"IdleRPM", "IdleRpm"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float idleRpm = Float.NaN;

    @PackFileProperty(configNames = {"AcousticMaxRPM", "AcousticMaxRpm"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float acousticMaxRpm = Float.NaN;

    @PackFileProperty(configNames = {"OutputGain", "Volume"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float outputGain = Float.NaN;

    @PackFileProperty(configNames = {"StarterRPM", "StarterRpm"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float starterRpm = Float.NaN;

    @PackFileProperty(configNames = {"StartDurationSeconds"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float startDurationSeconds = Float.NaN;

    @PackFileProperty(configNames = {"StopDurationSeconds"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float stopDurationSeconds = Float.NaN;

    @PackFileProperty(configNames = {"ExhaustResonanceHz"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float exhaustResonanceHz = Float.NaN;

    @PackFileProperty(configNames = {"IntakeResonanceHz"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float intakeResonanceHz = Float.NaN;

    @PackFileProperty(configNames = {"ExhaustGain"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float exhaustGain = Float.NaN;

    @PackFileProperty(configNames = {"IntakeGain"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float intakeGain = Float.NaN;

    @PackFileProperty(configNames = {"MechanicalGain"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float mechanicalGain = Float.NaN;

    @PackFileProperty(configNames = {"PulseSharpness"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float pulseSharpness = Float.NaN;

    @PackFileProperty(configNames = {"InductionCharacter"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float inductionCharacter = Float.NaN;

    @PackFileProperty(configNames = {"MechanicalBrightness"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float mechanicalBrightness = Float.NaN;

    @PackFileProperty(configNames = {"PrimaryBankDelayMillis"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float primaryBankDelayMillis = Float.NaN;

    @PackFileProperty(configNames = {"SecondaryBankDelayMillis"}, type = DefinitionType.DynamXDefinitionTypes.FLOAT, required = false)
    float secondaryBankDelayMillis = Float.NaN;

    public AdvancedSoundInfo(ISubInfoTypeOwner owner) {
        this.owner = owner;
    }

    @Override
    public void appendTo(ISubInfoTypeOwner owner) {
        owner.addSubProperty(this);
    }

    @Override
    public ISubInfoTypeOwner getOwner() {
        return owner;
    }

    @Override
    public void addModules(PackPhysicsEntity entity, ModuleListBuilder modules) {
        if (!(entity instanceof BaseVehicleEntity)) {
            return;
        }
        BaseVehicleEntity<?> vehicle = (BaseVehicleEntity<?>) entity;
        boolean horn = hasValue(hornPreset);
        boolean siren = hasValue(sirenPreset);
        if ((horn || siren) && AdvancedSoundAddon.shouldAttachSignalModule(vehicle)
                && !modules.hasModuleOfClass(VehicleSignalModule.class)) {
            ModuleListBuilderCompat.add(modules, new VehicleSignalModule(vehicle, horn, siren));
        }
    }

    @Override
    public String getName() {
        return "AdvancedSoundAddon of " + (owner == null ? "unknown" : owner.getName());
    }

    @Override
    public String getPackName() {
        return owner == null ? "unknown" : owner.getPackName();
    }

    EngineProfile applyTo(EngineProfile inherited) {
        EngineProfile result = inherited == null
                ? EngineProfile.forPreset("I4") : inherited;
        if (preset != null && !preset.trim().isEmpty()) {
            String normalized = preset.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            result = EngineProfile.forPreset(normalized);
        }
        result = result.withOverrides(
                configured(idleRpm), configured(acousticMaxRpm), configured(outputGain),
                configured(starterRpm),
                configured(startDurationSeconds), configured(stopDurationSeconds),
                configured(exhaustResonanceHz), configured(intakeResonanceHz),
                configured(exhaustGain), configured(intakeGain), configured(mechanicalGain),
                configured(pulseSharpness), configured(inductionCharacter),
                configured(mechanicalBrightness), configured(primaryBankDelayMillis),
                configured(secondaryBankDelayMillis));
        return applyFiringPattern(result);
    }

    RotorProfile resolveRotorProfile() {
        if (rotorPreset == null || rotorPreset.trim().isEmpty()) {
            return null;
        }
        String normalized = rotorPreset.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return RotorProfile.forPreset(normalized);
    }

    AirBrakeProfile resolveAirBrakeProfile() {
        if (airBrakePreset == null || airBrakePreset.trim().isEmpty()) {
            return null;
        }
        String normalized = airBrakePreset.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return AirBrakeProfile.forPreset(normalized);
    }

    BrakeSquealProfile resolveBrakeSquealProfile() {
        if (brakeSquealPreset == null || brakeSquealPreset.trim().isEmpty()) {
            return null;
        }
        String normalized = brakeSquealPreset.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return BrakeSquealProfile.forPreset(normalized);
    }

    AfterfireProfile resolveAfterfireProfile() {
        if (afterfirePreset == null || afterfirePreset.trim().isEmpty()) {
            return null;
        }
        String normalized = afterfirePreset.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return AfterfireProfile.forPreset(normalized);
    }

    TireSquealProfile resolveTireSquealProfile() {
        if (tireSquealPreset == null || tireSquealPreset.trim().isEmpty()) {
            return null;
        }
        String normalized = tireSquealPreset.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return TireSquealProfile.forPreset(normalized);
    }

    HornProfile resolveHornProfile() {
        if (hornPreset == null || hornPreset.trim().isEmpty()) return null;
        String normalized = normalize(hornPreset);
        if ("CUSTOM".equals(normalized)) {
            return HornProfile.custom(hornSource, hornFrequenciesHz, hornRelativeGains,
                    configured(hornAttackSeconds), configured(hornHoldSeconds),
                    configured(hornReleaseSeconds), configured(hornGain),
                    configured(hornBrightness), configured(hornRasp),
                    configured(hornAudibleDistance));
        }
        return HornProfile.forPreset(normalized).withOverrides(
                hornSource, hornFrequenciesHz, hornRelativeGains,
                configured(hornAttackSeconds), configured(hornHoldSeconds),
                configured(hornReleaseSeconds), configured(hornGain),
                configured(hornBrightness), configured(hornRasp),
                configured(hornAudibleDistance));
    }

    SirenProfile resolveSirenProfile() {
        if (sirenPreset == null || sirenPreset.trim().isEmpty()) return null;
        String normalized = normalize(sirenPreset);
        if ("CUSTOM".equals(normalized)) {
            return SirenProfile.custom(sirenSource, sirenPattern, sirenFrequenciesHz,
                    sirenSecondaryFrequenciesHz, sirenDurationsSeconds, sirenHarmonics,
                    configured(sirenGain), configured(sirenRasp), configured(sirenFlutterHz),
                    configured(sirenFlutterDepth), configured(sirenSubharmonicGain),
                    configured(sirenAudibleDistance));
        }
        return SirenProfile.forPreset(normalized).withOverrides(
                sirenSource, sirenPattern, sirenFrequenciesHz, sirenSecondaryFrequenciesHz,
                sirenDurationsSeconds, sirenHarmonics,
                configured(sirenGain), configured(sirenRasp), configured(sirenFlutterHz),
                configured(sirenFlutterDepth), configured(sirenSubharmonicGain),
                configured(sirenAudibleDistance));
    }

    private EngineProfile applyFiringPattern(EngineProfile profile) {
        if (firingOrder == null && firingBanks == null) {
            return profile;
        }
        if (profile.getPowertrain() != EnginePowertrain.COMBUSTION) {
            throw new IllegalArgumentException("FiringOrder and FiringBanks are only valid for combustion presets");
        }

        int[] inheritedOrder = profile.getFiringPattern().getFiringOrder();
        int[] order = firingOrder == null ? inheritedOrder : firingOrder;
        int[] banks;
        if (firingBanks != null) {
            banks = firingBanks;
        } else if (order.length == inheritedOrder.length) {
            banks = profile.getFiringPattern().getFiringBanks();
        } else {
            banks = new int[order.length];
        }
        return profile.withFiringPattern(order, banks);
    }

    private static Float configured(float value) {
        return Float.isNaN(value) ? null : value;
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
