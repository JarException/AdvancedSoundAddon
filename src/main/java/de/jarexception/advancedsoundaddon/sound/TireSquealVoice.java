package de.jarexception.advancedsoundaddon.sound;

/** Synthesizes tyre friction and stick-slip from synchronized wheel data. */
final class TireSquealVoice {
    private static final double TWO_PI = Math.PI * 2.0;

    private final TireSquealProfile profile;
    private final int sampleRate;
    private final Dsp.OnePoleLowPass contactNoiseLowPass;
    private final Dsp.OnePoleHighPass contactNoiseHighPass;
    private final Dsp.OnePoleLowPass rubberNoiseLowPass;
    private final Dsp.OnePoleHighPass rubberNoiseHighPass;
    private final Dsp.OnePoleLowPass adhesionNoiseLowPass;
    private final Dsp.Resonator primaryBeltMode;
    private final Dsp.Resonator secondaryBeltMode;
    private final Dsp.OnePoleLowPass squealExciterLowPass;
    private final Dsp.OnePoleHighPass squealExciterHighPass;
    private final Dsp.Resonator lowSquealMode;
    private final Dsp.Resonator middleSquealMode;
    private final Dsp.Resonator highSquealMode;
    private final Dsp.OnePoleHighPass dcBlocker;

    private long lastTelemetryTimestamp;
    private double targetIntensity;
    private double intensity;
    private double targetSquealIntensity;
    private double squealIntensity;
    private double currentSpeedKmh;
    private double currentSlip;
    private double throttleBias;
    private double brakingBias;
    private double cycleCharacter = 1.0;
    private double chatterPhase;
    private double treadPhase;
    private double squealFlutterPhase;
    private volatile boolean silent = true;
    private int randomState = 0x2F61B9D3;
    private int squealRandomState = 0x51C4A7E9;

