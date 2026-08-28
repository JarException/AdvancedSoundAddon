package de.jarexception.advancedsoundaddon.sound;

/** Optional vehicle-level friction-brake acoustics. */
public final class BrakeSquealProfile {
    private enum TemperatureCharacter {
        CLASSIC, CERAMIC, DRUM
    }

    private final String presetName;
    private final float primaryModeHz;
    private final float secondaryModeHz;
    private final float outputGain;
    private final float minimumSpeedKmh;
    private final float maximumSpeedKmh;
    private final float heatRate;
    private final TemperatureCharacter temperatureCharacter;

    private BrakeSquealProfile(String presetName, float primaryModeHz,
                               float secondaryModeHz, float outputGain,
                               float minimumSpeedKmh, float maximumSpeedKmh,
                               float heatRate, TemperatureCharacter temperatureCharacter) {
        this.presetName = presetName;
        this.primaryModeHz = primaryModeHz;
        this.secondaryModeHz = secondaryModeHz;
        this.outputGain = outputGain;
        this.minimumSpeedKmh = minimumSpeedKmh;
        this.maximumSpeedKmh = maximumSpeedKmh;
        this.heatRate = heatRate;
        this.temperatureCharacter = temperatureCharacter;
    }

    public static BrakeSquealProfile forPreset(String presetName) {
        switch (presetName) {
            case "CLASSIC_DISC":
                return new BrakeSquealProfile(presetName, 2_280, 3_720,
                        0.86F, 1.2F, 52, 10.5F, TemperatureCharacter.CLASSIC);
            case "CARBON_CERAMIC":
                return new BrakeSquealProfile(presetName, 4_850, 7_650,
                        0.78F, 2.0F, 125, 15.0F, TemperatureCharacter.CERAMIC);
            case "OLD_DRUM":
                return new BrakeSquealProfile(presetName, 820, 1_460,
                        0.92F, 0.7F, 30, 8.0F, TemperatureCharacter.DRUM);
            default:
                throw new IllegalArgumentException("Unknown BrakeSquealPreset " + presetName);
        }
    }

    double resolveIntensity(double speedKmh, double brakeDemand, double temperatureC) {
        double fadeIn = Dsp.smoothStep((speedKmh - minimumSpeedKmh) / 2.2);
        double fadeOut = 1.0 - Dsp.smoothStep(
                (speedKmh - maximumSpeedKmh * 0.72) / (maximumSpeedKmh * 0.28));
        double speedWindow = fadeIn * fadeOut;
        double demand = Dsp.clamp(brakeDemand, 0.0, 1.0);
        switch (temperatureCharacter) {
            case CERAMIC: {
                double cold = 1.0 - Dsp.smoothStep((temperatureC - 75.0) / 230.0);
                double lightBrakeBias = 1.0 - demand * 0.34;
                return speedWindow * (0.20 + cold * 0.80)
                        * lightBrakeBias * (0.34 + demand * 0.66);
            }
            case DRUM: {
                double nearStop = 1.0 - Dsp.smoothStep(speedKmh / maximumSpeedKmh);
                double warm = 0.68 + Dsp.smoothStep((temperatureC - 30.0) / 105.0) * 0.32;
                return speedWindow * (0.42 + nearStop * 0.58)
                        * warm * (0.26 + demand * 0.74);
            }
            case CLASSIC:
            default: {
                double nearStop = 1.0 - Dsp.smoothStep(speedKmh / maximumSpeedKmh);
                double temperature = 0.72
                        + Dsp.smoothStep((temperatureC - 28.0) / 150.0) * 0.28;
                return speedWindow * (0.50 + nearStop * 0.50)
                        * temperature * (0.30 + demand * 0.70);
            }
        }
    }

    public String getPresetName() {
        return presetName;
    }

    public float getPrimaryModeHz() {
        return primaryModeHz;
    }

    public float getSecondaryModeHz() {
        return secondaryModeHz;
    }

    public float getOutputGain() {
        return outputGain;
    }

    public float getMinimumSpeedKmh() {
        return minimumSpeedKmh;
    }

    public float getMaximumSpeedKmh() {
        return maximumSpeedKmh;
    }

    public float getHeatRate() {
        return heatRate;
    }
}
