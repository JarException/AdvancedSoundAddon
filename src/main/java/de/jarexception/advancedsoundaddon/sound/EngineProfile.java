package de.jarexception.advancedsoundaddon.sound;

public final class EngineProfile {
    private final String presetName;
    private final EnginePowertrain powertrain;
    private final EngineLayout layout;
    private final EngineFiringPattern firingPattern;
    private final boolean compressionIgnition;
    private final float idleRpm;
    private final float acousticMaxRpm;
    private final float outputGain;
    private final float starterRpm;
    private final float startDurationSeconds;
    private final float stopDurationSeconds;
    private final float exhaustResonanceHz;
    private final float intakeResonanceHz;
    private final float exhaustGain;
    private final float intakeGain;
    private final float mechanicalGain;
    private final float pulseSharpness;
    private final float inductionCharacter;
    private final float mechanicalBrightness;
    private final float primaryBankDelayMillis;
    private final float secondaryBankDelayMillis;

    private EngineProfile(String presetName, EnginePowertrain powertrain,
                          EngineLayout layout, EngineFiringPattern firingPattern,
                          boolean compressionIgnition,
                          float idleRpm, float acousticMaxRpm, float outputGain, float starterRpm,
                          float startDurationSeconds, float stopDurationSeconds,
                          float exhaustResonanceHz, float intakeResonanceHz,
                          float exhaustGain, float intakeGain, float mechanicalGain,
                          float pulseSharpness, float inductionCharacter,
                          float mechanicalBrightness, float primaryBankDelayMillis,
                          float secondaryBankDelayMillis) {
        this.presetName = presetName;
        this.powertrain = powertrain;
        this.layout = layout;
        this.firingPattern = firingPattern;
        this.compressionIgnition = compressionIgnition;
        this.idleRpm = idleRpm;
        this.acousticMaxRpm = acousticMaxRpm;
        this.outputGain = outputGain;
        this.starterRpm = starterRpm;
        this.startDurationSeconds = startDurationSeconds;
        this.stopDurationSeconds = stopDurationSeconds;
        this.exhaustResonanceHz = exhaustResonanceHz;
        this.intakeResonanceHz = intakeResonanceHz;
        this.exhaustGain = exhaustGain;
        this.intakeGain = intakeGain;
        this.mechanicalGain = mechanicalGain;
        this.pulseSharpness = pulseSharpness;
        this.inductionCharacter = inductionCharacter;
        this.mechanicalBrightness = mechanicalBrightness;
        this.primaryBankDelayMillis = primaryBankDelayMillis;
        this.secondaryBankDelayMillis = secondaryBankDelayMillis;
    }

