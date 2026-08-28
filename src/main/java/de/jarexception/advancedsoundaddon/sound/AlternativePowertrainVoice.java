package de.jarexception.advancedsoundaddon.sound;

/** Dedicated sample-free synthesis for non-combustion powertrains. */
final class AlternativePowertrainVoice {
    private static final double TWO_PI = Math.PI * 2.0;

    private final EnginePowertrain powertrain;
    private final EngineProfile profile;
    private final int sampleRate;
    private final Dsp.OnePoleLowPass noiseLowPass;
    private final Dsp.OnePoleHighPass noiseHighPass;
    private final Dsp.OnePoleHighPass dcBlocker;
    private final Dsp.ButterworthLowPass electricOutputLowPass;

    private double smoothedRpm;
    private double envelope;
    private double primaryPhase;
    private double secondaryPhase;
    private double carrierPhase;
    private volatile boolean silent;
    private int randomState = 0x45E1C7A3;

    AlternativePowertrainVoice(EngineProfile profile, int sampleRate,
                               EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.powertrain = profile.getPowertrain();
        this.sampleRate = sampleRate;
        noiseLowPass = new Dsp.OnePoleLowPass(sampleRate,
                powertrain == EnginePowertrain.ELECTRIC ? 6_200 : 8_500);
        noiseHighPass = new Dsp.OnePoleHighPass(sampleRate,
                powertrain == EnginePowertrain.ELECTRIC ? 140 : 420);
        dcBlocker = new Dsp.OnePoleHighPass(sampleRate, 28);
        electricOutputLowPass = powertrain == EnginePowertrain.ELECTRIC
                ? new Dsp.ButterworthLowPass(sampleRate, 4_800) : null;
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        byte[] pcm = new byte[sampleCount * 2];
        double maximumRpm = Math.max(500.0, telemetry.maxRpm);
        double targetRpm = telemetry.engineOn ? Math.max(0.0, telemetry.rpm) : 0.0;
        double targetEnvelope = telemetry.engineOn ? 1.0 : 0.0;
        double rpmTime = powertrain == EnginePowertrain.ELECTRIC
                ? (targetRpm > smoothedRpm ? 0.035 : 0.080)
                : (targetRpm > smoothedRpm
                ? Math.max(0.20, profile.getStartDurationSeconds() * 0.75)
                : Math.max(0.30, profile.getStopDurationSeconds() * 0.85));
        double rpmAlpha = 1.0 - Math.exp(-1.0 / sampleRate / rpmTime);
        double envelopeTime = targetEnvelope > envelope
                ? profile.getStartDurationSeconds()
                : profile.getStopDurationSeconds();
        double envelopeAlpha = 1.0 - Math.exp(-1.0 / sampleRate / envelopeTime);

        for (int sample = 0; sample < sampleCount; sample++) {
            smoothedRpm += (targetRpm - smoothedRpm) * rpmAlpha;
            envelope += (targetEnvelope - envelope) * envelopeAlpha;
            double rpmFraction = Dsp.clamp(smoothedRpm / maximumRpm, 0.0, 1.12);
            double value = powertrain == EnginePowertrain.ELECTRIC
                    ? synthesizeElectric(rpmFraction, telemetry)
                    : synthesizeTurboshaft(rpmFraction, telemetry);
            value = dcBlocker.process(value * envelope);
            value *= telemetry.interior ? 0.78 : 1.0;
            short output = (short) Math.round(Dsp.clamp(value, -0.92, 0.92) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);
        }
        silent = !telemetry.engineOn && envelope < 0.0005;
        return pcm;
    }

