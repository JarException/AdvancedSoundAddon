package de.jarexception.advancedsoundaddon.sound;

/** Simulates the diaphragm, pressure and resonances of one horn bell. */
final class PhysicalHornUnit {
    enum Type { ELECTRIC_DISC, ELECTRIC_TRUMPET, AIR_TRUMPET, MARINE_TRUMPET }

    private static final double TWO_PI = Math.PI * 2.0;

    private final int sampleRate;
    private final double nominalFrequency;
    private final double brightness;
    private final double rasp;
    private final double spectralPresence;
    private final Type type;
    private final double attackAlpha;
    private final double releaseAlpha;
    private final Dsp.OnePoleLowPass frequencyWander;
    private final Dsp.OnePoleLowPass pressureNoiseLowPass;
    private final Dsp.OnePoleHighPass pressureNoiseHighPass;
    private final Dsp.OnePoleHighPass dcBlocker;
    private final Dsp.OnePoleLowPass radiationLowPass;
    private final Dsp.Resonator secondMode;
    private final Dsp.Resonator thirdMode;
    private final Dsp.Resonator bellMode;

    private double pressure;
    private double phase;
    private double membranePhase;
    private double supplyPhase;
    private int randomState;

    PhysicalHornUnit(int sampleRate, double nominalFrequency, double brightness,
                     double rasp, Type type, int seed) {
        this(sampleRate, nominalFrequency, brightness, rasp, type, seed, 0.0);
    }