    public static EngineProfile forPreset(String presetName) {
        switch (presetName) {
            case "I1_SCOOTER":
                return petrol("I1_SCOOTER", EngineLayout.I1, 1_450, 7_000, 0.64F, 260,
                        0.65F, 0.85F, 165, 520, 0.88F, 0.55F, 0.28F,
                        0.82F, 0.94F, 1.05F, 0.68F, 0.68F);
            case "I1_KART":
                return petrol("I1_KART", EngineLayout.I1, 1_800, 8_500, 0.82F, 320,
                        0.45F, 0.55F, 185, 650, 1.18F, 0.72F, 0.34F,
                        1.05F, 1.30F, 1.25F, 0.68F, 0.68F);
            case "I3_ROAD":
                return roadInlineThree("I3_ROAD");
            case "I3_CITY":
                return petrol("I3_CITY", EngineLayout.I3, 780, 6_200, 0.62F, 195,
                        1.10F, 1.40F, 108, 310, 0.68F, 0.22F, 0.16F,
                        0.56F, 0.62F, 0.62F, 0.84F, 0.84F);
            case "I3_TURBO_ROAD":
                return petrol("I3_TURBO_ROAD", EngineLayout.I3, 800, 6_500, 0.70F, 205,
                        1.02F, 1.32F, 122, 355, 0.80F, 0.30F, 0.18F,
                        0.66F, 0.78F, 0.72F, 0.80F, 0.80F);
            case "I3_SPORT":
                return petrol("I3_SPORT", EngineLayout.I3, 900, 0, 0.88F, 240,
                        0.86F, 1.05F, 150, 455, 0.98F, 0.48F, 0.30F,
                        0.88F, 1.12F, 1.08F, 0.70F, 0.70F);
            case "I3_BIKE":
                return petrol("I3_BIKE", EngineLayout.I3, 1_200, 10_000, 0.86F, 300,
                        0.55F, 0.68F, 182, 640, 1.08F, 0.70F, 0.34F,
                        1.18F, 1.42F, 1.30F, 0.48F, 0.48F);
            case "I4_BIKE":
                return petrol("I4_BIKE", EngineLayout.I4, 1_200, 11_000, 0.90F, 320,
                        0.55F, 0.70F, 174, 690, 1.12F, 0.72F, 0.34F,
                        1.18F, 1.42F, 1.30F, 0.48F, 0.48F);
            case "I4_LUXURY":
                return petrol("I4_LUXURY", EngineLayout.I4, 750, 6_200, 0.68F, 200,
                        1.10F, 1.45F, 105, 330, 0.72F, 0.24F, 0.14F,
                        0.58F, 0.65F, 0.62F, 0.78F, 0.78F);
            case "I4_ROADSTER":
                return petrol("I4_ROADSTER", EngineLayout.I4, 850, 7_500, 0.80F, 225,
                        0.88F, 1.10F, 150, 500, 0.91F, 0.45F, 0.20F,
                        0.82F, 1.02F, 0.92F, 0.70F, 0.70F);
            case "I4_TURBO_SPORT":
                return petrol("I4_TURBO_SPORT", EngineLayout.I4, 850, 6_800, 0.88F, 225,
                        0.84F, 1.05F, 135, 460, 1.00F, 0.48F, 0.22F,
                        0.88F, 1.12F, 1.02F, 0.68F, 0.68F);
            case "I6_LUXURY_SPORT":
                return petrol("I6_LUXURY_SPORT", EngineLayout.I6, 700, 6_500, 0.76F, 195,
                        1.15F, 1.48F, 78, 260, 0.82F, 0.23F, 0.13F,
                        0.50F, 0.68F, 0.62F, 0.76F, 0.76F);
            case "I6_TURBO_SPORT":
                return petrol("I6_TURBO_SPORT", EngineLayout.I6, 750, 7_000, 0.88F, 210,
                        0.92F, 1.18F, 92, 320, 0.96F, 0.34F, 0.18F,
                        0.66F, 0.90F, 0.82F, 0.70F, 0.70F);
            case "I6_PERFORMANCE":
                return petrol("I6_PERFORMANCE", EngineLayout.I6, 850, 7_600, 0.94F, 225,
                        0.82F, 1.02F, 105, 380, 1.08F, 0.42F, 0.20F,
                        0.76F, 1.05F, 0.96F, 0.68F, 0.68F);
            case "V6_CLASSIC":
                return petrol("V6_CLASSIC", EngineLayout.V6, 750, 6_000, 0.70F, 205,
                        1.18F, 1.55F, 88, 270, 0.76F, 0.22F, 0.20F,
                        0.55F, 0.62F, 0.68F, 0.62F, 1.18F);
            case "V6_UTILITY_TURBO":
                return petrol("V6_UTILITY_TURBO", EngineLayout.V6, 750, 6_200, 0.78F, 210,
                        1.02F, 1.35F, 82, 275, 0.86F, 0.25F, 0.18F,
                        0.60F, 0.72F, 0.70F, 0.58F, 1.15F);
            case "V6_TWIN_TURBO":
                return petrol("V6_TWIN_TURBO", EngineLayout.V6, 850, 7_200, 0.94F, 225,
                        0.84F, 1.05F, 110, 380, 1.00F, 0.38F, 0.20F,
                        0.80F, 1.00F, 0.90F, 0.52F, 1.08F);
            case "FLAT6_RACE":
                return petrol("FLAT6_RACE", EngineLayout.FLAT6, 1_050, 9_000, 0.98F, 245,
                        0.68F, 0.84F, 120, 480, 1.05F, 0.52F, 0.22F,
                        0.92F, 1.20F, 1.08F, 0.46F, 1.02F);
            case "V8_LUXURY_TURBO":
                return petrol("V8_LUXURY_TURBO", EngineLayout.V8_CROSSPLANE, 700, 6_600, 0.78F, 185,
                        1.18F, 1.55F, 70, 240, 0.85F, 0.22F, 0.12F,
                        0.45F, 0.62F, 0.58F, 0.60F, 1.10F);
            case "V8_LUXURY_NA":
                return petrol("V8_LUXURY_NA", EngineLayout.V8_CROSSPLANE, 650, 6_000, 0.68F, 180,
                        1.25F, 1.70F, 64, 220, 0.78F, 0.18F, 0.11F,
                        0.42F, 0.55F, 0.52F, 0.62F, 1.12F);
            case "V8_MUSCLE":
                return petrol("V8_MUSCLE", EngineLayout.V8_CROSSPLANE, 800, 0, 1.00F, 205,
                        1.08F, 1.48F, 72, 260, 1.15F, 0.25F, 0.16F,
                        0.52F, 0.75F, 0.72F, 0.55F, 1.12F);
            case "V8_TRUCK":
                return petrol("V8_TRUCK", EngineLayout.V8_CROSSPLANE, 650, 5_800, 0.82F, 185,
                        1.25F, 1.70F, 62, 220, 1.00F, 0.20F, 0.15F,
                        0.48F, 0.62F, 0.62F, 0.58F, 1.15F);
            case "V8_SUPERCHARGED_CLASSIC":
                return petrol("V8_SUPERCHARGED_CLASSIC", EngineLayout.V8_CROSSPLANE, 800, 0, 1.00F, 205,
                        1.05F, 1.42F, 70, 245, 1.15F, 0.26F, 0.16F,
                        0.53F, 0.78F, 0.74F, 0.55F, 1.12F);
            case "V8_SUPERCHARGED_MODERN":
                return petrol("V8_SUPERCHARGED_MODERN", EngineLayout.V8_CROSSPLANE, 850, 0, 1.00F, 215,
                        0.98F, 1.32F, 80, 300, 1.12F, 0.30F, 0.17F,
                        0.58F, 0.84F, 0.80F, 0.53F, 1.10F);
            case "V8_SUPERCHARGED_SUV":
                return petrol("V8_SUPERCHARGED_SUV", EngineLayout.V8_CROSSPLANE, 700, 6_800, 0.84F, 195,
                        1.12F, 1.48F, 74, 275, 0.94F, 0.26F, 0.14F,
                        0.48F, 0.70F, 0.64F, 0.58F, 1.13F);
            case "V8_OFFROAD_RACE":
                return petrol("V8_OFFROAD_RACE", EngineLayout.V8_CROSSPLANE, 950, 8_500, 1.00F, 250,
                        0.68F, 0.82F, 90, 350, 1.22F, 0.42F, 0.22F,
                        0.72F, 1.05F, 0.98F, 0.50F, 1.05F);
            case "V8_MARINE":
                return petrol("V8_MARINE", EngineLayout.V8_CROSSPLANE, 700, 5_000, 0.88F, 190,
                        1.20F, 1.70F, 54, 180, 1.10F, 0.18F, 0.14F,
                        0.44F, 0.55F, 0.55F, 0.62F, 1.15F);
            case "V8_FLATPLANE_TURBO":
                return petrol("V8_FLATPLANE_TURBO", EngineLayout.V8_FLATPLANE, 850, 8_500, 0.94F, 225,
                        0.78F, 0.98F, 118, 420, 0.96F, 0.42F, 0.19F,
                        0.85F, 1.08F, 1.00F, 0.52F, 1.08F);
            case "V8_FLATPLANE_RACE":
                return petrol("V8_FLATPLANE_RACE", EngineLayout.V8_FLATPLANE, 800, 7_200, 0.96F, 220,
                        0.76F, 0.94F, 105, 390, 1.04F, 0.40F, 0.20F,
                        0.88F, 1.05F, 1.02F, 0.52F, 1.08F);
            case "V12_LUXURY":
                return petrol("V12_LUXURY", EngineLayout.V12, 600, 5_500, 0.48F, 160,
                        1.50F, 2.00F, 58, 210, 0.55F, 0.12F, 0.08F,
                        0.35F, 0.40F, 0.40F, 0.60F, 1.05F);
            case "V12_RACE":
                return petrol("V12_RACE", EngineLayout.V12, 1_100, 8_200, 1.00F, 240,
                        0.78F, 0.92F, 110, 400, 1.08F, 0.42F, 0.20F,
                        0.75F, 1.05F, 0.92F, 0.52F, 1.08F);
            case "W16_HYPERCAR":
                return petrol("W16_HYPERCAR", EngineLayout.W16, 900, 0, 1.00F, 210,
                        1.18F, 1.70F, 72, 305, 1.04F, 0.30F, 0.18F,
                        0.62F, 0.82F, 0.82F, 0.48F, 1.02F);
            case "I4_DIESEL":
                return diesel("I4_DIESEL", EngineLayout.I4, 760, 4_500, 0.62F, 175,
                        1.28F, 1.75F, 105, 260, 0.64F, 0.20F, 0.40F,
                        0.85F, 0.48F, 1.00F, 0.78F, 0.78F);
            case "I4_DIESEL_REFINED":
                return diesel("I4_DIESEL_REFINED", EngineLayout.I4, 720, 4_200, 0.54F, 165,
                        1.30F, 1.70F, 95, 245, 0.56F, 0.16F, 0.30F,
                        0.76F, 0.40F, 0.82F, 0.80F, 0.80F);
            case "I4_DIESEL_UTILITY":
                return diesel("I4_DIESEL_UTILITY", EngineLayout.I4, 750, 4_200, 0.60F, 175,
                        1.35F, 1.85F, 105, 260, 0.66F, 0.18F, 0.36F,
                        0.82F, 0.44F, 0.92F, 0.78F, 0.78F);
            case "I4_DIESEL_OFFROAD":
                return diesel("I4_DIESEL_OFFROAD", EngineLayout.I4, 800, 4_500, 0.64F, 185,
                        1.22F, 1.70F, 112, 285, 0.72F, 0.22F, 0.38F,
                        0.86F, 0.50F, 0.96F, 0.76F, 0.76F);
            case "I6_DIESEL":
                return diesel("I6_DIESEL", EngineLayout.I6, 680, 4_000, 0.68F, 155,
                        1.45F, 2.05F, 125, 270, 0.66F, 0.18F, 0.38F,
                        0.80F, 0.44F, 0.96F, 0.72F, 0.72F);
            case "I6_BUS_DIESEL":
                return diesel("I6_BUS_DIESEL", EngineLayout.I6, 600, 2_200, 0.58F, 140,
                        1.65F, 2.30F, 95, 220, 0.64F, 0.12F, 0.34F,
                        0.70F, 0.32F, 0.80F, 0.76F, 0.76F);
            case "I6_TRUCK_DIESEL":
                return diesel("I6_TRUCK_DIESEL", EngineLayout.I6, 650, 2_500, 0.66F, 145,
                        1.55F, 2.20F, 105, 245, 0.72F, 0.14F, 0.40F,
                        0.78F, 0.38F, 0.92F, 0.72F, 0.72F);
            case "I6_HEAVY_DIESEL":
                return diesel("I6_HEAVY_DIESEL", EngineLayout.I6, 600, 2_100, 0.68F, 145,
                        1.70F, 2.40F, 90, 210, 0.78F, 0.12F, 0.42F,
                        0.74F, 0.34F, 0.88F, 0.74F, 0.74F);
            case "V8_DIESEL":
                return diesel("V8_DIESEL", EngineLayout.V8_CROSSPLANE, 650, 4_000, 0.72F, 150,
                        1.55F, 2.20F, 105, 220, 0.74F, 0.16F, 0.38F,
                        0.78F, 0.44F, 0.95F, 0.55F, 1.12F);
            case "V8_DIESEL_ARMORED":
                return diesel("V8_DIESEL_ARMORED", EngineLayout.V8_CROSSPLANE, 650, 4_000, 0.70F, 150,
                        1.60F, 2.25F, 100, 215, 0.72F, 0.15F, 0.40F,
                        0.76F, 0.42F, 0.92F, 0.55F, 1.12F);
            case "ELECTRIC":
                return alternative("ELECTRIC", EnginePowertrain.ELECTRIC);
            case "ELECTRIC_CITY":
                return alternative("ELECTRIC_CITY", EnginePowertrain.ELECTRIC,
                        0.32F, 0.72F, 0.58F);
            case "ELECTRIC_PERFORMANCE":
                return alternative("ELECTRIC_PERFORMANCE", EnginePowertrain.ELECTRIC,
                        0.50F, 1.08F, 1.10F);
            case "ELECTRIC_UTILITY":
                return alternative("ELECTRIC_UTILITY", EnginePowertrain.ELECTRIC,
                        0.35F, 0.55F, 0.75F);
            case "TURBOSHAFT":
                return alternative("TURBOSHAFT", EnginePowertrain.TURBOSHAFT);
            default:
                return forLayout(EngineLayout.valueOf(presetName));
        }
    }

