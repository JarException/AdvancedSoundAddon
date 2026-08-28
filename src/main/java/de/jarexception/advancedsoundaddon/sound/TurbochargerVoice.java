package de.jarexception.advancedsoundaddon.sound;

/** Sample-free compressor, intake-flow and throttle-release synthesis. */
final class TurbochargerVoice {
    private static final double TWO_PI = Math.PI * 2.0;

    private final TurbochargerProfile profile;
    private final int sampleRate;
    private final Dsp.OnePoleLowPass airflowLowPass;
    private final Dsp.OnePoleHighPass airflowHighPass;
    private final Dsp.OnePoleLowPass compressorHissLowPass;
    private final Dsp.OnePoleHighPass compressorHissHighPass;
    private final Dsp.OnePoleLowPass whistleWanderLowPass;
    private final Dsp.OnePoleLowPass releaseLowPass;
    private final Dsp.OnePoleHighPass releaseHighPass;
    private final Dsp.OnePoleHighPass dcBlocker;

    private double spool;
    private double primaryPhase;
    private double secondaryPhase;
    private double turbulencePhase;
    private double releasePhase;
    private double releaseEnvelope;
    private double releaseOriginSpool;
    private int releaseElapsedSamples;
    private double previousThrottle;
    private volatile boolean silent;
    private int randomState = 0x7A4B39D1;

    TurbochargerVoice(TurbochargerProfile profile, int sampleRate,
                      EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        airflowLowPass = new Dsp.OnePoleLowPass(sampleRate, 8_600);
        airflowHighPass = new Dsp.OnePoleHighPass(sampleRate, 900);
        compressorHissLowPass = new Dsp.OnePoleLowPass(sampleRate, 7_200);
        compressorHissHighPass = new Dsp.OnePoleHighPass(sampleRate, 1_400);
        whistleWanderLowPass = new Dsp.OnePoleLowPass(sampleRate, 18);
        releaseLowPass = new Dsp.OnePoleLowPass(sampleRate, 7_200);
        releaseHighPass = new Dsp.OnePoleHighPass(sampleRate, 320);
        dcBlocker = new Dsp.OnePoleHighPass(sampleRate, 90);
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        double targetSpool = targetSpool(telemetry);
        double throttleDrop = previousThrottle - telemetry.throttle;
        if (telemetry.engineOn && throttleDrop > 0.22 && spool > 0.18
                && profile.getReleaseGain() > 0.0F) {
            double releaseEnergy = spool * Dsp.clamp((throttleDrop - 0.12) / 0.70, 0.0, 1.0);
            if (releaseEnergy > releaseEnvelope) {
                releaseEnvelope = releaseEnergy;
                releaseOriginSpool = spool;
                releasePhase = 0.0;
                releaseElapsedSamples = 0;
            }
        }
        previousThrottle = telemetry.throttle;

        byte[] pcm = new byte[sampleCount * 2];
        double riseAlpha = 1.0 - Math.exp(-1.0 / sampleRate / profile.getSpoolRiseSeconds());
        double fallAlpha = 1.0 - Math.exp(-1.0 / sampleRate / profile.getSpoolFallSeconds());
        double releaseDecay = Math.exp(-1.0 / sampleRate
                / Math.max(0.04, profile.getReleaseDurationSeconds()));
        double demand = Dsp.clamp(telemetry.throttle * 0.48 + telemetry.load * 0.66,
                0.0, 1.0);
        double interiorGain = telemetry.interior ? profile.getInteriorGain() : 1.0;
        double filterSpool = Dsp.clamp(Math.max(spool, targetSpool), 0.0, 1.0);
        airflowLowPass.setCutoff(2_800.0 + filterSpool * 4_500.0);
        airflowHighPass.setCutoff(260.0 + filterSpool * 520.0);
        compressorHissLowPass.setCutoff(4_600.0 + filterSpool * 3_000.0);
        compressorHissHighPass.setCutoff(1_150.0 + filterSpool * 650.0);

        for (int sample = 0; sample < sampleCount; sample++) {
            spool += (targetSpool - spool) * (targetSpool > spool ? riseAlpha : fallAlpha);
            double audibleSpool = Dsp.smoothStep((spool - 0.025) / 0.72);
            double frequencyProgress = Math.pow(Dsp.clamp(spool, 0.0, 1.08), 0.62);
            double whistleWander = whistleWanderLowPass.process(randomSigned());
            double whistleFrequency = (profile.getMinimumWhistleHz()
                    + (profile.getMaximumWhistleHz() - profile.getMinimumWhistleHz())
                    * frequencyProgress) * (1.0 + whistleWander * 0.32);
            primaryPhase = advance(primaryPhase, whistleFrequency);
            secondaryPhase = advance(secondaryPhase,
                    whistleFrequency * (profile.getCompressorCount() > 1 ? 1.006 : 2.0));
            turbulencePhase = advance(turbulencePhase, 19.0 + audibleSpool * 24.0);

            double roughPhase = primaryPhase + Math.sin(turbulencePhase) * 0.34
                    + whistleWander * 2.4;
            double multiCompressor = profile.getCompressorCount() > 1
                    ? Math.sin(secondaryPhase + 0.46) * 0.16 : 0.0;
            double releaseWhistle = Math.sqrt(releaseEnvelope) * 1.18;
            if (releaseWhistle > 0.0) {
                double surgePulse = 0.82 + 0.18 * (0.5 + 0.5 * Math.sin(releasePhase));
                releaseWhistle *= 1.0 - profile.getReleaseFlutterDepth()
                        + profile.getReleaseFlutterDepth() * surgePulse;
            }
            double whistle = (Math.sin(roughPhase) * 0.68
                    + Math.sin(roughPhase * 2.0 + 0.31) * 0.05
                    + multiCompressor)
                    * profile.getWhistleGain() * 0.44
                    * Math.pow(audibleSpool, 1.18)
                    * (0.22 + demand * 0.68 + releaseWhistle);

            double airflowNoise = randomSigned();
            double compressorAir = airflowHighPass.process(airflowLowPass.process(airflowNoise));
            double turbulence = 0.84 + Math.sin(turbulencePhase) * 0.10
                    + whistleWander * 0.10;
            double airflow = compressorAir * profile.getAirflowGain()
                    * 1.85 * audibleSpool * (0.18 + demand * 0.82) * turbulence;
            double hissNoise = compressorHissHighPass.process(
                    compressorHissLowPass.process(randomSigned()));
            double compressorTexture = hissNoise * profile.getWhistleGain() * 0.82
                    * Math.pow(audibleSpool, 1.10) * (0.24 + demand * 0.76);

            releaseEnvelope *= releaseDecay;
            double release = 0.0;
            if (releaseEnvelope > 0.0002) {
                double releaseProgress = Dsp.clamp(releaseEnvelope
                        / Math.max(0.001, releaseOriginSpool), 0.0, 1.0);
                if ((sample & 15) == 0) {
                    releaseLowPass.setCutoff(2_400.0 + releaseProgress * 5_400.0);
                    releaseHighPass.setCutoff(240.0 + releaseProgress * 620.0);
                }
                double releaseNoise = releaseHighPass.process(
                        releaseLowPass.process(randomSigned()));
                double flutterRate = 10.0 + releaseOriginSpool * 14.0;
                releasePhase = advance(releasePhase, flutterRate);
                double flutter = 1.0 - profile.getReleaseFlutterDepth()
                        + profile.getReleaseFlutterDepth()
                        * (0.38 + 0.62 * Math.max(0.0, Math.sin(releasePhase)));
                double attack = Dsp.smoothStep(releaseElapsedSamples
                        / Math.max(1.0, sampleRate * 0.006));
                release = releaseNoise * profile.getReleaseGain() * 2.25
                        * releaseEnvelope * flutter * attack;
                releaseElapsedSamples++;
            }

            double value = dcBlocker.process(
                    (whistle + airflow + compressorTexture + release) * interiorGain);
            short output = (short) Math.round(Dsp.clamp(value, -0.78, 0.78) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);
        }
        silent = spool < 0.001 && releaseEnvelope < 0.0002;
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        synchronize(telemetry);
    }

