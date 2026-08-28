package de.jarexception.advancedsoundaddon.sound;

/** Synthesizes pressure releases for compressed-air brake systems. */
final class AirBrakeVoice {
    private final AirBrakeProfile profile;
    private final int sampleRate;
    private final Dsp.OnePoleLowPass jetLowPass;
    private final Dsp.OnePoleHighPass jetHighPass;
    private final Dsp.OnePoleLowPass bodyNoiseLowPass;
    private final Dsp.Resonator valveBody;
    private final Dsp.OnePoleHighPass dcBlocker;

    private long lastTelemetryTimestamp;
    private long lastReleaseTimestamp;
    private long lastPurgeTimestamp;
    private boolean previousBrakeApplied;
    private float previousSpeed;
    private double recentMotionSpeed;
    private double reservoirPressure;
    private double stationarySeconds;
    private boolean departureArmed;
    private double serviceAge = Double.POSITIVE_INFINITY;
    private double releaseAge = Double.POSITIVE_INFINITY;
    private double purgeAge = Double.POSITIVE_INFINITY;
    private double serviceStrength;
    private double releaseStrength;
    private double purgeStrength;
    private double flutterPhase;
    private volatile boolean silent = true;
    private int randomState = 0x31E7A4D9;

    AirBrakeVoice(AirBrakeProfile profile, int sampleRate, EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        jetLowPass = new Dsp.OnePoleLowPass(sampleRate, profile.getNozzleCutoffHz());
        jetHighPass = new Dsp.OnePoleHighPass(sampleRate, 180);
        bodyNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 1_850);
        valveBody = new Dsp.Resonator(sampleRate, profile.getBodyResonanceHz(),
                profile.getBodyResonanceHz() * 0.88);
        dcBlocker = new Dsp.OnePoleHighPass(sampleRate, 55);
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        processTelemetry(telemetry);
        byte[] pcm = new byte[sampleCount * 2];
        final double dt = 1.0 / sampleRate;
        for (int sample = 0; sample < sampleCount; sample++) {
            double service = eventEnvelope(serviceAge, 0.006, 0.31) * serviceStrength;
            double release = eventEnvelope(releaseAge, 0.0035, 0.16) * releaseStrength;
            double purge = purgeEnvelope(purgeAge) * purgeStrength;

            double noise = randomUnit() * 2.0 - 1.0;
            double jet = jetHighPass.process(jetLowPass.process(noise));
            double bodyNoise = bodyNoiseLowPass.process(noise);
            double body = valveBody.process(bodyNoise);

            flutterPhase = wrap(flutterPhase + Math.PI * 2.0
                    * (21.0 + reservoirPressure * 8.0) / sampleRate);
            double purgeFlutter = 0.88 + Math.sin(flutterPhase) * 0.12
                    * Dsp.clamp(purgeAge / 0.55, 0.0, 1.0);
            double jetEnvelope = service * 0.66 + release * 0.92 + purge * 0.82 * purgeFlutter;
            double bodyEnvelope = service * 0.22 + release * 0.13 + purge * 0.25;
            double initialCrack = Math.exp(-releaseAge / 0.012) * releaseStrength * 0.08
                    + Math.exp(-purgeAge / 0.020) * purgeStrength * 0.06;
            double pressureColor = 0.72 + reservoirPressure * 0.28;
            double value = (jet * jetEnvelope * 0.72 * pressureColor
                    + body * bodyEnvelope + bodyNoise * initialCrack)
                    * profile.getOutputGain();
            value = dcBlocker.process(value) * (telemetry.interior ? 0.48 : 1.0);
            short output = (short) Math.round(Dsp.clamp(value, -0.88, 0.88) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);

            serviceAge += dt;
            releaseAge += dt;
            purgeAge += dt;
        }
        silent = activeEnvelope() < 0.0004;
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        synchronize(telemetry);
        serviceAge = Double.POSITIVE_INFINITY;
        releaseAge = Double.POSITIVE_INFINITY;
        purgeAge = Double.POSITIVE_INFINITY;
        silent = true;
    }

    boolean isSilent() {
        return silent;
    }

    private void processTelemetry(EngineTelemetry telemetry) {
        if (telemetry.timestampNanos == lastTelemetryTimestamp) {
            return;
        }
        double elapsed = lastTelemetryTimestamp == 0L ? 0.0
                : Dsp.clamp((telemetry.timestampNanos - lastTelemetryTimestamp) / 1_000_000_000.0,
                0.0, 0.25);
        float speed = Math.abs(telemetry.speedKmh);
        float oldSpeed = Math.abs(previousSpeed);

        if (telemetry.engineOn) {
            double rpmFraction = Dsp.clamp(telemetry.rpm / Math.max(1.0, telemetry.maxRpm), 0.0, 1.0);
            reservoirPressure = Math.min(1.0, reservoirPressure
                    + elapsed * (0.72 + rpmFraction * 0.28) / profile.getChargeSeconds());
        } else {
            reservoirPressure = Math.max(0.28, reservoirPressure - elapsed * 0.0015);
        }

        if (speed > 1.2F) {
            recentMotionSpeed = Math.max(speed, recentMotionSpeed * Math.exp(-elapsed / 2.8));
            stationarySeconds = 0.0;
        } else {
            stationarySeconds += elapsed;
        }
        boolean enteredStandstill = oldSpeed > 0.65F && speed <= 0.65F;
        if (enteredStandstill) {
            departureArmed = true;
        }

        boolean stoppedNow = recentMotionSpeed > 1.4 && enteredStandstill;
        if (stoppedNow) {
            double speedStrength = Dsp.clamp((recentMotionSpeed - 2.0) / 24.0, 0.34, 1.0);
            triggerService(speedStrength * (0.72 + reservoirPressure * 0.28));
            reservoirPressure = Math.max(0.30, reservoirPressure - 0.055 * speedStrength);
            recentMotionSpeed = 0.0;
            lastReleaseTimestamp = telemetry.timestampNanos;
            lastPurgeTimestamp = telemetry.timestampNanos;
        }

        boolean brakeReleased = previousBrakeApplied && !telemetry.brakeApplied;
        boolean stationaryBrakeReleased = brakeReleased && !stoppedNow
                && speed <= 0.65F && stationarySeconds >= 0.15;
        boolean startedMoving = departureArmed && speed > 0.80F;
        if ((stationaryBrakeReleased || startedMoving) && eventCooldownElapsed(lastReleaseTimestamp,
                telemetry.timestampNanos, 0.38)) {
            double releaseScale = brakeReleased ? 1.0 : 0.78;
            triggerRelease(releaseScale * (0.68 + reservoirPressure * 0.32));
            reservoirPressure = Math.max(0.30, reservoirPressure - 0.075 * releaseScale);
            lastReleaseTimestamp = telemetry.timestampNanos;
            lastPurgeTimestamp = telemetry.timestampNanos;
            departureArmed = false;
        }

        boolean purgeReady = telemetry.engineOn && speed <= 0.45F && stationarySeconds >= 3.0
                && reservoirPressure >= 0.92
                && eventCooldownElapsed(lastPurgeTimestamp, telemetry.timestampNanos,
                profile.getAutomaticPurgeSeconds());
        if (purgeReady) {
            triggerPurge(0.78 + reservoirPressure * 0.22);
            reservoirPressure = Math.max(0.30, reservoirPressure - 0.19);
            lastPurgeTimestamp = telemetry.timestampNanos;
        }

        previousBrakeApplied = telemetry.brakeApplied;
        previousSpeed = telemetry.speedKmh;
        lastTelemetryTimestamp = telemetry.timestampNanos;
    }

    private void triggerService(double strength) {
        serviceStrength = Math.max(serviceStrength * eventEnvelope(serviceAge, 0.006, 0.31), strength);
        serviceAge = 0.0;
        silent = false;
    }

    private void triggerRelease(double strength) {
        releaseStrength = Math.max(releaseStrength * eventEnvelope(releaseAge, 0.0035, 0.16), strength);
        releaseAge = 0.0;
        silent = false;
    }

    private void triggerPurge(double strength) {
        purgeStrength = Math.max(purgeStrength * purgeEnvelope(purgeAge), strength);
        purgeAge = 0.0;
        silent = false;
    }

    private void synchronize(EngineTelemetry telemetry) {
        lastTelemetryTimestamp = telemetry.timestampNanos;
        lastReleaseTimestamp = telemetry.timestampNanos;
        lastPurgeTimestamp = telemetry.timestampNanos;
        previousBrakeApplied = telemetry.brakeApplied;
        previousSpeed = telemetry.speedKmh;
        recentMotionSpeed = Math.abs(telemetry.speedKmh);
        reservoirPressure = telemetry.engineOn ? 0.66 : 0.54;
        stationarySeconds = 0.0;
        departureArmed = Math.abs(telemetry.speedKmh) <= 0.65F;
    }

    private double activeEnvelope() {
        return eventEnvelope(serviceAge, 0.006, 0.31) * serviceStrength
                + eventEnvelope(releaseAge, 0.0035, 0.16) * releaseStrength
                + purgeEnvelope(purgeAge) * purgeStrength;
    }

    private static double eventEnvelope(double age, double attackSeconds, double decaySeconds) {
        if (!Double.isFinite(age) || age > decaySeconds * 12.0) {
            return 0.0;
        }
        return (1.0 - Math.exp(-age / attackSeconds)) * Math.exp(-age / decaySeconds);
    }

    private static double purgeEnvelope(double age) {
        if (!Double.isFinite(age) || age > 6.0) {
            return 0.0;
        }
        double attack = 1.0 - Math.exp(-age / 0.010);
        double blowDown = Math.exp(-age / 0.56);
        double valveClose = 1.0 - Dsp.smoothStep((age - 0.58) / 0.50);
        return attack * blowDown * valveClose;
    }

    private static boolean eventCooldownElapsed(long previous, long current, double seconds) {
        return previous == 0L || current - previous >= (long) (seconds * 1_000_000_000L);
    }

    private static double wrap(double phase) {
        return phase >= Math.PI * 2.0 ? phase % (Math.PI * 2.0) : phase;
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