    public static EngineProfile forLayout(EngineLayout layout) {
        switch (layout) {
            case I1:
                return combustion("I1", layout, 1_450, 310, 0.72F, 0.95F,
                        145, 480, 1.45F, 0.70F, 0.70F, 0.86F, 1.05F, 1.25F, 0.68F, 0.68F);
            case I3:
                return roadInlineThree("I3");
            case I5:
                return combustion("I5", layout, 920, 230, 0.98F, 1.25F,
                        102, 330, 0.96F, 0.36F, 0.22F, 0.70F, 1.08F, 1.00F, 0.72F, 0.72F);
            case I6:
                return combustion("I6", layout, 880, 220, 1.04F, 1.45F,
                        88, 285, 1.02F, 0.31F, 0.18F, 0.65F, 0.84F, 0.88F, 0.72F, 0.72F);
            case V6:
                return combustion("V6", layout, 980, 235, 1.00F, 1.35F,
                        105, 350, 1.00F, 0.34F, 0.19F, 0.76F, 0.92F, 0.88F, 0.55F, 1.12F);
            case FLAT6:
                return combustion("FLAT6", layout, 1_050, 245, 0.92F, 1.25F,
                        112, 390, 0.98F, 0.38F, 0.23F, 0.82F, 1.02F, 1.04F, 0.48F, 1.04F);
            case V8_CROSSPLANE:
                return combustion("V8_CROSSPLANE", layout, 850, 205, 1.10F, 1.65F,
                        76, 265, 1.12F, 0.27F, 0.16F, 0.54F, 0.78F, 0.76F, 0.55F, 1.12F);
            case V8_FLATPLANE:
                return combustion("V8_FLATPLANE", layout, 1_450, 280, 0.88F, 1.18F,
                        128, 430, 0.92F, 0.40F, 0.24F, 0.90F, 1.08F, 1.12F, 0.55F, 1.12F);
            case V10:
                return combustion("V10", layout, 1_300, 265, 0.98F, 1.30F,
                        116, 395, 0.92F, 0.38F, 0.21F, 0.82F, 1.08F, 1.12F, 0.55F, 1.12F);
            case V12:
                return combustion("V12", layout, 1_150, 240, 1.12F, 1.55F,
                        94, 345, 0.88F, 0.33F, 0.16F, 0.65F, 0.84F, 0.76F, 0.55F, 1.12F);
            case W16:
                return combustion("W16", layout, 900, 210, 1.18F, 1.70F,
                        72, 305, 1.04F, 0.30F, 0.18F, 0.62F, 0.82F, 0.82F, 0.48F, 1.02F);
            case I4:
            default:
                return combustion("I4", layout, 900, 220, 0.94F, 1.20F,
                        148, 455, 1.08F, 0.52F, 0.26F, 0.94F, 1.20F, 1.14F, 0.72F, 0.72F);
        }
    }