    PhysicalHornUnit(int sampleRate, double nominalFrequency, double brightness,
                     double rasp, Type type, int seed, double spectralPresence) {
        this.sampleRate = sampleRate;
        this.nominalFrequency = nominalFrequency;
        this.brightness = brightness;
        this.rasp = rasp;
        this.spectralPresence = Dsp.clamp(spectralPresence, 0.0, 1.0);
        this.type = type;
        randomState = seed == 0 ? 0x6D2B79F5 : seed;
        phase = randomUnit() * TWO_PI;
        membranePhase = randomUnit() * TWO_PI;
        supplyPhase = randomUnit() * TWO_PI;

        double attackSeconds;
        double releaseSeconds;
        switch (type) {
            case AIR_TRUMPET:
                attackSeconds = 0.034;
                releaseSeconds = 0.052;
                break;
            case MARINE_TRUMPET:
                attackSeconds = 0.060;
                releaseSeconds = 0.095;
                break;
            case ELECTRIC_DISC:
                attackSeconds = 0.010;
                releaseSeconds = 0.025;
                break;
            case ELECTRIC_TRUMPET:
            default:
                attackSeconds = 0.016;
                releaseSeconds = 0.034;
                break;
        }
        attackAlpha = smoothingAlpha(attackSeconds);
        releaseAlpha = smoothingAlpha(releaseSeconds);
        frequencyWander = new Dsp.OnePoleLowPass(sampleRate,
                type == Type.AIR_TRUMPET || type == Type.MARINE_TRUMPET ? 5.0 : 10.0);
        pressureNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate,
                1_600.0 + brightness * 3_800.0);
        pressureNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 120.0);
        dcBlocker = new Dsp.OnePoleHighPass(sampleRate, 35.0);
        radiationLowPass = new Dsp.OnePoleLowPass(sampleRate,
                3_500.0 + brightness * 6_000.0);

        double bandwidthScale = type == Type.ELECTRIC_DISC ? 0.24 : 0.16;
        secondMode = new Dsp.Resonator(sampleRate, nominalFrequency * 2.01,
                nominalFrequency * bandwidthScale);
        thirdMode = new Dsp.Resonator(sampleRate, nominalFrequency * 3.04,
                nominalFrequency * (bandwidthScale + 0.04));
        double bellFrequency = nominalFrequency * (type == Type.ELECTRIC_DISC ? 4.35 : 4.72);
        bellMode = new Dsp.Resonator(sampleRate, bellFrequency,
                nominalFrequency * (0.34 + (1.0 - brightness) * 0.16));
    }

    double render(double targetPressure, double sharedSupply) {
        double target = Dsp.clamp(targetPressure, 0.0, 1.0);
        pressure += (target - pressure) * (target > pressure ? attackAlpha : releaseAlpha);

        double noise = bipolarNoise();
        double wander = frequencyWander.process(noise);
        double airDriven = type == Type.AIR_TRUMPET || type == Type.MARINE_TRUMPET ? 1.0 : 0.0;
        double pressurePitch = airDriven > 0.0
                ? 0.966 + 0.034 * Math.sqrt(Math.max(0.0, pressure))
                : 1.008 - 0.008 * pressure;
        double instability = (0.0012 + rasp * 0.0018) * wander
                + Math.sin(supplyPhase) * (0.0010 + airDriven * 0.0016);
        double frequency = nominalFrequency * pressurePitch
                * (1.0 + instability + sharedSupply * airDriven * 0.0025);
        phase = wrap(phase + TWO_PI * frequency / sampleRate);
        membranePhase = wrap(membranePhase + TWO_PI
                * (frequency * (2.006 + rasp * 0.006)) / sampleRate);
        supplyPhase = wrap(supplyPhase + TWO_PI
                * (type == Type.ELECTRIC_DISC ? 13.7 : 4.1) / sampleRate);

        double shapedNoise = pressureNoiseHighPass.process(
                pressureNoiseLowPass.process(noise));
        double fundamental = Math.sin(phase + Math.sin(membranePhase) * (0.035 + rasp * 0.055));
        double membrane = Math.sin(phase)
                + Math.sin(phase * 2.0 + 0.41) * (0.19 + brightness * 0.12)
                + Math.sin(phase * 3.0 + 1.07) * (0.07 + brightness * 0.08)
                + shapedNoise * (0.018 + rasp * 0.055);
        double closure = Math.tanh(membrane * (1.85 + rasp * 1.65));

        double second = secondMode.process(closure);
        double third = thirdMode.process(closure);
        double bell = bellMode.process(closure + shapedNoise * 0.08);
        double upperBellModes = Math.sin(phase * 2.0 + 0.19) * 0.42
                + Math.sin(phase * 3.0 + 0.61) * 0.65
                + Math.sin(phase * 4.0 + 1.10) * 0.52
                + Math.sin(phase * 5.0 + 0.34) * 0.24;
        double body = fundamental * (0.54 - brightness * 0.08 - spectralPresence * 0.45)
                + closure * (0.25 + rasp * 0.08) * (1.0 - spectralPresence * 0.70)
                + second * (0.34 + brightness * 0.22 + spectralPresence * 0.28)
                + third * (0.17 + brightness * 0.20 + spectralPresence * 0.34)
                + bell * (0.08 + brightness * 0.18 + spectralPresence * 0.25)
                + upperBellModes * spectralPresence * 1.36;

        double startupRattle = shapedNoise * (1.0 - pressure)
                * (0.08 + rasp * 0.15) * Math.min(1.0, pressure * 8.0);
        double continuousAir = shapedNoise * airDriven * (0.020 + rasp * 0.045);
        double pressureRipple = 1.0 + Math.sin(supplyPhase) * (0.008 + airDriven * 0.014)
                + sharedSupply * airDriven * 0.012;
        double output = (body + startupRattle + continuousAir) * pressure * pressureRipple;
        output = dcBlocker.process(output);
        return radiationLowPass.process(output);
    }

    double getPressure() {
        return pressure;
    }

    private double smoothingAlpha(double seconds) {
        return 1.0 - Math.exp(-1.0 / sampleRate / seconds);
    }

    private double bipolarNoise() {
        return randomUnit() * 2.0 - 1.0;
    }

    private double randomUnit() {
        int value = randomState;
        value ^= value << 13;
        value ^= value >>> 17;
        value ^= value << 5;
        randomState = value;
        return (value & 0x7FFFFFFF) / (double) Integer.MAX_VALUE;
    }

    private static double wrap(double value) {
        return value >= TWO_PI ? value % TWO_PI : value;
    }
}
