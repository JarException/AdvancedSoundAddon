package de.jarexception.advancedsoundaddon.sound;

/** Synthesizes speed- and temperature-dependent brake friction. */
final class BrakeSquealVoice {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double AMBIENT_TEMPERATURE_C = 24.0;

    private final BrakeSquealProfile profile;
    private final int sampleRate;
    private final Dsp.OnePoleLowPass frictionLowPass;
    private final Dsp.OnePoleHighPass frictionHighPass;
    private final Dsp.OnePoleLowPass stickSlipNoise;
    private final Dsp.Resonator primaryResonator;
    private final Dsp.Resonator secondaryResonator;
    private final Dsp.OnePoleHighPass dcBlocker;

    private long lastTelemetryTimestamp;
    private float previousSpeedKmh;
    private boolean previousBrakeApplied;
    private double estimatedTemperatureC = 31.0;
    private double targetIntensity;
    private double intensity;
    private double currentSpeedKmh;
    private double currentBrakeDemand;
    private double primaryPhase;
    private double secondaryPhase;
    private double stickSlipPhase;
    private double cycleCharacter = 1.0;
    private volatile boolean silent = true;
    private int randomState = 0x71C43A2D;

    BrakeSquealVoice(BrakeSquealProfile profile, int sampleRate,
                     EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        frictionLowPass = new Dsp.OnePoleLowPass(sampleRate,
                Math.min(sampleRate * 0.42, profile.getSecondaryModeHz() * 1.55));
        frictionHighPass = new Dsp.OnePoleHighPass(sampleRate,
                Math.max(180.0, profile.getPrimaryModeHz() * 0.34));
        stickSlipNoise = new Dsp.OnePoleLowPass(sampleRate, 24);
        primaryResonator = new Dsp.Resonator(sampleRate, profile.getPrimaryModeHz(),
                Math.max(90.0, profile.getPrimaryModeHz() * 0.075));
        secondaryResonator = new Dsp.Resonator(sampleRate, profile.getSecondaryModeHz(),
                Math.max(120.0, profile.getSecondaryModeHz() * 0.085));
        dcBlocker = new Dsp.OnePoleHighPass(sampleRate, 95);
        synchronize(initialTelemetry, true);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        processTelemetry(telemetry);
        byte[] pcm = new byte[sampleCount * 2];
        double attackAlpha = 1.0 - Math.exp(-1.0 / sampleRate / 0.045);
        double releaseAlpha = 1.0 - Math.exp(-1.0 / sampleRate / 0.105);
        double temperaturePitch = 1.0 + Dsp.clamp(
                (estimatedTemperatureC - 55.0) * 0.00010, -0.004, 0.022);
        double speedPitch = 0.985 + Dsp.clamp(currentSpeedKmh
                / Math.max(1.0, profile.getMaximumSpeedKmh()), 0.0, 1.0) * 0.030;

        for (int sample = 0; sample < sampleCount; sample++) {
            double smoothing = targetIntensity > intensity ? attackAlpha : releaseAlpha;
            intensity += (targetIntensity - intensity) * smoothing;

            double noise = randomUnit() * 2.0 - 1.0;
            double friction = frictionHighPass.process(frictionLowPass.process(noise));
            double slowNoise = stickSlipNoise.process(noise);
            double primaryBody = primaryResonator.process(friction);
            double secondaryBody = secondaryResonator.process(friction);

            double pitch = temperaturePitch * speedPitch;
            primaryPhase = wrap(primaryPhase + TWO_PI * profile.getPrimaryModeHz()
                    * pitch / sampleRate);
            secondaryPhase = wrap(secondaryPhase + TWO_PI * profile.getSecondaryModeHz()
                    * (pitch * 0.998 + slowNoise * 0.0007) / sampleRate);
            stickSlipPhase = wrap(stickSlipPhase + TWO_PI
                    * (5.5 + currentSpeedKmh * 0.16 + currentBrakeDemand * 4.0) / sampleRate);

            double stickSlip = 0.62 + 0.38
                    * Dsp.smoothStep((Math.sin(stickSlipPhase) + 1.0) * 0.5);
            double unstableTone = Math.sin(primaryPhase) * (0.052 + slowNoise * 0.010)
                    + Math.sin(secondaryPhase + 0.31) * (0.026 - slowNoise * 0.005);
            double resonantFriction = primaryBody * 0.36 + secondaryBody * 0.24;
            double surfaceRasp = friction * (0.020 + currentBrakeDemand * 0.014);
            double value = (unstableTone + resonantFriction + surfaceRasp)
                    * intensity * stickSlip * cycleCharacter * profile.getOutputGain();
            value = dcBlocker.process(value) * (telemetry.interior ? 0.34 : 1.0);
            short output = (short) Math.round(Dsp.clamp(value, -0.72, 0.72) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);
        }
        silent = targetIntensity < 0.0002 && intensity < 0.0004;
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        synchronize(telemetry, false);
        targetIntensity = 0.0;
        intensity = 0.0;
        silent = true;
    }