    private static EngineProfile combustion(String presetName, EngineLayout layout,
                                             float idleRpm, float starterRpm,
                                             float startDurationSeconds, float stopDurationSeconds,
                                             float exhaustResonanceHz, float intakeResonanceHz,
                                             float exhaustGain, float intakeGain, float mechanicalGain,
                                             float pulseSharpness, float inductionCharacter,
                                             float mechanicalBrightness, float primaryBankDelayMillis,
                                             float secondaryBankDelayMillis) {
        return new EngineProfile(presetName, EnginePowertrain.COMBUSTION, layout,
                EngineFiringPattern.forLayout(layout), false, idleRpm, 0,
                defaultCombustionOutputGain(layout), starterRpm,
                startDurationSeconds, stopDurationSeconds, exhaustResonanceHz,
                intakeResonanceHz, exhaustGain, intakeGain, mechanicalGain,
                pulseSharpness, inductionCharacter, mechanicalBrightness,
                primaryBankDelayMillis, secondaryBankDelayMillis);
    }

    private static EngineProfile roadInlineThree(String presetName) {
        return petrol(presetName, EngineLayout.I3, 790, 0, 0.74F, 205,
                1.05F, 1.35F, 118, 330, 0.82F, 0.28F, 0.18F,
                0.62F, 0.72F, 0.70F, 0.82F, 0.82F);
    }

