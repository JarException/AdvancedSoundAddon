package de.jarexception.advancedsoundaddon.sound;

/** Synthesizes diesel block radiation and two-stage injection acoustics. */
final class DieselCombustionVoice {
    private static final double TWO_PI = Math.PI * 2.0;

    private final int sampleRate;
    private final int cylinderCount;
    private final int bodyHarmonic;
    private final boolean heavyDuty;
    private final Dsp.ButterworthLowPass bodyNoiseLowPass;
    private final Dsp.OnePoleHighPass bodyNoiseHighPass;
    private final Dsp.OnePoleLowPass injectionNoiseLowPass;
    private final Dsp.OnePoleHighPass injectionNoiseHighPass;
    private final Dsp.Resonator lowerKnockResonator;
    private final Dsp.Resonator upperKnockResonator;

    private double firingPhase;
    private double chuffEnvelope;
    private double knockEnvelope;
    private double pendingMainStrength;
    private int pendingMainSamples = -1;
    private double roughness;
    private double roughnessTarget;
    private int roughnessSamples;
    private int randomState = 0x2D15E17;

    DieselCombustionVoice(EngineProfile profile, int sampleRate) {
        this.sampleRate = sampleRate;
        cylinderCount = profile.getFiringPattern().getCylinderCount();
        bodyHarmonic = cylinderCount <= 4 ? 4 : cylinderCount <= 6 ? 3 : 2;
        heavyDuty = profile.resolveAcousticMaxRpm(5_500) <= 3_000;

        bodyNoiseLowPass = new Dsp.ButterworthLowPass(sampleRate, heavyDuty ? 300 : 360);
        bodyNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, heavyDuty ? 48 : 58);
        injectionNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, heavyDuty ? 2_450 : 3_250);
        injectionNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, heavyDuty ? 330 : 440);
        lowerKnockResonator = new Dsp.Resonator(sampleRate, heavyDuty ? 610 : 790,
                heavyDuty ? 480 : 620);
        upperKnockResonator = new Dsp.Resonator(sampleRate, heavyDuty ? 1_280 : 1_670,
                heavyDuty ? 920 : 1_180);
    }

    double process(double rpm, double maximumRpm, double throttle, double load,
                   double ignitionBlend) {
        double rpmFraction = Dsp.clamp(rpm / Math.max(500.0, maximumRpm), 0.0, 1.15);
        double combustionDemand = Dsp.clamp(load * 0.82 + throttle * 0.18, 0.0, 1.0);
        double firingRate = Math.max(0.0, rpm) / 60.0 * cylinderCount * 0.5;
        firingPhase += firingRate / sampleRate;
        if (firingPhase >= 1.0) {
            firingPhase -= Math.floor(firingPhase);
            triggerPilot(combustionDemand, ignitionBlend, rpmFraction);
        }
        if (pendingMainSamples == 0) {
            chuffEnvelope = Math.min(2.2, chuffEnvelope + pendingMainStrength * 0.88);
            knockEnvelope = Math.min(2.0, knockEnvelope + pendingMainStrength * 0.76);
            pendingMainSamples = -1;
        } else if (pendingMainSamples > 0) {
            pendingMainSamples--;
        }

        updateRoughness();
        double random = randomSigned();
        double secondRandom = randomSigned();
        double bodyNoise = bodyNoiseHighPass.process(bodyNoiseLowPass.process(random));
        double injectionNoise = injectionNoiseHighPass.process(
                injectionNoiseLowPass.process(secondRandom));
        double lowerKnock = lowerKnockResonator.process(injectionNoise * knockEnvelope);
        double upperKnock = upperKnockResonator.process(injectionNoise * knockEnvelope);

        double bodyAngle = TWO_PI * firingPhase;
        double phaseWander = roughness * 0.34;
        double bodyTone = Math.sin(bodyAngle * bodyHarmonic + phaseWander) * 0.72
                + Math.sin(bodyAngle * (bodyHarmonic - 1) + 0.84 + phaseWander * 0.73) * 0.18
                + Math.sin(bodyAngle * (bodyHarmonic + 1) + 1.71 - phaseWander * 0.46) * 0.10;
        double lowOrderBody = Math.sin(bodyAngle * 2.0 + 0.38) * 0.08;
        double bodyGain = (heavyDuty ? 0.094 : 0.082)
                * (0.82 + combustionDemand * 0.18)
                * (1.0 + roughness * 0.18)
                * (1.0 - rpmFraction * 0.24)
                * ignitionBlend;

        double chuff = bodyNoise * (0.350 + Math.sqrt(chuffEnvelope) * 0.300);
        double knock = (injectionNoise * 0.50 + lowerKnock * 0.72 + upperKnock * 0.34)
                * knockEnvelope * (heavyDuty ? 0.115 : 0.135);
        double output = (bodyTone + lowOrderBody) * bodyGain
                + chuff * ignitionBlend + knock * ignitionBlend;

        double chuffSeconds = heavyDuty ? 0.034 : 0.029;
        chuffSeconds -= rpmFraction * (heavyDuty ? 0.010 : 0.009);
        chuffEnvelope *= Math.exp(-1.0 / sampleRate / Math.max(0.014, chuffSeconds));
        double knockSeconds = (heavyDuty ? 0.0052 : 0.0042) - rpmFraction * 0.0012;
        knockEnvelope *= Math.exp(-1.0 / sampleRate / Math.max(0.0025, knockSeconds));
        return output;
    }

    private void triggerPilot(double load, double ignitionBlend, double rpmFraction) {
        double cycleVariation = 0.84 + randomUnit() * 0.30;
        double strength = (0.64 + load * 0.36) * ignitionBlend * cycleVariation;
        chuffEnvelope = Math.min(2.2, chuffEnvelope + strength * 0.12);
        knockEnvelope = Math.min(2.0, knockEnvelope + strength * 0.24);
        pendingMainStrength = strength;
        double separationSeconds = 0.00105 - rpmFraction * 0.00034;
        pendingMainSamples = Math.max(1, (int) Math.round(sampleRate * separationSeconds));
    }

    private void updateRoughness() {
        if (roughnessSamples <= 0) {
            roughnessTarget = randomSigned();
            roughnessSamples = Math.max(1, (int) Math.round(sampleRate
                    * (0.045 + randomUnit() * 0.040)));
        }
        roughnessSamples--;
        roughness += (roughnessTarget - roughness)
                * (1.0 - Math.exp(-1.0 / sampleRate / 0.070));
    }

    void reset() {
        firingPhase = 0.0;
        chuffEnvelope = 0.0;
        knockEnvelope = 0.0;
        pendingMainStrength = 0.0;
        pendingMainSamples = -1;
        roughness = 0.0;
        roughnessTarget = 0.0;
        roughnessSamples = 0;
        bodyNoiseLowPass.reset();
        bodyNoiseHighPass.reset();
        injectionNoiseLowPass.reset();
        injectionNoiseHighPass.reset();
        lowerKnockResonator.reset();
        upperKnockResonator.reset();
    }

    private double randomSigned() {
        return randomUnit() * 2.0 - 1.0;
    }

    private double randomUnit() {
        int x = randomState;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        randomState = x;
        return (x & 0x7FFFFFFF) / (double) Integer.MAX_VALUE;
    }
}