    TireSquealVoice(TireSquealProfile profile, int sampleRate,
                    EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        contactNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate,
                profile.getNoiseCutoffHz());
        contactNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 310);
        rubberNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate,
                Math.min(2_200.0, profile.getNoiseCutoffHz() * 0.42));
        rubberNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 115);
        adhesionNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 31);
        primaryBeltMode = new Dsp.Resonator(sampleRate, profile.getPrimaryModeHz(),
                Math.max(420.0, profile.getPrimaryModeHz() * 0.58));
        secondaryBeltMode = new Dsp.Resonator(sampleRate, profile.getSecondaryModeHz(),
                Math.max(720.0, profile.getSecondaryModeHz() * 0.72));
        squealExciterLowPass = new Dsp.OnePoleLowPass(sampleRate,
                Math.min(sampleRate * 0.42, profile.getNoiseCutoffHz() * 1.22));
        squealExciterHighPass = new Dsp.OnePoleHighPass(sampleRate,
                Math.max(520.0, profile.getPrimaryModeHz() * 0.48));
        lowSquealMode = new Dsp.Resonator(sampleRate,
                profile.getPrimaryModeHz() * 1.08,
                Math.max(210.0, profile.getPrimaryModeHz() * 0.24));
        middleSquealMode = new Dsp.Resonator(sampleRate,
                profile.getPrimaryModeHz() * 1.62,
                Math.max(320.0, profile.getPrimaryModeHz() * 0.30));
        highSquealMode = new Dsp.Resonator(sampleRate,
                profile.getSecondaryModeHz() * 1.06,
                Math.max(520.0, profile.getSecondaryModeHz() * 0.31));
        dcBlocker = new Dsp.OnePoleHighPass(sampleRate, 82);
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        processTelemetry(telemetry);
        byte[] pcm = new byte[sampleCount * 2];
        double attackAlpha = 1.0 - Math.exp(-1.0 / sampleRate / 0.018);
        double releaseAlpha = 1.0 - Math.exp(-1.0 / sampleRate / 0.085);
        double squealAttackAlpha = 1.0 - Math.exp(-1.0 / sampleRate / 0.012);
        double squealReleaseAlpha = 1.0 - Math.exp(-1.0 / sampleRate / 0.115);
        for (int sample = 0; sample < sampleCount; sample++) {
            double smoothing = targetIntensity > intensity ? attackAlpha : releaseAlpha;
            intensity += (targetIntensity - intensity) * smoothing;
            double squealSmoothing = targetSquealIntensity > squealIntensity
                    ? squealAttackAlpha : squealReleaseAlpha;
            squealIntensity += (targetSquealIntensity - squealIntensity) * squealSmoothing;

            double noise = bipolarNoise();
            double coarseNoise = bipolarNoise();
            double contactNoise = contactNoiseHighPass.process(
                    contactNoiseLowPass.process(noise));
            double rubberGrain = rubberNoiseHighPass.process(
                    rubberNoiseLowPass.process(coarseNoise));
            double adhesionWander = adhesionNoiseLowPass.process(bipolarNoise());
            double modeExcitation = contactNoise * 0.72 + rubberGrain * 0.28;
            double primaryBody = primaryBeltMode.process(modeExcitation);
            double secondaryBody = secondaryBeltMode.process(modeExcitation);

            double chatterHz = 11.0 + currentSpeedKmh * 0.24
                    + brakingBias * 9.0 + throttleBias * 4.0
                    + adhesionWander * 5.0;
            chatterPhase = wrap(chatterPhase + TWO_PI * chatterHz / sampleRate);
            double treadHz = 34.0 + currentSpeedKmh * 0.72 + throttleBias * 18.0;
            treadPhase = wrap(treadPhase + TWO_PI * treadHz / sampleRate);

            double grabWave = Math.sin(chatterPhase) + adhesionWander * 1.8;
            double stickSlip = 0.44 + 0.56 * Dsp.smoothStep((grabWave + 1.0) * 0.5);
            double treadBlocks = Math.tanh(Math.sin(treadPhase) * 3.2) * rubberGrain;
            double belt = primaryBody * 0.25 + secondaryBody * 0.16;
            double scrub = contactNoise * (0.24 + currentSlip * 0.12);
            double tearing = rubberGrain * (0.22 + brakingBias * 0.07
                    + throttleBias * 0.05);
            double rawFriction = scrub + tearing + belt + treadBlocks * 0.08;
            double value = Math.tanh(rawFriction * 2.35) * 0.46
                    * intensity * stickSlip * cycleCharacter * profile.getOutputGain();

            double squealExcitation = squealExciterHighPass.process(
                    squealExciterLowPass.process(squealBipolarNoise()));
            double lowScream = lowSquealMode.process(squealExcitation);
            double middleScream = middleSquealMode.process(squealExcitation);
            double highScream = highSquealMode.process(squealExcitation);
            double flutterHz = 17.0 + currentSpeedKmh * 0.17
                    + brakingBias * 8.0 + squealBipolarNoise() * 1.4;
            squealFlutterPhase = wrap(squealFlutterPhase
                    + TWO_PI * flutterHz / sampleRate);
            double flutter = 0.66 + 0.34 * Dsp.smoothStep(
                    (Math.sin(squealFlutterPhase) + 1.0) * 0.5);
            double screamBody = lowScream * 1.55 + middleScream * 1.12
                    + highScream * 0.82 + squealExcitation * 0.12;
            double scream = Math.tanh(screamBody * 4.2) * 0.74
                    * squealIntensity * flutter * profile.getOutputGain();
            value += scream;
            value = dcBlocker.process(value) * (telemetry.interior ? 0.38 : 1.0);
            short output = (short) Math.round(Dsp.clamp(value, -0.92, 0.92) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);
        }
        silent = targetIntensity < 0.0002 && intensity < 0.0004;
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        synchronize(telemetry);
        targetIntensity = 0.0;
        intensity = 0.0;
        targetSquealIntensity = 0.0;
        squealIntensity = 0.0;
        silent = true;
    }

    boolean isSilent() {
        return silent;
    }

    double getIntensity() {
        return intensity;
    }

    double getSquealIntensity() {
        return squealIntensity;
    }

    private void processTelemetry(EngineTelemetry telemetry) {
        if (telemetry.timestampNanos == lastTelemetryTimestamp) {
            return;
        }
        double nextTarget = profile.resolveIntensity(telemetry.tireSlip,
                telemetry.speedKmh, telemetry.throttle, telemetry.brakeApplied);
        double nextSquealTarget = profile.resolveSquealIntensity(telemetry.tireSlip,
                telemetry.speedKmh, telemetry.throttle, telemetry.brakeApplied);
        if (nextTarget > 0.001 && targetIntensity <= 0.001) {
            cycleCharacter = 0.965 + randomUnit() * 0.070;
            chatterPhase = randomUnit() * TWO_PI;
        }
        targetIntensity = nextTarget;
        targetSquealIntensity = nextSquealTarget;
        currentSpeedKmh = Math.abs(telemetry.speedKmh);
        currentSlip = telemetry.tireSlip;
        throttleBias = Dsp.smoothStep((telemetry.throttle - 0.38) / 0.58);
        brakingBias = telemetry.brakeApplied ? 1.0 : 0.0;
        lastTelemetryTimestamp = telemetry.timestampNanos;
    }

    private void synchronize(EngineTelemetry telemetry) {
        lastTelemetryTimestamp = telemetry.timestampNanos;
        currentSpeedKmh = Math.abs(telemetry.speedKmh);
        currentSlip = telemetry.tireSlip;
        throttleBias = Dsp.smoothStep((telemetry.throttle - 0.38) / 0.58);
        brakingBias = telemetry.brakeApplied ? 1.0 : 0.0;
        targetIntensity = 0.0;
        intensity = 0.0;
        targetSquealIntensity = 0.0;
        squealIntensity = 0.0;
    }

    private static double wrap(double phase) {
        return phase >= TWO_PI ? phase % TWO_PI : phase;
    }

    private double randomUnit() {
        int value = randomState;
        value ^= value << 13;
        value ^= value >>> 17;
        value ^= value << 5;
        randomState = value;
        return (value & 0x7FFFFFFF) / (double) Integer.MAX_VALUE;
    }

    private double bipolarNoise() {
        return randomUnit() * 2.0 - 1.0;
    }

    private double squealBipolarNoise() {
        int value = squealRandomState;
        value ^= value << 13;
        value ^= value >>> 17;
        value ^= value << 5;
        squealRandomState = value;
        return (value & 0x7FFFFFFF) / (double) Integer.MAX_VALUE * 2.0 - 1.0;
    }
}