    private static EngineProfile petrol(String presetName, EngineLayout layout,
                                        float idleRpm, float acousticMaxRpm, float outputGain,
                                        float starterRpm,
                                        float startDurationSeconds, float stopDurationSeconds,
                                        float exhaustResonanceHz, float intakeResonanceHz,
                                        float exhaustGain, float intakeGain, float mechanicalGain,
                                        float pulseSharpness, float inductionCharacter,
                                        float mechanicalBrightness, float primaryBankDelayMillis,
                                        float secondaryBankDelayMillis) {
        return new EngineProfile(presetName, EnginePowertrain.COMBUSTION, layout,
                EngineFiringPattern.forLayout(layout), false, idleRpm, acousticMaxRpm,
                outputGain, starterRpm,
                startDurationSeconds, stopDurationSeconds, exhaustResonanceHz,
                intakeResonanceHz, exhaustGain, intakeGain, mechanicalGain,
                pulseSharpness, inductionCharacter, mechanicalBrightness,
                primaryBankDelayMillis, secondaryBankDelayMillis);
    }

    private static EngineProfile diesel(String presetName, EngineLayout layout,
                                         float idleRpm, float acousticMaxRpm, float outputGain,
                                         float starterRpm,
                                         float startDurationSeconds, float stopDurationSeconds,
                                         float exhaustResonanceHz, float intakeResonanceHz,
                                         float exhaustGain, float intakeGain, float mechanicalGain,
                                         float pulseSharpness, float inductionCharacter,
                                         float mechanicalBrightness, float primaryBankDelayMillis,
                                         float secondaryBankDelayMillis) {
        return new EngineProfile(presetName, EnginePowertrain.COMBUSTION, layout,
                EngineFiringPattern.forLayout(layout), true, idleRpm, acousticMaxRpm,
                outputGain, starterRpm,
                startDurationSeconds, stopDurationSeconds, exhaustResonanceHz,
                intakeResonanceHz, exhaustGain, intakeGain, mechanicalGain,
                pulseSharpness, inductionCharacter, mechanicalBrightness,
                primaryBankDelayMillis, secondaryBankDelayMillis);
    }

