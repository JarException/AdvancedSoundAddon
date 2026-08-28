package de.jarexception.advancedsoundaddon.sound;

import java.util.Arrays;

/** Vehicle horn construction and envelope, independent from the powertrain. */
public final class HornProfile {
    enum Source { ELECTRIC_DISC, ELECTRIC_TRUMPET, AIR_TRUMPET, MARINE_TRUMPET }

    private static final HornProfile COMPACT_CAR = preset("COMPACT_CAR",
            new float[]{420, 500}, new float[]{1.0F, 0.70F},
            0.012F, 0.34F, 0.11F, 0.72F, 0.68F, 0.18F, 52,
            Source.ELECTRIC_DISC);
    private static final HornProfile STANDARD_CAR = preset("STANDARD_CAR",
            new float[]{410, 500}, new float[]{1.0F, 0.78F},
            0.014F, 0.44F, 0.13F, 0.82F, 0.60F, 0.20F, 58,
            Source.ELECTRIC_TRUMPET);
    private static final HornProfile LUXURY_CAR = preset("LUXURY_CAR",
            new float[]{400, 500}, new float[]{1.0F, 0.82F},
            0.018F, 0.52F, 0.17F, 0.84F, 0.46F, 0.12F, 62,
            Source.ELECTRIC_TRUMPET);
    private static final HornProfile SPORT_CAR = preset("SPORT_CAR",
            new float[]{500, 620}, new float[]{1.0F, 0.72F},
            0.009F, 0.36F, 0.10F, 0.86F, 0.82F, 0.22F, 60,
            Source.ELECTRIC_DISC);
    private static final HornProfile CLASSIC_CAR = preset("CLASSIC_CAR",
            new float[]{335, 410}, new float[]{1.0F, 0.66F},
            0.020F, 0.48F, 0.18F, 0.76F, 0.42F, 0.44F, 52,
            Source.ELECTRIC_DISC);
    private static final HornProfile MOTORCYCLE = preset("MOTORCYCLE",
            new float[]{435}, new float[]{1.0F},
            0.006F, 0.27F, 0.08F, 0.68F, 0.78F, 0.25F, 44,
            Source.ELECTRIC_DISC);
    private static final HornProfile TRUCK_AIR = preset("TRUCK_AIR",
            new float[]{170, 220}, new float[]{1.0F, 0.84F},
            0.028F, 0.78F, 0.24F, 1.08F, 0.38F, 0.58F, 82,
            Source.AIR_TRUMPET);
    private static final HornProfile BUS_AIR = preset("BUS_AIR",
            new float[]{250, 315}, new float[]{1.0F, 0.76F},
            0.024F, 0.64F, 0.20F, 0.98F, 0.42F, 0.43F, 74,
            Source.AIR_TRUMPET);
    private static final HornProfile UTILITY = preset("UTILITY",
            new float[]{360}, new float[]{1.0F},
            0.007F, 0.25F, 0.08F, 0.66F, 0.70F, 0.36F, 42,
            Source.ELECTRIC_DISC);
    private static final HornProfile MARINE = preset("MARINE",
            new float[]{180, 240}, new float[]{1.0F, 0.58F},
            0.040F, 0.92F, 0.30F, 1.04F, 0.28F, 0.48F, 88,
            Source.MARINE_TRUMPET);

    private final String presetName;
    private final float[] frequenciesHz;
    private final float[] relativeGains;
    private final float attackSeconds;
    private final float holdSeconds;
    private final float releaseSeconds;
    private final float outputGain;
    private final float brightness;
    private final float rasp;
    private final float audibleDistance;
    private final Source source;

    private HornProfile(String presetName, float[] frequenciesHz, float[] relativeGains,
                        float attackSeconds, float holdSeconds, float releaseSeconds,
                        float outputGain, float brightness, float rasp,
                        float audibleDistance, Source source) {
        validate(frequenciesHz, relativeGains, attackSeconds, holdSeconds, releaseSeconds,
                outputGain, brightness, rasp, audibleDistance);
        this.presetName = presetName;
        this.frequenciesHz = frequenciesHz.clone();
        this.relativeGains = relativeGains.clone();
        this.attackSeconds = attackSeconds;
        this.holdSeconds = holdSeconds;
        this.releaseSeconds = releaseSeconds;
        this.outputGain = outputGain;
        this.brightness = brightness;
        this.rasp = rasp;
        this.audibleDistance = audibleDistance;
        this.source = source;
    }

    private static HornProfile preset(String name, float[] frequencies, float[] gains,
                                      float attack, float hold, float release, float output,
                                      float brightness, float rasp, float distance,
                                      Source source) {
        return new HornProfile(name, frequencies, gains, attack, hold, release,
                output, brightness, rasp, distance, source);
    }

    public static HornProfile forPreset(String presetName) {
        switch (presetName) {
            case "COMPACT_CAR": return COMPACT_CAR;
            case "STANDARD_CAR": return STANDARD_CAR;
            case "LUXURY_CAR": return LUXURY_CAR;
            case "SPORT_CAR": return SPORT_CAR;
            case "CLASSIC_CAR": return CLASSIC_CAR;
            case "MOTORCYCLE": return MOTORCYCLE;
            case "TRUCK_AIR": return TRUCK_AIR;
            case "BUS_AIR": return BUS_AIR;
            case "UTILITY": return UTILITY;
            case "MARINE": return MARINE;
            default: throw new IllegalArgumentException("Unknown HornPreset " + presetName);
        }
    }

