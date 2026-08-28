package de.jarexception.advancedsoundaddon.sound;

/** Built-in compressor acoustics selected automatically by engine character. */
final class TurbochargerProfile {
    private final String presetName;
    private final int compressorCount;
    private final float spoolStartFraction;
    private final float fullSpoolFraction;
    private final float spoolRiseSeconds;
    private final float spoolFallSeconds;
    private final float minimumWhistleHz;
    private final float maximumWhistleHz;
    private final float whistleGain;
    private final float airflowGain;
    private final float releaseGain;
    private final float releaseDurationSeconds;
    private final float releaseFlutterDepth;
    private final float interiorGain;

    private TurbochargerProfile(String presetName, int compressorCount,
                               float spoolStartFraction, float fullSpoolFraction,
                               float spoolRiseSeconds, float spoolFallSeconds,
                               float minimumWhistleHz, float maximumWhistleHz,
                               float whistleGain, float airflowGain,
                               float releaseGain, float releaseDurationSeconds,
                               float releaseFlutterDepth, float interiorGain) {
        this.presetName = presetName;
        this.compressorCount = compressorCount;
        this.spoolStartFraction = spoolStartFraction;
        this.fullSpoolFraction = fullSpoolFraction;
        this.spoolRiseSeconds = spoolRiseSeconds;
        this.spoolFallSeconds = spoolFallSeconds;
        this.minimumWhistleHz = minimumWhistleHz;
        this.maximumWhistleHz = maximumWhistleHz;
        this.whistleGain = whistleGain;
        this.airflowGain = airflowGain;
        this.releaseGain = releaseGain;
        this.releaseDurationSeconds = releaseDurationSeconds;
        this.releaseFlutterDepth = releaseFlutterDepth;
        this.interiorGain = interiorGain;
    }

    static TurbochargerProfile forEngineProfile(EngineProfile engine) {
        if (engine.getPowertrain() != EnginePowertrain.COMBUSTION) {
            return null;
        }
        switch (engine.getPresetName()) {
            case "I3_TURBO_ROAD":
                return turbo("COMPACT_TURBO", 1, 0.22F, 0.70F,
                        0.34F, 0.52F, 950, 5_800,
                        0.034F, 0.018F, 0.026F, 0.22F, 0.08F, 0.58F);
            case "I4_TURBO_SPORT":
                return turbo("SPORT_SINGLE_TURBO", 1, 0.20F, 0.66F,
                        0.25F, 0.40F, 1_050, 7_600,
                        0.060F, 0.032F, 0.070F, 0.28F, 0.16F, 0.68F);
            case "I6_LUXURY_SPORT":
                return turbo("E_BOOSTED_INLINE_SIX", 1, 0.18F, 0.62F,
                        0.22F, 0.38F, 1_000, 6_900,
                        0.042F, 0.026F, 0.040F, 0.24F, 0.08F, 0.62F);
            case "I6_TURBO_SPORT":
                return turbo("TWIN_SCROLL_INLINE_SIX", 1, 0.18F, 0.64F,
                        0.23F, 0.38F, 1_050, 7_700,
                        0.066F, 0.034F, 0.075F, 0.29F, 0.18F, 0.70F);
            case "I6_PERFORMANCE":
                return turbo("PERFORMANCE_TWIN_TURBO", 2, 0.18F, 0.63F,
                        0.22F, 0.36F, 1_100, 8_100,
                        0.074F, 0.039F, 0.090F, 0.31F, 0.28F, 0.72F);
            case "V6_UTILITY_TURBO":
                return turbo("UTILITY_TWIN_TURBO", 2, 0.20F, 0.66F,
                        0.27F, 0.45F, 850, 5_900,
                        0.032F, 0.020F, 0.018F, 0.20F, 0.05F, 0.55F);
            case "V6_TWIN_TURBO":
                return turbo("GT_R_TWIN_TURBO", 2, 0.17F, 0.61F,
                        0.20F, 0.36F, 1_150, 8_600,
                        0.108F, 0.054F, 0.120F, 0.46F, 0.34F, 0.76F);
            case "V8_LUXURY_TURBO":
                return turbo("LUXURY_TWIN_TURBO", 2, 0.18F, 0.62F,
                        0.22F, 0.40F, 900, 6_700,
                        0.046F, 0.026F, 0.035F, 0.23F, 0.06F, 0.54F);
            case "V8_FLATPLANE_TURBO":
                return turbo("SUPERCAR_TWIN_TURBO", 2, 0.17F, 0.60F,
                        0.19F, 0.34F, 1_200, 8_900,
                        0.080F, 0.042F, 0.078F, 0.27F, 0.14F, 0.70F);
            case "V8_FLATPLANE_RACE":
                return turbo("RACE_TWIN_TURBO", 2, 0.16F, 0.59F,
                        0.18F, 0.32F, 1_250, 9_200,
                        0.070F, 0.040F, 0.064F, 0.25F, 0.18F, 0.68F);
            case "V12_LUXURY":
                return turbo("SILENCED_LUXURY_TWIN_TURBO", 2, 0.16F, 0.58F,
                        0.20F, 0.42F, 750, 4_600,
                        0.014F, 0.009F, 0.008F, 0.18F, 0.0F, 0.38F);
            case "W16":
            case "W16_HYPERCAR":
                return turbo("HYPERCAR_QUAD_TURBO", 4, 0.15F, 0.58F,
                        0.18F, 0.34F, 1_100, 8_400,
                        0.082F, 0.046F, 0.070F, 0.28F, 0.10F, 0.65F);
            case "I4_DIESEL_REFINED":
                return diesel("REFINED_DIESEL_TURBO", 0.020F, 0.016F, 0.50F);
            case "I4_DIESEL":
            case "I4_DIESEL_UTILITY":
                return diesel("UTILITY_DIESEL_TURBO", 0.030F, 0.024F, 0.54F);
            case "I4_DIESEL_OFFROAD":
                return diesel("BITURBO_DIESEL_OFFROAD", 0.042F, 0.030F, 0.58F);
            case "I6_DIESEL":
                return diesel("INLINE_SIX_DIESEL_TURBO", 0.036F, 0.026F, 0.54F);
            case "I6_BUS_DIESEL":
                return heavyDiesel("BUS_DIESEL_TURBO", 0.034F, 0.028F);
            case "I6_TRUCK_DIESEL":
                return heavyDiesel("TRUCK_DIESEL_TURBO", 0.056F, 0.038F);
            case "I6_HEAVY_DIESEL":
                return heavyDiesel("HEAVY_DIESEL_TURBO", 0.068F, 0.044F);
            case "V8_DIESEL":
            case "V8_DIESEL_ARMORED":
                return turbo("ARMORED_V8_DIESEL_TURBO", 1, 0.14F, 0.56F,
                        0.34F, 0.62F, 650, 5_400,
                        0.050F, 0.034F, 0.004F, 0.18F, 0.0F, 0.48F);
            default:
                return null;
        }
    }

