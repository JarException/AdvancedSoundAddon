package de.jarexception.advancedsoundaddon.sound;

/** Vehicle-level rotor acoustics, intentionally independent from the engine preset. */
public final class RotorProfile {
    private static final float ROTOR_OUTPUT_BOOST = 4.0F;

    private final String presetName;
    private final int bladeCount;
    private final float nominalRotorRpm;
    private final float outputGain;
    private final float tailRotorRatio;

    private RotorProfile(String presetName, int bladeCount, float nominalRotorRpm,
                         float outputGain, float tailRotorRatio) {
        this.presetName = presetName;
        this.bladeCount = bladeCount;
        this.nominalRotorRpm = nominalRotorRpm;
        this.outputGain = outputGain;
        this.tailRotorRatio = tailRotorRatio;
    }

    public static RotorProfile forPreset(String presetName) {
        switch (presetName) {
            case "HELICOPTER_2_BLADE":
                return new RotorProfile(presetName, 2, 394,
                        1.08F * ROTOR_OUTPUT_BOOST, 6.45F);
            case "HELICOPTER_4_BLADE":
                return new RotorProfile(presetName, 4, 383,
                        0.92F * ROTOR_OUTPUT_BOOST, 8.20F);
            default:
                throw new IllegalArgumentException("Unknown RotorPreset " + presetName);
        }
    }

    public String getPresetName() {
        return presetName;
    }

    public int getBladeCount() {
        return bladeCount;
    }

    public float getNominalRotorRpm() {
        return nominalRotorRpm;
    }

    public float getOutputGain() {
        return outputGain;
    }

    public float getTailRotorRatio() {
        return tailRotorRatio;
    }

    public float getNominalBladePassHz() {
        return nominalRotorRpm / 60.0F * bladeCount;
    }
}
