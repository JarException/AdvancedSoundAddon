package de.jarexception.advancedsoundaddon.sound;

/** Country-specific or fully custom emergency warning signal. */
public final class SirenProfile {
    public enum Pattern { STEP, TRIANGLE, SINE, SAW_UP, SAW_DOWN }
    enum Source { AIR_HORN, ELECTRONIC_SPEAKER, MECHANICAL_ROTOR }

    private static final float[] AIR_HARMONICS = {0.56F, 0.88F, 1.0F, 0.62F, 0.24F};
    private static final float[] GERMAN_SPEAKER_HARMONICS = {
            0.40F, 0.90F, 1.0F, 0.80F, 0.42F, 0.22F, 0.12F
    };
    private static final float[] FRENCH_HARMONICS = {0.48F, 0.74F, 1.0F, 0.72F, 0.28F};
    private static final float[] US_VEHICLE_HARMONICS = {0.68F, 1.0F, 0.54F, 0.24F, 0.09F};
    private static final float[] ELECTRONIC_HARMONICS = {0.64F, 1.0F, 0.56F, 0.26F, 0.10F};

    private final String presetName;
    private final Pattern pattern;
    private final float[] primaryFrequenciesHz;
    private final float[] secondaryFrequenciesHz;
    private final float[] durationsSeconds;
    private final float[] harmonics;
    private final float outputGain;
    private final float rasp;
    private final float flutterHz;
    private final float flutterDepth;
    private final float subharmonicGain;
    private final float audibleDistance;
    private final Source source;

    private SirenProfile(String presetName, Pattern pattern, float[] primary,
                         float[] secondary, float[] durations, float[] harmonics,
                         float outputGain, float rasp, float flutterHz,
                         float flutterDepth, float subharmonicGain,
                         float audibleDistance, Source source) {
        validate(pattern, primary, secondary, durations, harmonics, outputGain,
                rasp, flutterHz, flutterDepth, subharmonicGain, audibleDistance);
        if (source == Source.AIR_HORN && pattern != Pattern.STEP) {
            throw new IllegalArgumentException("AIR_HORN SirenSource requires STEP SirenPattern");
        }
        this.presetName = presetName;
        this.pattern = pattern;
        this.primaryFrequenciesHz = primary.clone();
        this.secondaryFrequenciesHz = secondary == null ? new float[0] : secondary.clone();
        this.durationsSeconds = durations.clone();
        this.harmonics = harmonics.clone();
        this.outputGain = outputGain;
        this.rasp = rasp;
        this.flutterHz = flutterHz;
        this.flutterDepth = flutterDepth;
        this.subharmonicGain = subharmonicGain;
        this.audibleDistance = audibleDistance;
        this.source = source;
    }

