package de.jarexception.advancedsoundaddon.sound;

/** Synthesizes main- and tail-rotor blade passages and turbulence. */
final class RotorVoice {
    private static final double TWO_PI = Math.PI * 2.0;

    private final RotorProfile profile;
    private final int sampleRate;
    private final Dsp.OnePoleLowPass slapNoiseLowPass;
    private final Dsp.OnePoleHighPass slapNoiseHighPass;
    private final Dsp.OnePoleLowPass washNoiseLowPass;
    private final Dsp.OnePoleHighPass dcBlocker;

    private double spool;
    private double bladePhase;
    private double thumpPhase;
    private double tailPhase;
    private double thumpEnvelope;
    private double slapEnvelope;
    private double washEnvelope;
    private volatile boolean silent;
    private int randomState = 0x62A9D4B7;

    RotorVoice(RotorProfile profile, int sampleRate, EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        slapNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 1_650);
        slapNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 95);
        washNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 520);
        dcBlocker = new Dsp.OnePoleHighPass(sampleRate, 18);
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        byte[] pcm = new byte[sampleCount * 2];
        double targetSpool = telemetry.engineOn
                ? Dsp.clamp(telemetry.rpm / Math.max(1.0, telemetry.maxRpm), 0.0, 1.05)
                : 0.0;
        double spoolTime = targetSpool > spool ? 0.85 : 1.65;
        double spoolAlpha = 1.0 - Math.exp(-1.0 / sampleRate / spoolTime);
        double thumpDecay = Math.exp(-1.0 / sampleRate / 0.082);
        double slapDecay = Math.exp(-1.0 / sampleRate / 0.036);
        double washDecay = Math.exp(-1.0 / sampleRate / 0.145);

        for (int sample = 0; sample < sampleCount; sample++) {
            spool += (targetSpool - spool) * spoolAlpha;
            double audibleSpool = Dsp.smoothStep((spool - 0.055) / 0.38);
            double rotorRpm = profile.getNominalRotorRpm() * (0.16 + 0.84 * spool);
            double rotorHz = rotorRpm / 60.0;
            double bladePassHz = rotorHz * profile.getBladeCount();

            bladePhase += TWO_PI * bladePassHz / sampleRate;
            if (bladePhase >= TWO_PI) {
                bladePhase -= TWO_PI;
                double loading = (0.54 + telemetry.load * 0.34 + telemetry.throttle * 0.12)
                        * audibleSpool;
                thumpEnvelope = Math.min(1.7, thumpEnvelope + loading);
                slapEnvelope = Math.min(1.5, slapEnvelope + loading * 0.82);
                washEnvelope = Math.min(1.4, washEnvelope + loading * 0.58);
                thumpPhase = 0.0;
            }

            thumpPhase = wrap(thumpPhase + TWO_PI * (43.0 + profile.getBladeCount() * 2.8) / sampleRate);
            tailPhase = wrap(tailPhase + TWO_PI * rotorHz * profile.getTailRotorRatio()
                    * (2.0 + 0.08 * telemetry.load) / sampleRate);

            double noise = randomUnit() * 2.0 - 1.0;
            double slapNoise = slapNoiseHighPass.process(slapNoiseLowPass.process(noise));
            double washNoise = washNoiseLowPass.process(noise);
            double body = (Math.sin(thumpPhase) * 0.78
                    + Math.sin(thumpPhase * 2.0 + 0.42) * 0.22) * thumpEnvelope * 0.17;
            double slap = slapNoise * slapEnvelope * 0.21;
            double wash = washNoise * washEnvelope * 0.11;
            double periodicAir = (Math.cos(bladePhase) * 0.68
                    + Math.cos(bladePhase * 2.0 + 0.55) * 0.20)
                    * audibleSpool * 0.026;
            double tailRotor = (Math.sin(tailPhase) * 0.72
                    + Math.sin(tailPhase * 2.0 + 0.37) * 0.28)
                    * audibleSpool * (0.012 + telemetry.load * 0.010);

            double value = dcBlocker.process((body + slap + wash + periodicAir + tailRotor)
                    * profile.getOutputGain());
            value *= telemetry.interior ? 0.62 : 1.0;
            short output = (short) Math.round(Dsp.clamp(value, -0.82, 0.82) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);

            thumpEnvelope *= thumpDecay;
            slapEnvelope *= slapDecay;
            washEnvelope *= washDecay;
        }
        silent = !telemetry.engineOn && spool < 0.001
                && thumpEnvelope < 0.0005 && slapEnvelope < 0.0005;
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        synchronize(telemetry);
    }

    boolean isSilent() {
        return silent;
    }

    private void synchronize(EngineTelemetry telemetry) {
        spool = telemetry.engineOn
                ? Dsp.clamp(telemetry.rpm / Math.max(1.0, telemetry.maxRpm), 0.0, 1.05)
                : 0.0;
        thumpEnvelope = 0.0;
        slapEnvelope = 0.0;
        washEnvelope = 0.0;
        silent = !telemetry.engineOn;
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
