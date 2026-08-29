package de.jarexception.advancedsoundaddon.sound;

/** Vehicle-level reverse warning acoustics, independent from the engine preset. */
public final class ReverseWarningProfile {
    enum Source { TONAL, BROADBAND }

    private final String presetName;
    private final Source source;
    private final float centerFrequencyHz;
    private final float bandLowHz;
    private final float bandHighHz;
    private final float pulsesPerMinute;
    private final float dutyCycle;
    private final float outputGain;
    private final float audibleDistance;

    private ReverseWarningProfile(String presetName, Source source,
                                  float centerFrequencyHz, float bandLowHz,
                                  float bandHighHz, float pulsesPerMinute,
                                  float dutyCycle, float outputGain,
                                  float audibleDistance) {
        this.presetName = presetName;
        this.source = source;
        this.centerFrequencyHz = centerFrequencyHz;
        this.bandLowHz = bandLowHz;
        this.bandHighHz = bandHighHz;
        this.pulsesPerMinute = pulsesPerMinute;
        this.dutyCycle = dutyCycle;
        this.outputGain = outputGain;
        this.audibleDistance = audibleDistance;
    }

    public static ReverseWarningProfile forPreset(String presetName) {
        switch (presetName) {
            case "TONAL":
            case "CLASSIC_TONAL":
            case "TONAL_BEEPER":
                return new ReverseWarningProfile("TONAL_BEEPER", Source.TONAL,
                        1_250, 0, 0, 60, 0.48F, 0.48F, 72);
            case "WHITE_NOISE":
            case "BROADBAND_WHITE_NOISE":
            case "BROADBAND":
                return new ReverseWarningProfile("BROADBAND", Source.BROADBAND,
                        1_050, 600, 1_600, 60, 0.48F, 0.36F, 64);
            default:
                throw new IllegalArgumentException("Unknown ReverseWarningPreset " + presetName);
        }
    }

    public String getPresetName() {
        return presetName;
    }

    Source getSource() {
        return source;
    }

    public float getCenterFrequencyHz() {
        return centerFrequencyHz;
    }

    float getBandLowHz() {
        return bandLowHz;
    }

    float getBandHighHz() {
        return bandHighHz;
    }

    public float getPulsesPerMinute() {
        return pulsesPerMinute;
    }

    public float getDutyCycle() {
        return dutyCycle;
    }

    public float getOutputGain() {
        return outputGain;
    }

    public float getAudibleDistance() {
        return audibleDistance;
    }
}