    public static SirenProfile forPreset(String presetName) {
        switch (presetName) {
            case "DE_POLICE":
                return step(presetName, new float[]{458, 608}, null,
                        new float[]{0.70F, 0.70F}, GERMAN_SPEAKER_HARMONICS,
                        1.55F, 0.18F, 9, 0.005F, 0, 105,
                        Source.ELECTRONIC_SPEAKER);
            case "DE_AMBULANCE":
                return step(presetName, new float[]{458, 608}, null,
                        new float[]{0.70F, 0.70F}, GERMAN_SPEAKER_HARMONICS,
                        1.52F, 0.16F, 8, 0.004F, 0, 105,
                        Source.ELECTRONIC_SPEAKER);
            case "DE_FIRE":
                return step(presetName, new float[]{435, 580}, new float[]{450, 600},
                        new float[]{0.75F, 0.75F}, AIR_HARMONICS, 0.90F, 0.52F, 11, 0.008F, 0, 112,
                        Source.AIR_HORN);
            case "FR_POLICE":
                return step(presetName, new float[]{435, 580}, null,
                        new float[]{0.55F, 0.55F}, FRENCH_HARMONICS, 1.0F, 0.20F, 6, 0.003F, 0, 100,
                        Source.ELECTRONIC_SPEAKER);
            case "FR_GENDARMERIE":
                return step(presetName, new float[]{435, 732}, null,
                        new float[]{0.55F, 0.55F}, FRENCH_HARMONICS, 1.02F, 0.21F, 6, 0.003F, 0, 102,
                        Source.ELECTRONIC_SPEAKER);
            case "FR_FIRE":
                return step(presetName, new float[]{435, 488}, null,
                        new float[]{1.10F, 1.10F}, AIR_HARMONICS, 1.06F, 0.42F, 8, 0.006F, 0, 108,
                        Source.AIR_HORN);
            case "FR_SAMU":
                return step(presetName, new float[]{435, 651}, null,
                        new float[]{0.55F, 0.55F}, FRENCH_HARMONICS, 1.02F, 0.22F, 6, 0.003F, 0, 102,
                        Source.ELECTRONIC_SPEAKER);
            case "FR_AMBULANCE":
                return step(presetName, new float[]{420, 516, 420, 0}, null,
                        new float[]{0.22F, 0.22F, 0.22F, 1.50F}, FRENCH_HARMONICS,
                        0.94F, 0.18F, 5, 0.002F, 0, 92,
                        Source.ELECTRONIC_SPEAKER);
            case "US_WAIL":
                return sweep(presetName, Pattern.SINE, 725, 1_800, 60.0F / 15.0F,
                        1.04F, 0.18F, 3.2F, 0.008F, 0, 112);
            case "US_YELP":
                return sweep(presetName, Pattern.TRIANGLE, 725, 1_800, 60.0F / 220.0F,
                        1.06F, 0.20F, 8.0F, 0.009F, 0, 112);
            case "US_HI_LO":
                return step(presetName, new float[]{725, 1_800}, null,
                        new float[]{60.0F / 140.0F, 60.0F / 140.0F},
                        US_VEHICLE_HARMONICS, 1.02F, 0.16F, 4, 0.003F, 0, 108,
                        Source.ELECTRONIC_SPEAKER);
            case "US_PRIORITY":
                return sweep(presetName, Pattern.TRIANGLE, 725, 1_800, 60.0F / 1_300.0F,
                        1.05F, 0.24F, 12, 0.010F, 0, 112);
            case "US_RUMBLER_WAIL":
                return sweep(presetName, Pattern.SINE, 725, 1_800, 60.0F / 15.0F,
                        1.04F, 0.22F, 3.2F, 0.008F, 0.38F, 118);
            case "US_RUMBLER_YELP":
                return sweep(presetName, Pattern.TRIANGLE, 725, 1_800, 60.0F / 220.0F,
                        1.06F, 0.24F, 8.0F, 0.010F, 0.38F, 118);
            case "US_Q_SIREN":
                return new SirenProfile(presetName, Pattern.SAW_UP,
                        new float[]{480, 1_600}, new float[0], new float[]{5.8F},
                        new float[]{0.66F, 1.0F, 0.72F, 0.43F, 0.25F, 0.13F},
                        1.12F, 0.60F, 7.0F, 0.014F, 0.22F, 120,
                        Source.MECHANICAL_ROTOR);
            case "EU_HI_LO":
                return step(presetName, new float[]{440, 585}, null,
                        new float[]{0.65F, 0.65F}, ELECTRONIC_HARMONICS, 0.98F, 0.20F, 5, 0.003F, 0, 100,
                        Source.ELECTRONIC_SPEAKER);
            default:
                throw new IllegalArgumentException("Unknown SirenPreset " + presetName);
        }
    }

    private static SirenProfile step(String name, float[] primary, float[] secondary,
                                     float[] durations, float[] harmonics, float gain,
                                     float rasp, float flutter, float flutterDepth,
                                     float subharmonic, float distance, Source source) {
        return new SirenProfile(name, Pattern.STEP, primary, secondary, durations,
                harmonics, gain, rasp, flutter, flutterDepth, subharmonic, distance, source);
    }

    private static SirenProfile sweep(String name, Pattern pattern, float low, float high,
                                      float cycle, float gain, float rasp, float flutter,
                                      float flutterDepth, float subharmonic, float distance) {
        return new SirenProfile(name, pattern, new float[]{low, high}, new float[0],
                new float[]{cycle}, US_VEHICLE_HARMONICS, gain, rasp, flutter,
                flutterDepth, subharmonic, distance, Source.ELECTRONIC_SPEAKER);
    }

    public static SirenProfile custom(String patternName, float[] primary, float[] secondary,
                                      float[] durations, float[] harmonics,
                                      Float gain, Float rasp, Float flutter,
                                      Float flutterDepth, Float subharmonic,
                                      Float distance) {
        return custom(null, patternName, primary, secondary, durations, harmonics,
                gain, rasp, flutter, flutterDepth, subharmonic, distance);
    }

    public static SirenProfile custom(String sourceName, String patternName,
                                      float[] primary, float[] secondary,
                                      float[] durations, float[] harmonics,
                                      Float gain, Float rasp, Float flutter,
                                      Float flutterDepth, Float subharmonic,
                                      Float distance) {
        if (primary == null || primary.length == 0) {
            throw new IllegalArgumentException("CUSTOM SirenPreset requires SirenFrequenciesHz");
        }
        Pattern pattern = parsePattern(patternName == null ? "STEP" : patternName);
        float[] resolvedDurations = durations;
        if (resolvedDurations == null || resolvedDurations.length == 0) {
            resolvedDurations = pattern == Pattern.STEP
                    ? fill(primary.length, 0.55F) : new float[]{1.5F};
        }
        return new SirenProfile("CUSTOM", pattern, primary, secondary,
                resolvedDurations,
                harmonics == null ? ELECTRONIC_HARMONICS : harmonics,
                gain == null ? 1.0F : gain,
                rasp == null ? 0.20F : rasp,
                flutter == null ? 5.0F : flutter,
                flutterDepth == null ? 0.004F : flutterDepth,
                subharmonic == null ? 0.0F : subharmonic,
                distance == null ? 100.0F : distance,
                sourceName == null ? Source.ELECTRONIC_SPEAKER : parseSource(sourceName));
    }

