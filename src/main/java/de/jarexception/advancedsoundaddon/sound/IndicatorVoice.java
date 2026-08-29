package de.jarexception.advancedsoundaddon.sound;

/** Synthesizes the energizing and releasing clicks of a cabin indicator relay. */
final class IndicatorVoice {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double THIRD_PERSON_GAIN = 0.28;
    private static final int STRIKE_FINISHED = Integer.MAX_VALUE;

    private final IndicatorProfile profile;
    private final int sampleRate;
    private final double gainCompensation;
    private final Dsp.OnePoleHighPass noiseHighPass;
    private final Dsp.OnePoleLowPass noiseLowPass;
    private final double engageDecay;
    private final double releaseDecay;

    private long lastTelemetryTimestamp;
    private double cycleTime;
    private double engageEnvelope;
    private double releaseEnvelope;
    private double engagePhase;
    private double releasePhase;
    private double listenerGain;
    private int engageStrikeAge = STRIKE_FINISHED;
    private int releaseStrikeAge = STRIKE_FINISHED;
    private boolean active;
    private boolean previousActive;
    private boolean previousAudible;
    private boolean releaseFired;
    private volatile boolean silent = true;
    private int randomState = 0x6D2B79F5;

    IndicatorVoice(IndicatorProfile profile, float powertrainGain, int sampleRate,
                   EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        gainCompensation = Math.min(1.8, 1.0 / Math.max(0.35, powertrainGain));
        noiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 620);
        noiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 6_800);
        engageDecay = Math.exp(-1.0 / sampleRate / profile.getEngageDecaySeconds());
        releaseDecay = Math.exp(-1.0 / sampleRate / profile.getReleaseDecaySeconds());
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        processTelemetry(telemetry);
        byte[] pcm = new byte[sampleCount * 2];
        double dt = 1.0 / sampleRate;
        for (int sample = 0; sample < sampleCount; sample++) {
            if (active) {
                if (!releaseFired && cycleTime >= profile.getIlluminatedSeconds()) {
                    triggerRelease();
                    releaseFired = true;
                }
                if (cycleTime >= profile.getPeriodSeconds()) {
                    cycleTime -= profile.getPeriodSeconds();
                    releaseFired = false;
                    triggerEngage();
                }
                cycleTime += dt;
            }

            engagePhase = wrap(engagePhase
                    + TWO_PI * profile.getEngageFrequencyHz() / sampleRate);
            releasePhase = wrap(releasePhase
                    + TWO_PI * profile.getReleaseFrequencyHz() / sampleRate);
            double noise = randomUnit() * 2.0 - 1.0;
            double relayNoise = noiseLowPass.process(noiseHighPass.process(noise));
            double engage;
            double release;
            if (profile.isElectronic()) {
                engage = (Math.sin(engagePhase) * 0.62
                        + Math.sin(engagePhase * 1.97 + 0.31) * 0.18
                        + relayNoise * profile.getNoiseMix()) * engageEnvelope;
                release = (Math.sin(releasePhase) * 0.54
                        + Math.sin(releasePhase * 2.11 + 0.67) * 0.15
                        + relayNoise * profile.getNoiseMix() * 0.82) * releaseEnvelope;
            } else {
                double engageBody = (Math.sin(engagePhase) * 0.18
                        + Math.sin(engagePhase * 2.43 + 0.31) * 0.07) * engageEnvelope;
                double releaseBody = (Math.sin(releasePhase) * 0.15
                        + Math.sin(releasePhase * 2.17 + 0.67) * 0.06) * releaseEnvelope;
                double engageContact = relayNoise * profile.getNoiseMix()
                        * engageEnvelope * engageEnvelope;
                double releaseContact = relayNoise * profile.getNoiseMix() * 0.78
                        * releaseEnvelope * releaseEnvelope;
                engage = engageBody + engageContact
                        + mechanicalStrike(engageStrikeAge, false);
                release = releaseBody + releaseContact
                        + mechanicalStrike(releaseStrikeAge, true);
            }
            double value = (engage + release) * profile.getOutputGain()
                    * gainCompensation * listenerGain;
            short output = (short) Math.round(Dsp.clamp(value, -0.82, 0.82) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);

            engageEnvelope *= engageDecay;
            releaseEnvelope *= releaseDecay;
            engageStrikeAge = advanceStrike(engageStrikeAge);
            releaseStrikeAge = advanceStrike(releaseStrikeAge);
        }
        silent = listenerGain <= 0.0
                || (!active && engageEnvelope < 0.0004 && releaseEnvelope < 0.0004);
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        cycleTime = 0.0;
        engageEnvelope = 0.0;
        releaseEnvelope = 0.0;
        engagePhase = 0.0;
        releasePhase = 0.0;
        engageStrikeAge = STRIKE_FINISHED;
        releaseStrikeAge = STRIKE_FINISHED;
        releaseFired = false;
        previousActive = false;
        previousAudible = false;
        synchronize(telemetry);
    }

    boolean isSilent() {
        return silent;
    }

    private void processTelemetry(EngineTelemetry telemetry) {
        if (telemetry.timestampNanos == lastTelemetryTimestamp) return;
        updateState(telemetry);
        lastTelemetryTimestamp = telemetry.timestampNanos;
    }

    private void synchronize(EngineTelemetry telemetry) {
        updateState(telemetry);
        lastTelemetryTimestamp = telemetry.timestampNanos;
    }

    private void updateState(EngineTelemetry telemetry) {
        active = telemetry.indicatorLeftActive || telemetry.indicatorRightActive;
        boolean audible = telemetry.vehicleOccupant && telemetry.cabinSoundGain > 0.0F;
        listenerGain = audible ? telemetry.cabinSoundGain
                * (telemetry.interior ? 1.0 : THIRD_PERSON_GAIN) : 0.0;

        if (active && (!previousActive || !previousAudible && audible)) {
            cycleTime = 0.0;
            releaseFired = false;
            triggerEngage();
        } else if (!active && previousActive && !releaseFired) {
            triggerRelease();
        }
        if (!active) {
            cycleTime = 0.0;
            releaseFired = false;
        }
        previousActive = active;
        previousAudible = audible;
        silent = !audible || (!active
                && engageEnvelope < 0.0004 && releaseEnvelope < 0.0004);
    }

    private void triggerEngage() {
        engageEnvelope = 1.0;
        engagePhase = randomUnit() * 0.28;
        engageStrikeAge = profile.isElectronic() ? STRIKE_FINISHED : 0;
        silent = false;
    }

    private void triggerRelease() {
        releaseEnvelope = 1.0;
        releasePhase = randomUnit() * 0.34;
        releaseStrikeAge = profile.isElectronic() ? STRIKE_FINISHED : 0;
        silent = false;
    }

    private double mechanicalStrike(int age, boolean release) {
        if (age == STRIKE_FINISHED) return 0.0;
        int chatterDelay = Math.max(4, (int) Math.round(sampleRate * 0.00135));
        double primary = bipolarImpulse(age);
        double chatter = bipolarImpulse(age - chatterDelay) * 0.24;
        double polarity = release ? -0.82 : 1.0;
        return (primary + chatter) * polarity;
    }

    private static double bipolarImpulse(int age) {
        switch (age) {
            case 0: return 0.92;
            case 1: return -0.68;
            case 2: return 0.31;
            case 3: return -0.11;
            default: return 0.0;
        }
    }

    private int advanceStrike(int age) {
        if (age == STRIKE_FINISHED) return age;
        int finishedAfter = Math.max(4, (int) Math.round(sampleRate * 0.00135)) + 4;
        return age >= finishedAfter ? STRIKE_FINISHED : age + 1;
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
}
