package de.jarexception.advancedsoundaddon.sound;

/** Optional vehicle-level compressed-air brake acoustics. */
public final class AirBrakeProfile {
    private final String presetName;
    private final float outputGain;
    private final float nozzleCutoffHz;
    private final float bodyResonanceHz;
    private final float chargeSeconds;
    private final float automaticPurgeSeconds;

    private AirBrakeProfile(String presetName, float outputGain, float nozzleCutoffHz,
                            float bodyResonanceHz, float chargeSeconds,
                            float automaticPurgeSeconds) {
        this.presetName = presetName;
        this.outputGain = outputGain;
        this.nozzleCutoffHz = nozzleCutoffHz;
        this.bodyResonanceHz = bodyResonanceHz;
        this.chargeSeconds = chargeSeconds;
        this.automaticPurgeSeconds = automaticPurgeSeconds;
    }

    public static AirBrakeProfile forPreset(String presetName) {
        switch (presetName) {
            case "TRUCK_AIR_BRAKE":
                return new AirBrakeProfile(presetName, 1.28F, 6_200, 430, 25, 14);
            case "BUS_AIR_BRAKE":
                return new AirBrakeProfile(presetName, 1.16F, 7_400, 560, 28, 16);
            default:
                throw new IllegalArgumentException("Unknown AirBrakePreset " + presetName);
        }
    }

    public String getPresetName() {
        return presetName;
    }

    public float getOutputGain() {
        return outputGain;
    }

    public float getNozzleCutoffHz() {
        return nozzleCutoffHz;
    }

    public float getBodyResonanceHz() {
        return bodyResonanceHz;
    }

    public float getChargeSeconds() {
        return chargeSeconds;
    }

    public float getAutomaticPurgeSeconds() {
        return automaticPurgeSeconds;
    }
}