    private static EngineProfile alternative(String presetName, EnginePowertrain powertrain) {
        return alternative(presetName, powertrain,
                powertrain == EnginePowertrain.ELECTRIC ? 0.50F : 0.85F,
                1.0F, 1.0F);
    }

    private static EngineProfile alternative(String presetName, EnginePowertrain powertrain,
                                             float outputGain, float inductionCharacter,
                                             float mechanicalBrightness) {
        return new EngineProfile(presetName, powertrain, null, null, false,
                0, 0, outputGain,
                0, 0.35F, 0.65F, 100, 400,
                1, 1, 1, 1, inductionCharacter, mechanicalBrightness, 0, 0);
    }

    private static float defaultCombustionOutputGain(EngineLayout layout) {
        switch (layout) {
            case I1:
                return 0.90F;
            case I3:
                return 0.74F;
            case I4:
                return 0.96F;
            case I5:
                return 0.97F;
            case I6:
                return 0.98F;
            case V6:
                return 0.98F;
            default:
                return 1.0F;
        }
    }

    public EngineProfile withOverrides(Float idleRpm, Float acousticMaxRpm, Float outputGain,
                                       Float starterRpm,
                                       Float startDurationSeconds, Float stopDurationSeconds, Float exhaustResonanceHz,
                                       Float intakeResonanceHz, Float exhaustGain,
                                       Float intakeGain, Float mechanicalGain, Float pulseSharpness,
                                       Float inductionCharacter, Float mechanicalBrightness,
                                       Float primaryBankDelayMillis, Float secondaryBankDelayMillis) {
        return new EngineProfile(presetName, powertrain, layout, firingPattern, compressionIgnition,
                boundedOr(idleRpm, this.idleRpm, 100, 4_000, "IdleRPM"),
                boundedOr(acousticMaxRpm, this.acousticMaxRpm, 500, 20_000, "AcousticMaxRPM"),
                boundedOr(outputGain, this.outputGain, 0, 2, "OutputGain"),
                boundedOr(starterRpm, this.starterRpm, 50, 1_500, "StarterRPM"),
                boundedOr(startDurationSeconds, this.startDurationSeconds, 0.05F, 10, "StartDurationSeconds"),
                boundedOr(stopDurationSeconds, this.stopDurationSeconds, 0.05F, 10, "StopDurationSeconds"),
                boundedOr(exhaustResonanceHz, this.exhaustResonanceHz, 20, 5_000, "ExhaustResonanceHz"),
                boundedOr(intakeResonanceHz, this.intakeResonanceHz, 20, 5_000, "IntakeResonanceHz"),
                boundedOr(exhaustGain, this.exhaustGain, 0, 4, "ExhaustGain"),
                boundedOr(intakeGain, this.intakeGain, 0, 4, "IntakeGain"),
                boundedOr(mechanicalGain, this.mechanicalGain, 0, 4, "MechanicalGain"),
                boundedOr(pulseSharpness, this.pulseSharpness, 0.1F, 2, "PulseSharpness"),
                boundedOr(inductionCharacter, this.inductionCharacter, 0, 4, "InductionCharacter"),
                boundedOr(mechanicalBrightness, this.mechanicalBrightness, 0, 4, "MechanicalBrightness"),
                boundedOr(primaryBankDelayMillis, this.primaryBankDelayMillis, 0, 25, "PrimaryBankDelayMillis"),
                boundedOr(secondaryBankDelayMillis, this.secondaryBankDelayMillis, 0, 25, "SecondaryBankDelayMillis"));
    }

