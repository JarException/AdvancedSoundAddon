package de.jarexception.advancedsoundaddon.sound;

/** Optional vehicle-level tyre scrub and skid acoustics. */
public final class TireSquealProfile {
    private static final TireSquealProfile STREET_TIRE = new TireSquealProfile(
            "STREET_TIRE", 0.74F, 0.88F, 1_080, 2_420, 6_100, 0.38F, 2.5F);
    private static final TireSquealProfile PERFORMANCE_TIRE = new TireSquealProfile(
            "PERFORMANCE_TIRE", 0.68F, 0.84F, 1_340, 3_080, 7_300, 0.44F, 3.0F);
    private static final TireSquealProfile RACE_SLICK = new TireSquealProfile(
            "RACE_SLICK", 0.62F, 0.82F, 1_620, 3_860, 8_600, 0.47F, 4.0F);
    private static final TireSquealProfile HEAVY_TIRE = new TireSquealProfile(
            "HEAVY_TIRE", 0.76F, 0.90F, 720, 1_580, 4_500, 0.43F, 2.0F);

    private final String presetName;
    private final float activationSlip;
    private final float squealActivationSlip;
    private final float primaryModeHz;
    private final float secondaryModeHz;
    private final float noiseCutoffHz;
    private final float outputGain;
    private final float minimumRollingSpeedKmh;

    private TireSquealProfile(String presetName, float activationSlip,
                             float squealActivationSlip,
                             float primaryModeHz, float secondaryModeHz,
                             float noiseCutoffHz, float outputGain,
                             float minimumRollingSpeedKmh) {
        this.presetName = presetName;
        this.activationSlip = activationSlip;
        this.squealActivationSlip = squealActivationSlip;
        this.primaryModeHz = primaryModeHz;
        this.secondaryModeHz = secondaryModeHz;
        this.noiseCutoffHz = noiseCutoffHz;
        this.outputGain = outputGain;
        this.minimumRollingSpeedKmh = minimumRollingSpeedKmh;
    }

    public static TireSquealProfile forPreset(String presetName) {
        switch (presetName) {
            case "STREET_TIRE":
                return STREET_TIRE;
            case "PERFORMANCE_TIRE":
                return PERFORMANCE_TIRE;
            case "RACE_SLICK":
                return RACE_SLICK;
            case "HEAVY_TIRE":
                return HEAVY_TIRE;
            default:
                throw new IllegalArgumentException("Unknown TireSquealPreset " + presetName);
        }
    }

    public static TireSquealProfile defaultProfile() {
        return STREET_TIRE;
    }

    double resolveIntensity(double slip, double speedKmh,
                            double throttle, boolean brakeApplied) {
        double normalizedSlip = Dsp.smoothStep(
                (slip - activationSlip) / Math.max(0.01, 1.0 - activationSlip));
        double rolling = Dsp.smoothStep(
                (Math.abs(speedKmh) - minimumRollingSpeedKmh) / 5.0);
        double burnout = throttle > 0.52
                ? Dsp.smoothStep((slip - 0.82) / 0.16) * 0.86 : 0.0;
        double braking = brakeApplied ? Math.min(1.0, rolling + 0.12) : rolling;
        return normalizedSlip * Math.max(burnout, braking);
    }

    double resolveSquealIntensity(double slip, double speedKmh,
                                  double throttle, boolean brakeApplied) {
        double severeSlip = Dsp.smoothStep((slip - squealActivationSlip)
                / Math.max(0.01, 1.0 - squealActivationSlip));
        double rolling = Dsp.smoothStep((Math.abs(speedKmh)
                - minimumRollingSpeedKmh - 1.5) / 7.0);
        double wheelspin = throttle > 0.58
                ? Dsp.smoothStep((slip - 0.91) / 0.08) * 0.82 : 0.0;
        double lockup = brakeApplied ? Math.min(1.0, rolling + 0.18) : rolling;
        return severeSlip * Math.max(wheelspin, lockup);
    }

    public String getPresetName() {
        return presetName;
    }

    public float getActivationSlip() {
        return activationSlip;
    }

    public float getPrimaryModeHz() {
        return primaryModeHz;
    }

    public float getSquealActivationSlip() {
        return squealActivationSlip;
    }

    public float getSecondaryModeHz() {
        return secondaryModeHz;
    }

    public float getNoiseCutoffHz() {
        return noiseCutoffHz;
    }

    public float getOutputGain() {
        return outputGain;
    }

    public float getMinimumRollingSpeedKmh() {
        return minimumRollingSpeedKmh;
    }
}