    public HornProfile withOverrides(float[] frequencies, float[] gains,
                                     Float attack, Float hold, Float release,
                                     Float output, Float brightnessOverride,
                                     Float raspOverride, Float distance) {
        return withOverrides(null, frequencies, gains, attack, hold, release,
                output, brightnessOverride, raspOverride, distance);
    }

    public HornProfile withOverrides(String sourceName, float[] frequencies, float[] gains,
                                     Float attack, Float hold, Float release,
                                     Float output, Float brightnessOverride,
                                     Float raspOverride, Float distance) {
        float[] resolvedFrequencies = frequencies == null ? frequenciesHz : frequencies;
        float[] resolvedGains = gains == null
                ? (frequencies == null || frequencies.length == relativeGains.length
                ? relativeGains : equalGains(frequencies.length)) : gains;
        return new HornProfile(presetName, resolvedFrequencies, resolvedGains,
                attack == null ? attackSeconds : attack,
                hold == null ? holdSeconds : hold,
                release == null ? releaseSeconds : release,
                output == null ? outputGain : output,
                brightnessOverride == null ? brightness : brightnessOverride,
                raspOverride == null ? rasp : raspOverride,
                distance == null ? audibleDistance : distance,
                sourceName == null ? source : parseSource(sourceName));
    }

    public static HornProfile custom(float[] frequencies, float[] gains,
                                     Float attack, Float hold, Float release,
                                     Float output, Float brightness, Float rasp,
                                     Float distance) {
        return custom(null, frequencies, gains, attack, hold, release,
                output, brightness, rasp, distance);
    }

    public static HornProfile custom(String sourceName, float[] frequencies, float[] gains,
                                     Float attack, Float hold, Float release,
                                     Float output, Float brightness, Float rasp,
                                     Float distance) {
        if (frequencies == null || frequencies.length == 0) {
            throw new IllegalArgumentException("CUSTOM HornPreset requires HornFrequenciesHz");
        }
        return new HornProfile("CUSTOM", frequencies,
                gains == null ? equalGains(frequencies.length) : gains,
                attack == null ? 0.012F : attack,
                hold == null ? 0.42F : hold,
                release == null ? 0.13F : release,
                output == null ? 0.82F : output,
                brightness == null ? 0.60F : brightness,
                rasp == null ? 0.20F : rasp,
                distance == null ? 58.0F : distance,
                sourceName == null ? Source.ELECTRIC_TRUMPET : parseSource(sourceName));
    }

    private static Source parseSource(String value) {
        try {
            return Source.valueOf(value.trim().toUpperCase().replace('-', '_'));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Unknown HornSource " + value);
        }
    }

    private static float[] equalGains(int count) {
        float[] gains = new float[count];
        Arrays.fill(gains, 1.0F / Math.max(1, count));
        gains[0] = 1.0F;
        return gains;
    }

    private static void validate(float[] frequencies, float[] gains,
                                 float attack, float hold, float release,
                                 float output, float brightness, float rasp,
                                 float distance) {
        if (frequencies == null || frequencies.length == 0 || frequencies.length > 4) {
            throw new IllegalArgumentException("HornFrequenciesHz requires 1 to 4 values");
        }
        if (gains == null || gains.length != frequencies.length) {
            throw new IllegalArgumentException("HornRelativeGains must match HornFrequenciesHz");
        }
        for (float frequency : frequencies) {
            requireFiniteRange("HornFrequenciesHz", frequency, 70, 1_600);
        }
        for (float gain : gains) requireFiniteRange("HornRelativeGains", gain, 0, 2);
        requireFiniteRange("HornAttackSeconds", attack, 0.001F, 0.30F);
        requireFiniteRange("HornHoldSeconds", hold, 0.02F, 3.0F);
        requireFiniteRange("HornReleaseSeconds", release, 0.01F, 1.0F);
        requireFiniteRange("HornGain", output, 0.05F, 2.5F);
        requireFiniteRange("HornBrightness", brightness, 0, 1);
        requireFiniteRange("HornRasp", rasp, 0, 1);
        requireFiniteRange("HornAudibleDistance", distance, 8, 160);
    }

    private static void requireFiniteRange(String name, float value, float min, float max) {
        if (!Float.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
    }

    public String getPresetName() { return presetName; }
    public float[] getFrequenciesHz() { return frequenciesHz.clone(); }
    public float[] getRelativeGains() { return relativeGains.clone(); }
    public float getAttackSeconds() { return attackSeconds; }
    public float getHoldSeconds() { return holdSeconds; }
    public float getReleaseSeconds() { return releaseSeconds; }
    public float getOutputGain() { return outputGain; }
    public float getBrightness() { return brightness; }
    public float getRasp() { return rasp; }
    public float getAudibleDistance() { return audibleDistance; }
    Source getSource() { return source; }
    public String getSourceName() { return source.name(); }
}