    public EngineProfile withFiringPattern(int[] firingOrder, int[] firingBanks) {
        if (powertrain != EnginePowertrain.COMBUSTION) {
            throw new IllegalArgumentException("FiringOrder and FiringBanks are only valid for combustion presets");
        }
        EngineFiringPattern replacement = EngineFiringPattern.create(firingOrder, firingBanks);
        return new EngineProfile(presetName, powertrain, layout, replacement, compressionIgnition,
                idleRpm, acousticMaxRpm, outputGain, starterRpm,
                startDurationSeconds, stopDurationSeconds,
                exhaustResonanceHz, intakeResonanceHz, exhaustGain, intakeGain,
                mechanicalGain, pulseSharpness, inductionCharacter,
                mechanicalBrightness, primaryBankDelayMillis, secondaryBankDelayMillis);
    }

    private static float boundedOr(Float value, float fallback, float minimum, float maximum, String name) {
        if (value == null) {
            return fallback;
        }
        if (Float.isNaN(value) || Float.isInfinite(value) || value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum + ", got " + value);
        }
        return value;
    }

    public String getPresetName() { return presetName; }
    public EnginePowertrain getPowertrain() { return powertrain; }
    public EngineLayout getLayout() { return layout; }
    public EngineFiringPattern getFiringPattern() { return firingPattern; }
    public boolean isCompressionIgnition() { return compressionIgnition; }
    public float getIdleRpm() { return idleRpm; }
    public float resolveAcousticMaxRpm(float engineMaxRpm) {
        return acousticMaxRpm > 0 ? acousticMaxRpm : engineMaxRpm;
    }
    public float mapAcousticRpm(float normalizedRpm, float engineMaxRpm) {
        float sourceMaximum = Math.max(500.0F, engineMaxRpm);
        float targetMaximum = Math.max(500.0F, resolveAcousticMaxRpm(sourceMaximum));
        float sourceRpm = Math.max(0.0F, Math.min(1.1F, normalizedRpm)) * sourceMaximum;
        if (sourceRpm <= idleRpm || Math.abs(targetMaximum - sourceMaximum) < 0.001F) {
            return sourceRpm;
        }
        float sourceRange = sourceMaximum - idleRpm;
        float targetRange = targetMaximum - idleRpm;
        if (sourceRange <= 1.0F || targetRange <= 1.0F) {
            return Math.max(0.0F, Math.min(targetMaximum * 1.1F, sourceRpm));
        }
        return idleRpm + (sourceRpm - idleRpm) * targetRange / sourceRange;
    }
    public float getOutputGain() { return outputGain; }
    public float getStarterRpm() { return starterRpm; }
    public float getStartDurationSeconds() { return startDurationSeconds; }
    public float getStopDurationSeconds() { return stopDurationSeconds; }
    public float getExhaustResonanceHz() { return exhaustResonanceHz; }
    public float getIntakeResonanceHz() { return intakeResonanceHz; }
    public float getExhaustGain() { return exhaustGain; }
    public float getIntakeGain() { return intakeGain; }
    public float getMechanicalGain() { return mechanicalGain; }
    public float getPulseSharpness() { return pulseSharpness; }
    public float getInductionCharacter() { return inductionCharacter; }
    public float getMechanicalBrightness() { return mechanicalBrightness; }
    public float getBankDelayMillis(int bank) {
        return bank == 0 ? primaryBankDelayMillis : secondaryBankDelayMillis;
    }
}