    boolean isSilent() {
        return silent;
    }

    private void synchronize(EngineTelemetry telemetry) {
        spool = targetSpool(telemetry);
        previousThrottle = telemetry.throttle;
        releaseEnvelope = 0.0;
        releaseOriginSpool = 0.0;
        releaseElapsedSamples = 0;
        silent = spool < 0.001;
        airflowLowPass.reset();
        airflowHighPass.reset();
        compressorHissLowPass.reset();
        compressorHissHighPass.reset();
        whistleWanderLowPass.reset();
        releaseLowPass.reset();
        releaseHighPass.reset();
        dcBlocker.reset();
    }

    private double targetSpool(EngineTelemetry telemetry) {
        if (!telemetry.engineOn) {
            return 0.0;
        }
        double rpmFraction = Dsp.clamp(telemetry.rpm / Math.max(500.0, telemetry.maxRpm),
                0.0, 1.10);
        double rpmRange = Math.max(0.05,
                profile.getFullSpoolFraction() - profile.getSpoolStartFraction());
        double rpmDrive = Dsp.smoothStep((rpmFraction - profile.getSpoolStartFraction()) / rpmRange);
        double demand = Dsp.clamp(telemetry.throttle * 0.48 + telemetry.load * 0.66 - 0.06,
                0.0, 1.0);
        return rpmDrive * demand;
    }

    private double advance(double phase, double frequency) {
        phase += TWO_PI * frequency / sampleRate;
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

    private double randomSigned() {
        return randomUnit() * 2.0 - 1.0;
    }
}
