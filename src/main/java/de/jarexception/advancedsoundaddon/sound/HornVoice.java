package de.jarexception.advancedsoundaddon.sound;

/** Sample-free diaphragm, motorcycle, air and marine horn synthesis. */
final class HornVoice {
    private static final double TWO_PI = Math.PI * 2.0;

    private final HornProfile profile;
    private final int sampleRate;
    private final float[] relativeGains;
    private final PhysicalHornUnit[] horns;
    private final double totalHornGain;
    private final double gainCompensation;
    private final Dsp.DelayLine bellReflection;
    private final Dsp.DelayLine bodyReflection;
    private final Dsp.OnePoleLowPass reflectionLowPass;

    private long lastTelemetryTimestamp;
    private boolean previousHornActive;
    private boolean eventActive;
    private double eventTime;
    private double releaseStartTime = Double.POSITIVE_INFINITY;
    private double supplyPhase;
    private double secondarySupplyPhase;
    private volatile boolean silent = true;
    private int randomState = 0x4A39B70D;

    HornVoice(HornProfile profile, float powertrainGain, int sampleRate,
              EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        float[] frequencies = profile.getFrequenciesHz();
        relativeGains = profile.getRelativeGains();
        horns = new PhysicalHornUnit[frequencies.length];
        PhysicalHornUnit.Type type = physicalType(profile.getSource());
        double gainSum = 0.0;
        for (int index = 0; index < frequencies.length; index++) {
            horns[index] = new PhysicalHornUnit(sampleRate, frequencies[index],
                    profile.getBrightness(), profile.getRasp(), type,
                    0x4A39B70D + index * 0x1020305);
            gainSum += relativeGains[index];
        }
        totalHornGain = Math.max(0.01, gainSum);
        gainCompensation = Math.min(1.8, 1.0 / Math.max(0.35, powertrainGain));
        bellReflection = new Dsp.DelayLine((int) (sampleRate * 0.0017));
        bodyReflection = new Dsp.DelayLine((int) (sampleRate * 0.0031));
        reflectionLowPass = new Dsp.OnePoleLowPass(sampleRate, 3_800);
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        processTelemetry(telemetry);
        byte[] pcm = new byte[sampleCount * 2];
        if (!eventActive) {
            silent = true;
            return pcm;
        }

        double dt = 1.0 / sampleRate;
        for (int sample = 0; sample < sampleCount; sample++) {
            double envelope = envelopeAt(eventTime);
            supplyPhase = wrap(supplyPhase + TWO_PI * 2.7 * dt);
            secondarySupplyPhase = wrap(secondarySupplyPhase + TWO_PI * 1.16 * dt);
            double sharedSupply = Math.sin(supplyPhase) * 0.72
                    + Math.sin(secondarySupplyPhase + 1.1) * 0.28;
            double tone = 0.0;
            double maximumPressure = 0.0;
            for (int index = 0; index < horns.length; index++) {
                tone += horns[index].render(envelope, sharedSupply) * relativeGains[index];
                maximumPressure = Math.max(maximumPressure, horns[index].getPressure());
            }
            tone /= totalHornGain;
            double reflected = bellReflection.process(tone) * 0.13
                    + bodyReflection.process(tone) * 0.07;
            double radiated = tone + reflectionLowPass.process(reflected);
            double value = Math.tanh(radiated * (1.22 + profile.getRasp() * 0.62))
                    * 0.72 * profile.getOutputGain() * gainCompensation;
            value *= telemetry.interior ? 0.46 : 1.0;
            short output = (short) Math.round(Dsp.clamp(value, -0.84, 0.84) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);
            eventTime += dt;
            if (releaseStartTime != Double.POSITIVE_INFINITY
                    && eventTime >= releaseStartTime + profile.getReleaseSeconds()
                    && maximumPressure < 0.0003) {
                eventActive = false;
                break;
            }
        }
        silent = !eventActive;
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        eventActive = false;
        eventTime = 0.0;
        releaseStartTime = Double.POSITIVE_INFINITY;
        previousHornActive = false;
        lastTelemetryTimestamp = telemetry.timestampNanos;
        if (telemetry.hornActive) trigger();
        previousHornActive = telemetry.hornActive;
        silent = !eventActive;
    }

    boolean isSilent() {
        return silent;
    }

    float getAudibleDistance() {
        return profile.getAudibleDistance();
    }

    private void processTelemetry(EngineTelemetry telemetry) {
        if (telemetry.timestampNanos == lastTelemetryTimestamp) return;
        if (telemetry.hornActive && !previousHornActive) trigger();
        if (!telemetry.hornActive && previousHornActive && eventActive) {
            releaseStartTime = Math.max(eventTime,
                    profile.getAttackSeconds() + profile.getHoldSeconds());
        }
        previousHornActive = telemetry.hornActive;
        lastTelemetryTimestamp = telemetry.timestampNanos;
    }

    private void synchronize(EngineTelemetry telemetry) {
        lastTelemetryTimestamp = telemetry.timestampNanos;
        previousHornActive = false;
        if (telemetry.hornActive) trigger();
        previousHornActive = telemetry.hornActive;
    }

    private void trigger() {
        eventActive = true;
        eventTime = 0.0;
        releaseStartTime = Double.POSITIVE_INFINITY;
        supplyPhase = randomUnit() * TWO_PI;
        secondarySupplyPhase = randomUnit() * TWO_PI;
        silent = false;
    }

    private double envelopeAt(double seconds) {
        double attack = profile.getAttackSeconds();
        if (seconds < attack) return Dsp.smoothStep(seconds / attack);
        if (seconds < releaseStartTime) return 1.0;
        double releaseEnd = releaseStartTime + profile.getReleaseSeconds();
        if (seconds < releaseEnd) return 1.0 - Dsp.smoothStep((seconds - releaseStartTime)
                / profile.getReleaseSeconds());
        return 0.0;
    }

    private double randomUnit() {
        int value = randomState;
        value ^= value << 13;
        value ^= value >>> 17;
        value ^= value << 5;
        randomState = value;
        return (value & 0x7FFFFFFF) / (double) Integer.MAX_VALUE;
    }

    private static double wrap(double phase) {
        return phase >= TWO_PI ? phase % TWO_PI : phase;
    }

    private static PhysicalHornUnit.Type physicalType(HornProfile.Source source) {
        switch (source) {
            case ELECTRIC_DISC: return PhysicalHornUnit.Type.ELECTRIC_DISC;
            case AIR_TRUMPET: return PhysicalHornUnit.Type.AIR_TRUMPET;
            case MARINE_TRUMPET: return PhysicalHornUnit.Type.MARINE_TRUMPET;
            case ELECTRIC_TRUMPET:
            default: return PhysicalHornUnit.Type.ELECTRIC_TRUMPET;
        }
    }
}