    private static TurbochargerProfile diesel(String name, float whistleGain,
                                               float airflowGain, float interiorGain) {
        return turbo(name, 1, 0.16F, 0.62F,
                0.30F, 0.56F, 700, 4_900,
                whistleGain, airflowGain, 0.004F, 0.18F, 0.0F, interiorGain);
    }

    private static TurbochargerProfile heavyDiesel(String name, float whistleGain,
                                                    float airflowGain) {
        return turbo(name, 1, 0.12F, 0.54F,
                0.38F, 0.72F, 620, 5_200,
                whistleGain, airflowGain, 0.003F, 0.20F, 0.0F, 0.48F);
    }

    private static TurbochargerProfile turbo(String name, int compressorCount,
                                             float spoolStartFraction, float fullSpoolFraction,
                                             float spoolRiseSeconds, float spoolFallSeconds,
                                             float minimumWhistleHz, float maximumWhistleHz,
                                             float whistleGain, float airflowGain,
                                             float releaseGain, float releaseDurationSeconds,
                                             float releaseFlutterDepth, float interiorGain) {
        return new TurbochargerProfile(name, compressorCount,
                spoolStartFraction, fullSpoolFraction,
                spoolRiseSeconds, spoolFallSeconds,
                minimumWhistleHz, maximumWhistleHz,
                whistleGain, airflowGain, releaseGain,
                releaseDurationSeconds, releaseFlutterDepth, interiorGain);
    }

    String getPresetName() { return presetName; }
    int getCompressorCount() { return compressorCount; }
    float getSpoolStartFraction() { return spoolStartFraction; }
    float getFullSpoolFraction() { return fullSpoolFraction; }
    float getSpoolRiseSeconds() { return spoolRiseSeconds; }
    float getSpoolFallSeconds() { return spoolFallSeconds; }
    float getMinimumWhistleHz() { return minimumWhistleHz; }
    float getMaximumWhistleHz() { return maximumWhistleHz; }
    float getWhistleGain() { return whistleGain; }
    float getAirflowGain() { return airflowGain; }
    float getReleaseGain() { return releaseGain; }
    float getReleaseDurationSeconds() { return releaseDurationSeconds; }
    float getReleaseFlutterDepth() { return releaseFlutterDepth; }
    float getInteriorGain() { return interiorGain; }
}
