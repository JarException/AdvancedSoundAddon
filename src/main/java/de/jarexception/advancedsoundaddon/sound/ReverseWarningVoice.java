package de.jarexception.advancedsoundaddon.sound;

/** Synthesizes intermittent tonal and broadband reverse warning devices. */
final class ReverseWarningVoice {
    private static final double TWO_PI = Math.PI * 2.0;

    private final ReverseWarningProfile profile;
    private final int sampleRate;
    private final Dsp.OnePoleHighPass broadbandHighPass;
    private final Dsp.OnePoleLowPass broadbandLowPass;

    private double cycleTime;
    private double tonePhase;
    private double envelope;
    private boolean previousActive;
    private volatile boolean silent;
    private int randomState = 0x4A17C9E3;

    ReverseWarningVoice(ReverseWarningProfile profile, int sampleRate,
                        EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        broadbandHighPass = profile.getSource() == ReverseWarningProfile.Source.BROADBAND
                ? new Dsp.OnePoleHighPass(sampleRate, profile.getBandLowHz()) : null;
        broadbandLowPass = profile.getSource() == ReverseWarningProfile.Source.BROADBAND
                ? new Dsp.OnePoleLowPass(sampleRate, profile.getBandHighHz()) : null;
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        byte[] pcm = new byte[sampleCount * 2];
        boolean active = isActive(telemetry);
        if (active && !previousActive) {
            cycleTime = 0.0;
            tonePhase = 0.0;
            silent = false;
        }

        double cycleSeconds = 60.0 / profile.getPulsesPerMinute();
        double soundingSeconds = cycleSeconds * profile.getDutyCycle();
        double dt = 1.0 / sampleRate;
        double attackAlpha = 1.0 - Math.exp(-dt / 0.006);
        double releaseAlpha = 1.0 - Math.exp(-dt / 0.012);

        for (int sample = 0; sample < sampleCount; sample++) {
            boolean sounding = active && cycleTime < soundingSeconds;
            double targetEnvelope = sounding ? 1.0 : 0.0;
            envelope += (targetEnvelope - envelope)
                    * (targetEnvelope > envelope ? attackAlpha : releaseAlpha);

            double source = profile.getSource() == ReverseWarningProfile.Source.TONAL
                    ? renderTonal() : renderBroadband();
            double value = source * envelope * profile.getOutputGain();
            if (telemetry.interior) {
                value *= 0.34;
            }
            short output = (short) Math.round(Dsp.clamp(value, -0.86, 0.86) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);

            if (active) {
                cycleTime += dt;
                if (cycleTime >= cycleSeconds) {
                    cycleTime -= cycleSeconds;
                    tonePhase = 0.0;
                }
            } else {
                cycleTime = 0.0;
            }
        }

        previousActive = active;
        silent = !active && envelope < 0.0005;
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        synchronize(telemetry);
    }

    boolean isSilent() {
        return silent;
    }

    float getAudibleDistance() {
        return profile.getAudibleDistance();
    }

    private double renderTonal() {
        tonePhase = wrap(tonePhase + TWO_PI * profile.getCenterFrequencyHz() / sampleRate);
        return Math.sin(tonePhase) * 0.78
                + Math.sin(tonePhase * 2.0 + 0.18) * 0.16
                + Math.sin(tonePhase * 3.0 + 0.42) * 0.06;
    }

    private double renderBroadband() {
        tonePhase = wrap(tonePhase + TWO_PI * profile.getCenterFrequencyHz() / sampleRate);
        double noise = randomUnit() * 2.0 - 1.0;
        double bandNoise = broadbandLowPass.process(broadbandHighPass.process(noise));
        return bandNoise * 2.15 + Math.sin(tonePhase) * 0.045;
    }

    private void synchronize(EngineTelemetry telemetry) {
        previousActive = isActive(telemetry);
        cycleTime = 0.0;
        tonePhase = 0.0;
        envelope = 0.0;
        if (broadbandHighPass != null) {
            broadbandHighPass.reset();
            broadbandLowPass.reset();
        }
        silent = !previousActive;
    }

    private static boolean isActive(EngineTelemetry telemetry) {
        return telemetry.engineOn && telemetry.gear < 0;
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