    public SirenProfile withOverrides(String patternName, float[] primary, float[] secondary,
                                      float[] durations, float[] harmonicOverrides,
                                      Float gain, Float raspOverride, Float flutter,
                                      Float flutterDepthOverride, Float subharmonic,
                                      Float distance) {
        return withOverrides(null, patternName, primary, secondary, durations,
                harmonicOverrides, gain, raspOverride, flutter,
                flutterDepthOverride, subharmonic, distance);
    }

    public SirenProfile withOverrides(String sourceName, String patternName,
                                      float[] primary, float[] secondary,
                                      float[] durations, float[] harmonicOverrides,
                                      Float gain, Float raspOverride, Float flutter,
                                      Float flutterDepthOverride, Float subharmonic,
                                      Float distance) {
        return new SirenProfile(presetName,
                patternName == null ? pattern : parsePattern(patternName),
                primary == null ? primaryFrequenciesHz : primary,
                secondary == null ? secondaryFrequenciesHz : secondary,
                durations == null ? durationsSeconds : durations,
                harmonicOverrides == null ? harmonics : harmonicOverrides,
                gain == null ? outputGain : gain,
                raspOverride == null ? rasp : raspOverride,
                flutter == null ? flutterHz : flutter,
                flutterDepthOverride == null ? flutterDepth : flutterDepthOverride,
                subharmonic == null ? subharmonicGain : subharmonic,
                distance == null ? audibleDistance : distance,
                sourceName == null ? source : parseSource(sourceName));
    }

    public static Pattern parsePattern(String value) {
        try {
            return Pattern.valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unknown SirenPattern " + value);
        }
    }

    private static Source parseSource(String value) {
        try {
            return Source.valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unknown SirenSource " + value);
        }
    }

    private static float[] fill(int count, float value) {
        float[] values = new float[count];
        for (int i = 0; i < values.length; i++) values[i] = value;
        return values;
    }

    private static void validate(Pattern pattern, float[] primary, float[] secondary,
                                 float[] durations, float[] harmonics, float gain,
                                 float rasp, float flutter, float flutterDepth,
                                 float subharmonic, float distance) {
        if (primary == null || primary.length == 0 || primary.length > 12) {
            throw new IllegalArgumentException("SirenFrequenciesHz requires 1 to 12 values");
        }
        if (pattern != Pattern.STEP && primary.length != 2) {
            throw new IllegalArgumentException("Sweep SirenPattern requires exactly 2 frequencies");
        }
        if (secondary != null && secondary.length != 0 && secondary.length != primary.length) {
            throw new IllegalArgumentException("SirenSecondaryFrequenciesHz must match SirenFrequenciesHz");
        }
        for (float frequency : primary) validateFrequency(frequency);
        if (secondary != null) for (float frequency : secondary) validateFrequency(frequency);
        int requiredDurations = pattern == Pattern.STEP ? primary.length : 1;
        if (durations == null || (durations.length != 1 && durations.length != requiredDurations)) {
            throw new IllegalArgumentException("SirenDurationsSeconds must have 1 or "
                    + requiredDurations + " values");
        }
        for (float duration : durations) range("SirenDurationsSeconds", duration, 0.03F, 20.0F);
        if (harmonics == null || harmonics.length == 0 || harmonics.length > 8) {
            throw new IllegalArgumentException("SirenHarmonics requires 1 to 8 values");
        }
        for (float harmonic : harmonics) range("SirenHarmonics", harmonic, 0, 1.5F);
        range("SirenGain", gain, 0.05F, 2.5F);
        range("SirenRasp", rasp, 0, 1);
        range("SirenFlutterHz", flutter, 0, 40);
        range("SirenFlutterDepth", flutterDepth, 0, 0.10F);
        range("SirenSubharmonicGain", subharmonic, 0, 1);
        range("SirenAudibleDistance", distance, 10, 180);
    }

    private static void validateFrequency(float frequency) {
        if (frequency != 0) range("SirenFrequenciesHz", frequency, 120, 2_400);
    }

    private static void range(String name, float value, float min, float max) {
        if (!Float.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
    }

    public String getPresetName() { return presetName; }
    public Pattern getPattern() { return pattern; }
    public float[] getPrimaryFrequenciesHz() { return primaryFrequenciesHz.clone(); }
    public float[] getSecondaryFrequenciesHz() { return secondaryFrequenciesHz.clone(); }
    public float[] getDurationsSeconds() { return durationsSeconds.clone(); }
    public float[] getHarmonics() { return harmonics.clone(); }
    public float getOutputGain() { return outputGain; }
    public float getRasp() { return rasp; }
    public float getFlutterHz() { return flutterHz; }
    public float getFlutterDepth() { return flutterDepth; }
    public float getSubharmonicGain() { return subharmonicGain; }
    public float getAudibleDistance() { return audibleDistance; }
    Source getSource() { return source; }
    public String getSourceName() { return source.name(); }
}