    private double synthesizeElectric(double rpmFraction, EngineTelemetry telemetry) {
        double roadFraction = Dsp.clamp(Math.abs(telemetry.speedKmh) / 160.0, 0.0, 1.12);
        double demandedRpm = telemetry.throttle > 0.015F || Math.abs(telemetry.speedKmh) > 1.0F
                ? rpmFraction : 0.0;
        double motorFraction = Dsp.clamp(Math.max(roadFraction, demandedRpm * 0.82), 0.0, 1.12);
        double motion = Math.sqrt(motorFraction);
        double motorCharacter = profile.getInductionCharacter();
        double gearBrightness = profile.getMechanicalBrightness();

        double shaftFrequency = 26.0 + 315.0 * Math.pow(motorFraction, 0.76)
                * (0.92 + motorCharacter * 0.08);
        double gearMeshFrequency = (145.0 + 1_820.0 * Math.pow(motorFraction, 0.70))
                * (0.82 + gearBrightness * 0.18);
        primaryPhase = advance(primaryPhase, shaftFrequency);
        secondaryPhase = advance(secondaryPhase, gearMeshFrequency);
        double carrierFrequency = 3_150.0 + 1_850.0 * motorFraction;
        carrierPhase = advance(carrierPhase, carrierFrequency);

        double effort = Dsp.clamp(0.18 + telemetry.throttle * 0.48 + telemetry.load * 0.34,
                0.0, 1.0);
        double shaftBody = (Math.sin(primaryPhase) * 0.38
                + Math.sin(primaryPhase * 2.0 + 0.48) * 0.23
                + Math.sin(primaryPhase * 4.0 + 1.17) * 0.10)
                * motion * (0.026 + effort * 0.038) * motorCharacter;
        double phaseModulation = Math.sin(primaryPhase) * (0.24 + telemetry.load * 0.18);
        double gearWhirr = (Math.sin(secondaryPhase + phaseModulation) * 0.34
                + Math.sin(secondaryPhase * 2.0 + primaryPhase * 0.31) * 0.20
                + Math.sin(secondaryPhase * 3.0 + 0.82) * 0.15)
                * motion * (0.020 + effort * 0.032) * gearBrightness;
        double inverter = (Math.sin(carrierPhase + Math.sin(primaryPhase) * 0.55) * 0.62
                + Math.sin(carrierPhase * 2.0 + secondaryPhase * 0.07) * 0.38)
                * (0.0008 + telemetry.throttle * 0.0045) * (0.10 + motion * 0.90)
                * gearBrightness;
        double motorAir = noiseHighPass.process(noiseLowPass.process(randomUnit() * 2.0 - 1.0));
        double airflow = motorAir * (0.0025 + motorFraction * motorFraction * 0.020);
        return electricOutputLowPass.process(shaftBody + gearWhirr + inverter + airflow);
    }

    private double synthesizeTurboshaft(double rpmFraction, EngineTelemetry telemetry) {
        double shaftFrequency = 82.0 + 365.0 * Math.pow(rpmFraction, 0.76);
        primaryPhase = advance(primaryPhase, shaftFrequency);
        secondaryPhase = advance(secondaryPhase, shaftFrequency * 6.65);
        carrierPhase = advance(carrierPhase, shaftFrequency * 11.2);

        double spool = 0.075 + rpmFraction * 0.105 + telemetry.load * 0.045;
        double whine = Math.sin(primaryPhase) * 0.22
                + Math.sin(primaryPhase * 2.02 + 0.18) * 0.10
                + Math.sin(secondaryPhase) * 0.54
                + Math.sin(carrierPhase + 0.72) * 0.18;
        double turbineNoise = noiseHighPass.process(noiseLowPass.process(randomUnit() * 2.0 - 1.0))
                * (0.055 + rpmFraction * 0.12);
        return whine * spool + turbineNoise;
    }

    void requestResync(EngineTelemetry telemetry) {
        synchronize(telemetry);
    }

    boolean isSilent() {
        return silent;
    }

    private void synchronize(EngineTelemetry telemetry) {
        smoothedRpm = telemetry.engineOn ? Math.max(0.0, telemetry.rpm) : 0.0;
        envelope = telemetry.engineOn ? 1.0 : 0.0;
        silent = !telemetry.engineOn;
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
}
