package de.jarexception.advancedsoundaddon.sound;

/** Cabin indicator-relay character and cadence. */
public final class IndicatorProfile {
    private static final IndicatorProfile STANDARD_RELAY = preset("STANDARD_RELAY",
            0.94F, 0.45F, 520, 390, 0.014F, 0.019F, 0.78F, 0.54F, false);
    private static final IndicatorProfile MODERN_RELAY = preset("MODERN_RELAY",
            0.92F, 0.44F, 760, 580, 0.009F, 0.012F, 0.52F, 0.465F, false);
    private static final IndicatorProfile HEAVY_RELAY = preset("HEAVY_RELAY",
            1.00F, 0.48F, 320, 235, 0.022F, 0.030F, 1.00F, 0.60F, false);
    private static final IndicatorProfile ELECTRONIC = preset("ELECTRONIC",
            0.90F, 0.43F, 2_180, 1_590, 0.024F, 0.032F, 0.10F, 0.27F, true);

    private final String presetName;
    private final float periodSeconds;
    private final float illuminatedSeconds;
    private final float engageFrequencyHz;
    private final float releaseFrequencyHz;
    private final float engageDecaySeconds;
    private final float releaseDecaySeconds;
    private final float noiseMix;
    private final float outputGain;
    private final boolean electronic;

    private IndicatorProfile(String presetName, float periodSeconds, float illuminatedSeconds,
                             float engageFrequencyHz, float releaseFrequencyHz,
                             float engageDecaySeconds, float releaseDecaySeconds,
                             float noiseMix, float outputGain, boolean electronic) {
        this.presetName = presetName;
        this.periodSeconds = periodSeconds;
        this.illuminatedSeconds = illuminatedSeconds;
        this.engageFrequencyHz = engageFrequencyHz;
        this.releaseFrequencyHz = releaseFrequencyHz;
        this.engageDecaySeconds = engageDecaySeconds;
        this.releaseDecaySeconds = releaseDecaySeconds;
        this.noiseMix = noiseMix;
        this.outputGain = outputGain;
        this.electronic = electronic;
    }

    private static IndicatorProfile preset(String name, float period, float illuminated,
                                           float engageFrequency, float releaseFrequency,
                                           float engageDecay, float releaseDecay,
                                           float noise, float output, boolean electronic) {
        return new IndicatorProfile(name, period, illuminated, engageFrequency,
                releaseFrequency, engageDecay, releaseDecay, noise, output, electronic);
    }

    public static IndicatorProfile defaultProfile() {
        return STANDARD_RELAY;
    }

    public static IndicatorProfile forPreset(String presetName) {
        switch (presetName) {
            case "STANDARD_RELAY": return STANDARD_RELAY;
            case "MODERN_RELAY": return MODERN_RELAY;
            case "HEAVY_RELAY": return HEAVY_RELAY;
            case "ELECTRONIC": return ELECTRONIC;
            default: throw new IllegalArgumentException("Unknown IndicatorPreset " + presetName);
        }
    }

    public String getPresetName() {
        return presetName;
    }

    float getPeriodSeconds() {
        return periodSeconds;
    }

    float getIlluminatedSeconds() {
        return illuminatedSeconds;
    }

    float getEngageFrequencyHz() {
        return engageFrequencyHz;
    }

    float getReleaseFrequencyHz() {
        return releaseFrequencyHz;
    }

    float getEngageDecaySeconds() {
        return engageDecaySeconds;
    }

    float getReleaseDecaySeconds() {
        return releaseDecaySeconds;
    }

    float getNoiseMix() {
        return noiseMix;
    }

    float getOutputGain() {
        return outputGain;
    }

    boolean isElectronic() {
        return electronic;
    }
}
