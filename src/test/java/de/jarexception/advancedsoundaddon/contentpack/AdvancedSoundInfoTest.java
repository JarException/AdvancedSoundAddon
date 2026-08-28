package de.jarexception.advancedsoundaddon.contentpack;

import de.jarexception.advancedsoundaddon.sound.EngineLayout;
import de.jarexception.advancedsoundaddon.sound.EngineProfile;
import de.jarexception.advancedsoundaddon.sound.EnginePowertrain;
import de.jarexception.advancedsoundaddon.sound.AirBrakeProfile;
import de.jarexception.advancedsoundaddon.sound.AfterfireProfile;
import de.jarexception.advancedsoundaddon.sound.BrakeSquealProfile;
import de.jarexception.advancedsoundaddon.sound.RotorProfile;
import de.jarexception.advancedsoundaddon.sound.TireSquealProfile;
import de.jarexception.advancedsoundaddon.sound.HornProfile;
import de.jarexception.advancedsoundaddon.sound.SirenProfile;
import fr.dynamx.api.contentpack.registry.RegisteredSubInfoType;
import fr.dynamx.api.contentpack.registry.SubInfoTypeRegistries;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AdvancedSoundInfoTest {
    @Test
    public void engineAndVehicleRegistrationsAreSplitAcrossDynamXLoaders() {
        RegisteredSubInfoType engine = AdvancedSoundInfo.class.getAnnotation(RegisteredSubInfoType.class);
        RegisteredSubInfoType vehicle = VehicleAdvancedSoundInfo.class.getAnnotation(RegisteredSubInfoType.class);

        assertEquals("AdvancedSoundAddon", engine.name());
        assertEquals("AdvancedSoundAddon", vehicle.name());
        assertTrue(engine.strictName());
        assertTrue(vehicle.strictName());
        assertEquals(Arrays.asList(SubInfoTypeRegistries.CAR_ENGINES),
                Arrays.asList(engine.registries()));
        assertEquals(Arrays.asList(
                        SubInfoTypeRegistries.WHEELED_VEHICLES,
                        SubInfoTypeRegistries.HELICOPTER),
                Arrays.asList(vehicle.registries()));
        assertTrue(AdvancedSoundInfo.class.isAssignableFrom(VehicleAdvancedSoundInfo.class));
    }

    @Test
    public void registrationsNeverTargetTheSameDynamXRegistryTwice() {
        assertSame(SubInfoTypeRegistries.CAR_ENGINES.getInfoList().getDefaultSubInfoTypesRegistry(),
                SubInfoTypeRegistries.HELICOPTER_ENGINES.getInfoList().getDefaultSubInfoTypesRegistry());
        assertSame(SubInfoTypeRegistries.WHEELED_VEHICLES.getInfoList().getDefaultSubInfoTypesRegistry(),
                SubInfoTypeRegistries.BOATS.getInfoList().getDefaultSubInfoTypesRegistry());
        assertNotSame(SubInfoTypeRegistries.WHEELED_VEHICLES.getInfoList().getDefaultSubInfoTypesRegistry(),
                SubInfoTypeRegistries.HELICOPTER.getInfoList().getDefaultSubInfoTypesRegistry());

        Set<Object> uniqueRegistries = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        RegisteredSubInfoType engine = AdvancedSoundInfo.class.getAnnotation(RegisteredSubInfoType.class);
        RegisteredSubInfoType vehicle = VehicleAdvancedSoundInfo.class.getAnnotation(RegisteredSubInfoType.class);
        for (SubInfoTypeRegistries registration : engine.registries()) {
            assertTrue(uniqueRegistries.add(registration.getInfoList().getDefaultSubInfoTypesRegistry()));
        }
        for (SubInfoTypeRegistries registration : vehicle.registries()) {
            assertTrue(uniqueRegistries.add(registration.getInfoList().getDefaultSubInfoTypesRegistry()));
        }
        assertFalse(uniqueRegistries.isEmpty());
    }

    @Test
    public void presetCanBeSelectedAndPartiallyOverridden() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.preset = "V8_CROSSPLANE";
        definition.idleRpm = 940.0F;
        definition.acousticMaxRpm = 6_250.0F;
        definition.outputGain = 0.80F;

        EngineProfile result = definition.applyTo(EngineProfile.forLayout(EngineLayout.I4));

        assertEquals(EngineLayout.V8_CROSSPLANE, result.getLayout());
        assertEquals(940.0F, result.getIdleRpm(), 0.0001F);
        assertEquals(6_250.0F, result.resolveAcousticMaxRpm(5_500.0F), 0.0001F);
        assertEquals(0.80F, result.getOutputGain(), 0.0001F);
        assertEquals(76.0F, result.getExhaustResonanceHz(), 0.0001F);
    }

    @Test
    public void vehicleDefinitionInheritsEngineValuesAndOverridesOnlyConfiguredFields() {
        AdvancedSoundInfo engine = new AdvancedSoundInfo(null);
        engine.preset = "I6";
        engine.idleRpm = 910.0F;
        engine.intakeGain = 0.55F;

        AdvancedSoundInfo vehicle = new AdvancedSoundInfo(null);
        vehicle.exhaustGain = 1.35F;

        EngineProfile engineProfile = engine.applyTo(EngineProfile.forLayout(EngineLayout.I4));
        EngineProfile result = vehicle.applyTo(engineProfile);

        assertEquals(EngineLayout.I6, result.getLayout());
        assertEquals(910.0F, result.getIdleRpm(), 0.0001F);
        assertEquals(0.55F, result.getIntakeGain(), 0.0001F);
        assertEquals(1.35F, result.getExhaustGain(), 0.0001F);
        assertEquals(5_500.0F, result.resolveAcousticMaxRpm(5_500.0F), 0.0001F);
    }

    @Test
    public void customFiringOrderAndBanksOverridePresetPhysics() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.preset = "V8_CROSSPLANE";
        definition.firingOrder = new int[]{1, 6, 5, 10, 2, 7, 3, 8, 4, 9};
        definition.firingBanks = new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1};

        EngineProfile result = definition.applyTo(EngineProfile.forLayout(EngineLayout.I4));

        assertEquals(EngineLayout.V8_CROSSPLANE, result.getLayout());
        assertEquals(10, result.getFiringPattern().getCylinderCount());
        assertEquals(2, result.getFiringPattern().getBankCount());
        assertTrue(Arrays.equals(new int[]{1, 6, 5, 10, 2, 7, 3, 8, 4, 9},
                result.getFiringPattern().getFiringOrder()));
    }

    @Test
    public void customCylinderCountDefaultsToSingleBank() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.firingOrder = new int[]{1, 3, 5, 2, 4};

        EngineProfile result = definition.applyTo(EngineProfile.forLayout(EngineLayout.I4));

        assertEquals(5, result.getFiringPattern().getCylinderCount());
        assertEquals(1, result.getFiringPattern().getBankCount());
    }

    @Test
    public void electricPresetSelectsDedicatedNonCombustionSynthesis() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.preset = "ELECTRIC";

        EngineProfile result = definition.applyTo(EngineProfile.forLayout(EngineLayout.I4));

        assertEquals("ELECTRIC", result.getPresetName());
        assertEquals(EnginePowertrain.ELECTRIC, result.getPowertrain());
        assertEquals(0.50F, result.getOutputGain(), 0.0001F);
        assertTrue(result.getFiringPattern() == null);
    }

    @Test
    public void presetsCarryTheirOwnSafeDefaultVolumeAndSoundRedline() {
        assertEquals(1.0F, EngineProfile.forPreset("V8_CROSSPLANE").getOutputGain(), 0.0001F);
        assertEquals(1.0F, EngineProfile.forPreset("W16").getOutputGain(), 0.0001F);
        assertEquals(0.62F, EngineProfile.forPreset("I4_DIESEL").getOutputGain(), 0.0001F);
        assertEquals(0.66F, EngineProfile.forPreset("I6_TRUCK_DIESEL").getOutputGain(), 0.0001F);
        assertEquals(4_000.0F,
                EngineProfile.forPreset("V8_DIESEL").resolveAcousticMaxRpm(7_500.0F),
                0.0001F);
        assertEquals(2_500.0F,
                EngineProfile.forPreset("I6_TRUCK_DIESEL").resolveAcousticMaxRpm(5_500.0F),
                0.0001F);

        EngineProfile lenco = EngineProfile.forPreset("V8_DIESEL");
        assertEquals("the acoustic redline must still map to the preset maximum",
                4_000.0F, lenco.mapAcousticRpm(1.0F, 7_500.0F), 0.0001F);
        assertEquals("cranking and idle must not be divided by the redline ratio",
                650.0F, lenco.mapAcousticRpm(650.0F / 7_500.0F, 7_500.0F), 0.0001F);
        assertEquals("a real 800 RPM idle must remain near idle after redline compression",
                723.358F, lenco.mapAcousticRpm(800.0F / 7_500.0F, 7_500.0F), 0.01F);

        EngineProfile truck = EngineProfile.forPreset("I6_TRUCK_DIESEL");
        assertEquals("truck idle must not collapse to the old 364 RPM artifact",
                707.216F, truck.mapAcousticRpm(800.0F / 5_500.0F, 5_500.0F), 0.01F);
    }

    @Test
    public void roadTriplesAreSeparatedFromSportAndMotorcycleCharacters() {
        EngineProfile defaultTriple = EngineProfile.forPreset("I3");
        EngineProfile roadTriple = EngineProfile.forPreset("I3_ROAD");
        EngineProfile sportTriple = EngineProfile.forPreset("I3_SPORT");
        EngineProfile bikeTriple = EngineProfile.forPreset("I3_BIKE");
        EngineProfile bikeFour = EngineProfile.forPreset("I4_BIKE");

        assertEquals(EngineLayout.I3, roadTriple.getLayout());
        assertEquals(defaultTriple.getOutputGain(), roadTriple.getOutputGain(), 0.0001F);
        assertEquals(0.74F, roadTriple.getOutputGain(), 0.0001F);
        assertTrue(roadTriple.getPulseSharpness() < sportTriple.getPulseSharpness());
        assertTrue(roadTriple.getInductionCharacter() < sportTriple.getInductionCharacter());
        assertTrue(roadTriple.getMechanicalBrightness() < bikeTriple.getMechanicalBrightness());
        assertEquals(10_000.0F, bikeTriple.resolveAcousticMaxRpm(5_500), 0.0001F);
        assertEquals(EngineLayout.I4, bikeFour.getLayout());
        assertEquals(11_000.0F, bikeFour.resolveAcousticMaxRpm(7_000), 0.0001F);
    }

    @Test
    public void vehicleCharacterPresetsSeparateLuxuryRoadAndRaceEngines() {
        EngineProfile rollsRoyce = EngineProfile.forPreset("V12_LUXURY");
        EngineProfile raceV12 = EngineProfile.forPreset("V12_RACE");
        EngineProfile luxuryV8 = EngineProfile.forPreset("V8_LUXURY_TURBO");
        EngineProfile muscleV8 = EngineProfile.forPreset("V8_MUSCLE");
        EngineProfile classicShelby = EngineProfile.forPreset("V8_SUPERCHARGED_CLASSIC");
        EngineProfile modernShelby = EngineProfile.forPreset("V8_SUPERCHARGED_MODERN");

        assertEquals(EngineLayout.V12, rollsRoyce.getLayout());
        assertEquals(0.48F, rollsRoyce.getOutputGain(), 0.0001F);
        assertTrue(rollsRoyce.getExhaustGain() < raceV12.getExhaustGain());
        assertTrue(rollsRoyce.getMechanicalBrightness() < raceV12.getMechanicalBrightness());
        assertTrue(luxuryV8.getOutputGain() < muscleV8.getOutputGain());
        assertEquals(1.0F, classicShelby.getOutputGain(), 0.0001F);
        assertEquals(1.0F, modernShelby.getOutputGain(), 0.0001F);
        assertTrue(classicShelby.getExhaustResonanceHz()
                < modernShelby.getExhaustResonanceHz());
    }

    @Test
    public void electricCharactersCarryAppropriateDefaultLevels() {
        EngineProfile city = EngineProfile.forPreset("ELECTRIC_CITY");
        EngineProfile performance = EngineProfile.forPreset("ELECTRIC_PERFORMANCE");
        EngineProfile utility = EngineProfile.forPreset("ELECTRIC_UTILITY");

        assertEquals(EnginePowertrain.ELECTRIC, city.getPowertrain());
        assertEquals(0.32F, city.getOutputGain(), 0.0001F);
        assertEquals(0.50F, performance.getOutputGain(), 0.0001F);
        assertTrue(city.getMechanicalBrightness() < performance.getMechanicalBrightness());
        assertTrue(utility.getInductionCharacter() < performance.getInductionCharacter());
    }

    @Test
    public void everyPackFacingPresetNameResolvesExactly() {
        String[] presets = {"I1", "I1_SCOOTER", "I1_KART",
                "I3", "I3_ROAD", "I3_CITY", "I3_TURBO_ROAD",
                "I3_SPORT", "I3_BIKE", "I4", "I4_BIKE", "I4_LUXURY",
                "I4_ROADSTER", "I4_TURBO_SPORT", "I5", "I6",
                "I6_LUXURY_SPORT", "I6_TURBO_SPORT", "I6_PERFORMANCE",
                "V6", "V6_CLASSIC", "V6_UTILITY_TURBO", "V6_TWIN_TURBO",
                "FLAT6", "FLAT6_RACE", "V8_CROSSPLANE", "V8_LUXURY_TURBO",
                "V8_LUXURY_NA", "V8_MUSCLE", "V8_TRUCK",
                "V8_SUPERCHARGED_CLASSIC", "V8_SUPERCHARGED_MODERN",
                "V8_SUPERCHARGED_SUV", "V8_OFFROAD_RACE", "V8_MARINE",
                "V8_FLATPLANE", "V8_FLATPLANE_TURBO", "V8_FLATPLANE_RACE",
                "V10", "V12", "V12_LUXURY", "V12_RACE", "W16", "W16_HYPERCAR",
                "I4_DIESEL", "I4_DIESEL_REFINED", "I4_DIESEL_UTILITY",
                "I4_DIESEL_OFFROAD", "I6_DIESEL", "I6_BUS_DIESEL",
                "I6_TRUCK_DIESEL", "I6_HEAVY_DIESEL", "V8_DIESEL",
                "V8_DIESEL_ARMORED", "ELECTRIC", "ELECTRIC_CITY",
                "ELECTRIC_PERFORMANCE", "ELECTRIC_UTILITY", "TURBOSHAFT"};
        for (String preset : presets) {
            assertEquals(preset, EngineProfile.forPreset(preset).getPresetName());
        }
    }

    @Test
    public void vehicleBlockSelectsIndependentRotorPreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.rotorPreset = "helicopter-4-blade";

        RotorProfile rotor = definition.resolveRotorProfile();

        assertEquals("HELICOPTER_4_BLADE", rotor.getPresetName());
        assertEquals(4, rotor.getBladeCount());
        assertEquals(EngineLayout.I4,
                definition.applyTo(EngineProfile.forLayout(EngineLayout.I4)).getLayout());
    }

    @Test
    public void vehicleBlockSelectsIndependentAirBrakePreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.airBrakePreset = "truck-air-brake";

        AirBrakeProfile airBrake = definition.resolveAirBrakeProfile();

        assertEquals("TRUCK_AIR_BRAKE", airBrake.getPresetName());
        assertEquals(EngineLayout.I4,
                definition.applyTo(EngineProfile.forLayout(EngineLayout.I4)).getLayout());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownAirBrakePreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.airBrakePreset = "random-truck-noise";
        definition.resolveAirBrakeProfile();
    }

    @Test
    public void vehicleBlockSelectsIndependentBrakeSquealPreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.brakeSquealPreset = "carbon-ceramic";

        BrakeSquealProfile brakeSqueal = definition.resolveBrakeSquealProfile();

        assertEquals("CARBON_CERAMIC", brakeSqueal.getPresetName());
        assertEquals(EngineLayout.I4,
                definition.applyTo(EngineProfile.forLayout(EngineLayout.I4)).getLayout());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownBrakeSquealPreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.brakeSquealPreset = "generic-brakes";
        definition.resolveBrakeSquealProfile();
    }

    @Test
    public void vehicleBlockSelectsIndependentAfterfirePreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.afterfirePreset = "aggressive";

        AfterfireProfile afterfire = definition.resolveAfterfireProfile();

        assertEquals("AGGRESSIVE", afterfire.getPresetName());
        assertEquals(EngineLayout.I4,
                definition.applyTo(EngineProfile.forLayout(EngineLayout.I4)).getLayout());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownAfterfirePreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.afterfirePreset = "random-explosion";
        definition.resolveAfterfireProfile();
    }

    @Test
    public void vehicleBlockSelectsIndependentTireSquealPreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.tireSquealPreset = "performance-tire";

        TireSquealProfile tireSqueal = definition.resolveTireSquealProfile();

        assertEquals("PERFORMANCE_TIRE", tireSqueal.getPresetName());
        assertEquals(EngineLayout.I4,
                definition.applyTo(EngineProfile.forLayout(EngineLayout.I4)).getLayout());
    }

    @Test
    public void unconfiguredTyresHaveAStreetTyreDefault() {
        assertEquals("STREET_TIRE", TireSquealProfile.defaultProfile().getPresetName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownTireSquealPreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.tireSquealPreset = "generic-loop";
        definition.resolveTireSquealProfile();
    }

    @Test
    public void hornAndSirenRemainOptInAndResolveIndependentPresets() {
        AdvancedSoundInfo empty = new AdvancedSoundInfo(null);
        assertTrue(empty.resolveHornProfile() == null);
        assertTrue(empty.resolveSirenProfile() == null);

        AdvancedSoundInfo configured = new AdvancedSoundInfo(null);
        configured.hornPreset = "truck-air";
        configured.sirenPreset = "de-fire";

        assertEquals("TRUCK_AIR", configured.resolveHornProfile().getPresetName());
        assertEquals("DE_FIRE", configured.resolveSirenProfile().getPresetName());
    }

    @Test
    public void fullyCustomSignalsAreAvailableWithoutPackAudioFiles() {
        AdvancedSoundInfo configured = new AdvancedSoundInfo(null);
        configured.hornPreset = "CUSTOM";
        configured.hornSource = "air-trumpet";
        configured.hornFrequenciesHz = new float[]{390, 470};
        configured.hornGain = 0.9F;
        configured.sirenPreset = "CUSTOM";
        configured.sirenSource = "air-horn";
        configured.sirenPattern = "STEP";
        configured.sirenFrequenciesHz = new float[]{430, 690};
        configured.sirenDurationsSeconds = new float[]{0.4F, 0.7F};
        configured.sirenSubharmonicGain = 0.25F;

        HornProfile horn = configured.resolveHornProfile();
        SirenProfile siren = configured.resolveSirenProfile();
        assertEquals("CUSTOM", horn.getPresetName());
        assertEquals(390.0F, horn.getFrequenciesHz()[0], 0.0001F);
        assertEquals(0.9F, horn.getOutputGain(), 0.0001F);
        assertEquals("AIR_TRUMPET", horn.getSourceName());
        assertEquals("CUSTOM", siren.getPresetName());
        assertEquals(690.0F, siren.getPrimaryFrequenciesHz()[1], 0.0001F);
        assertEquals(0.25F, siren.getSubharmonicGain(), 0.0001F);
        assertEquals("AIR_HORN", siren.getSourceName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void customSirenRejectsIncompleteFrequencyConfiguration() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.sirenPreset = "CUSTOM";
        definition.resolveSirenProfile();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownRotorPreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.rotorPreset = "guess-from-helicopter-name";
        definition.resolveRotorProfile();
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCombustionFiringDataForElectricPreset() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.preset = "ELECTRIC";
        definition.firingOrder = new int[]{1};
        definition.applyTo(EngineProfile.forLayout(EngineLayout.I4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidCustomFiringOrder() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.firingOrder = new int[]{1, 2, 2, 4};
        definition.applyTo(EngineProfile.forLayout(EngineLayout.I4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownPresetInsteadOfGuessingFromItsName() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.preset = "BMW_M4";
        definition.applyTo(EngineProfile.forLayout(EngineLayout.I4));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsafeValues() {
        AdvancedSoundInfo definition = new AdvancedSoundInfo(null);
        definition.exhaustGain = 99.0F;
        definition.applyTo(EngineProfile.forLayout(EngineLayout.I4));
    }
}