    boolean isSilent() {
        return silent;
    }

    double getEstimatedTemperatureC() {
        return estimatedTemperatureC;
    }

    private void processTelemetry(EngineTelemetry telemetry) {
        if (telemetry.timestampNanos == lastTelemetryTimestamp) {
            return;
        }
        double elapsed = lastTelemetryTimestamp == 0L ? 0.0
                : Dsp.clamp((telemetry.timestampNanos - lastTelemetryTimestamp)
                / 1_000_000_000.0, 0.0, 0.25);
        double speed = Math.abs(telemetry.speedKmh);
        double oldSpeed = Math.abs(previousSpeedKmh);
        double deceleration = elapsed <= 0.0001 ? 0.0
                : Math.max(0.0, oldSpeed - speed) / elapsed;
        double brakeDemand = telemetry.brakeApplied
                ? Dsp.clamp(0.12 + deceleration / 22.0, 0.12, 1.0) : 0.0;

        if (telemetry.brakeApplied && !previousBrakeApplied) {
            cycleCharacter = 0.82 + randomUnit() * 0.28;
            stickSlipPhase = randomUnit() * TWO_PI;
        }

        if (brakeDemand > 0.0 && speed > profile.getMinimumSpeedKmh()) {
            double speedEnergy = 0.38 + Dsp.clamp(speed / 75.0, 0.0, 1.35);
            estimatedTemperatureC += elapsed * profile.getHeatRate()
                    * brakeDemand * speedEnergy;
            estimatedTemperatureC = Math.min(650.0, estimatedTemperatureC);
            targetIntensity = profile.resolveIntensity(speed, brakeDemand,
                    estimatedTemperatureC);
        } else {
            targetIntensity = 0.0;
        }

        double coolingSeconds = speed > 8.0 ? 52.0 : 105.0;
        double cooling = 1.0 - Math.exp(-elapsed / coolingSeconds);
        estimatedTemperatureC += (AMBIENT_TEMPERATURE_C - estimatedTemperatureC) * cooling;
        currentSpeedKmh = speed;
        currentBrakeDemand = brakeDemand;
        previousSpeedKmh = telemetry.speedKmh;
        previousBrakeApplied = telemetry.brakeApplied;
        lastTelemetryTimestamp = telemetry.timestampNanos;
    }

    private void synchronize(EngineTelemetry telemetry, boolean initializeTemperature) {
        lastTelemetryTimestamp = telemetry.timestampNanos;
        previousSpeedKmh = telemetry.speedKmh;
        previousBrakeApplied = telemetry.brakeApplied;
        currentSpeedKmh = Math.abs(telemetry.speedKmh);
        currentBrakeDemand = 0.0;
        targetIntensity = 0.0;
        intensity = 0.0;
        if (initializeTemperature) {
            estimatedTemperatureC = 31.0;
        }
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
}
