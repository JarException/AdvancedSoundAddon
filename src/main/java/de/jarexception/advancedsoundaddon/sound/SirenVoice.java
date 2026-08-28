package de.jarexception.advancedsoundaddon.sound;

/** Continuous sample-free emergency siren with country-specific timing. */
final class SirenVoice {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double DEFAULT_OUTPUT_SCALE = 0.15;
    private static final double INTERIOR_OUTPUT_SCALE = 0.48;
    private static final double DE_FIRE_EXTERIOR_OUTPUT_SCALE = 0.52;

    private final SirenProfile profile;
    private final int sampleRate;
    private final float[] primary;
    private final float[] secondary;
    private final float[] durations;
    private final float[] harmonics;
    private final PhysicalHornUnit[] primaryAirHorns;
    private final PhysicalHornUnit[] secondaryAirHorns;
    private final double airHornGain;
    private final double gainCompensation;
    private final Dsp.OnePoleLowPass speakerNoiseLowPass;
    private final Dsp.OnePoleHighPass speakerNoiseHighPass;
    private final Dsp.DelayLine hornMouthReflection;
    private final Dsp.DelayLine vehicleReflection;
    private final Dsp.OnePoleLowPass reflectionLowPass;
    private final Dsp.OnePoleHighPass airPresenceHighPass;
    private final Dsp.OnePoleHighPass airBrillianceHighPass;
    private final Dsp.OnePoleHighPass speakerPresenceHighPass;
    private final double[] stepPrimaryPhases;
    private final double[] stepSecondaryPhases;
    private final double[] stepSubharmonicPhases;
    private final double[] stepEnvelopes;

    private long lastTelemetryTimestamp;
    private double targetEnvelope;
    private double envelope;
    private double patternTime;
    private double primaryPhase;
    private double secondaryPhase;
    private double subharmonicPhase;
    private double flutterPhase;
    private double compressorPhase;
    private double secondaryCompressorPhase;
    private double speakerToneEnvelope;
    private boolean previousActive;
    private boolean interior;
    private volatile boolean silent = true;
    private int randomState = 0x79B52C31;

    SirenVoice(SirenProfile profile, float powertrainGain, int sampleRate,
               EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        primary = profile.getPrimaryFrequenciesHz();
        secondary = profile.getSecondaryFrequenciesHz();
        durations = profile.getDurationsSeconds();
        harmonics = profile.getHarmonics();
        gainCompensation = Math.min(1.8, 1.0 / Math.max(0.35, powertrainGain));
        speakerNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 6_800);
        speakerNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 260);
        hornMouthReflection = new Dsp.DelayLine((int) (sampleRate * 0.0022));
        vehicleReflection = new Dsp.DelayLine((int) (sampleRate * 0.0068));
        reflectionLowPass = new Dsp.OnePoleLowPass(sampleRate, 3_400);
        airPresenceHighPass = new Dsp.OnePoleHighPass(sampleRate, 680);
        airBrillianceHighPass = new Dsp.OnePoleHighPass(sampleRate, 1_250);
        speakerPresenceHighPass = new Dsp.OnePoleHighPass(sampleRate, 900);
        stepPrimaryPhases = new double[primary.length];
        stepSecondaryPhases = new double[primary.length];
        stepSubharmonicPhases = new double[primary.length];
        stepEnvelopes = new double[primary.length];
        if (profile.getSource() == SirenProfile.Source.AIR_HORN) {
            primaryAirHorns = createAirHorns(primary, 0x79B52C31);
            secondaryAirHorns = createAirHorns(secondary, 0x1B873593);
            airHornGain = secondary.length == 0 ? 1.0 : 1.72;
        } else {
            primaryAirHorns = new PhysicalHornUnit[0];
            secondaryAirHorns = new PhysicalHornUnit[0];
            airHornGain = 1.0;
        }
        synchronize(initialTelemetry);
    }

    byte[] render(EngineTelemetry telemetry, int sampleCount) {
        processTelemetry(telemetry);
        byte[] pcm = new byte[sampleCount * 2];
        double attackAlpha = 1.0 - Math.exp(-1.0 / sampleRate / 0.028);
        double releaseAlpha = 1.0 - Math.exp(-1.0 / sampleRate / 0.16);
        double dt = 1.0 / sampleRate;
        for (int sample = 0; sample < sampleCount; sample++) {
            double smoothing = targetEnvelope > envelope ? attackAlpha : releaseAlpha;
            envelope += (targetEnvelope - envelope) * smoothing;
            if (envelope < 0.00001 && targetEnvelope == 0.0) continue;

            double pressure;
            if (profile.getSource() == SirenProfile.Source.AIR_HORN) {
                pressure = renderAirHorns(envelope, dt);
            } else {
                pressure = renderPoweredSpeaker(dt);
            }
            double drive = profile.getSource() == SirenProfile.Source.AIR_HORN
                    ? 1.36 + profile.getRasp() * 0.48
                    : 1.15 + profile.getRasp() * 1.35;
            double sourceOutput = profile.getSource() == SirenProfile.Source.AIR_HORN
                    ? pressure : Math.tanh(pressure * drive);
            double value = sourceOutput * DEFAULT_OUTPUT_SCALE * envelope
                    * profile.getOutputGain() * gainCompensation;
            if (profile.getSource() == SirenProfile.Source.AIR_HORN) {
                value /= Math.max(0.08, envelope);
            }
            value *= perspectiveOutputScale();
            short output = (short) Math.round(Dsp.clamp(value, -0.88, 0.88) * 32767.0);
            pcm[sample * 2] = (byte) (output & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((output >>> 8) & 0xFF);
            patternTime += dt;
        }
        silent = targetEnvelope == 0.0 && envelope < 0.0004;
        return pcm;
    }

    void requestResync(EngineTelemetry telemetry) {
        synchronize(telemetry);
        envelope = 0.0;
        silent = !telemetry.sirenActive;
    }

    boolean isSilent() {
        return silent;
    }

    float getAudibleDistance() {
        return profile.getAudibleDistance();
    }

    private void processTelemetry(EngineTelemetry telemetry) {
        if (telemetry.timestampNanos == lastTelemetryTimestamp) return;
        if (telemetry.sirenActive && !previousActive) patternTime = 0.0;
        targetEnvelope = telemetry.sirenActive ? 1.0 : 0.0;
        previousActive = telemetry.sirenActive;
        interior = telemetry.interior;
        lastTelemetryTimestamp = telemetry.timestampNanos;
    }

    private void synchronize(EngineTelemetry telemetry) {
        lastTelemetryTimestamp = telemetry.timestampNanos;
        targetEnvelope = telemetry.sirenActive ? 1.0 : 0.0;
        previousActive = telemetry.sirenActive;
        interior = telemetry.interior;
        patternTime = 0.0;
    }

    private Frequencies frequenciesAt(double seconds) {
        if (profile.getPattern() == SirenProfile.Pattern.STEP) {
            double cycle = 0.0;
            for (int i = 0; i < primary.length; i++) cycle += durationAt(i);
            double cursor = seconds % Math.max(0.03, cycle);
            int index = 0;
            while (index < primary.length - 1 && cursor >= durationAt(index)) {
                cursor -= durationAt(index++);
            }
            return new Frequencies(primary[index], secondary.length == 0 ? 0 : secondary[index]);
        }

        double cycle = durations[0];
        double phase = (seconds % cycle) / cycle;
        double blend;
        switch (profile.getPattern()) {
            case TRIANGLE:
                blend = phase < 0.5 ? phase * 2.0 : 2.0 - phase * 2.0;
                break;
            case SINE:
                blend = 0.5 - 0.5 * Math.cos(TWO_PI * phase);
                break;
            case SAW_DOWN:
                blend = 1.0 - phase;
                break;
            case SAW_UP:
            default:
                blend = phase;
                break;
        }
        return new Frequencies(primary[0] + (primary[1] - primary[0]) * blend, 0);
    }

    private int stepIndexAt(double seconds) {
        double cycle = 0.0;
        for (int index = 0; index < primary.length; index++) cycle += durationAt(index);
        double cursor = seconds % Math.max(0.03, cycle);
        int index = 0;
        while (index < primary.length - 1 && cursor >= durationAt(index)) {
            cursor -= durationAt(index++);
        }
        return index;
    }

    private double renderAirHorns(double envelope, double dt) {
        int activeIndex = stepIndexAt(patternTime);
        compressorPhase = wrap(compressorPhase + TWO_PI * 3.15 * dt);
        secondaryCompressorPhase = wrap(secondaryCompressorPhase + TWO_PI * 1.17 * dt);
        double sharedSupply = Math.sin(compressorPhase) * 0.68
                + Math.sin(secondaryCompressorPhase + 0.8) * 0.32;
        double tone = 0.0;
        for (int index = 0; index < primaryAirHorns.length; index++) {
            PhysicalHornUnit horn = primaryAirHorns[index];
            if (horn != null) {
                tone += horn.render(index == activeIndex ? envelope : 0.0, sharedSupply);
            }
        }
        for (int index = 0; index < secondaryAirHorns.length; index++) {
            PhysicalHornUnit horn = secondaryAirHorns[index];
            if (horn != null) {
                tone += horn.render(index == activeIndex ? envelope : 0.0, sharedSupply) * 0.82;
            }
        }
        tone /= airHornGain;
        double reflections = hornMouthReflection.process(tone) * 0.16
                + vehicleReflection.process(tone) * 0.075;
        double radiated = tone + reflectionLowPass.process(reflections);
        double presence = airPresenceHighPass.process(radiated);
        double brilliance = airBrillianceHighPass.process(presence);
        return radiated * 0.14 + presence * 1.28 + brilliance * 0.76;
    }

    private double renderPoweredSpeaker(double dt) {
        flutterPhase = wrap(flutterPhase + TWO_PI * profile.getFlutterHz() * dt);
        double flutter = 1.0 + Math.sin(flutterPhase) * profile.getFlutterDepth()
                + bipolarNoise() * profile.getRasp() * 0.0007;
        double tone = profile.getPattern() == SirenProfile.Pattern.STEP
                ? renderSteppedSpeaker(dt, flutter)
                : renderSweptSpeaker(dt, flutter);

        double speakerNoise = speakerNoiseHighPass.process(
                speakerNoiseLowPass.process(bipolarNoise()));
        double coneBreakup = Math.tanh(tone * (1.08 + profile.getRasp() * 0.75));
        double speaker = tone * 0.72 + coneBreakup * 0.29
                + speakerNoise * profile.getRasp() * 0.15 * speakerToneEnvelope;
        return speaker * 0.78 + speakerPresenceHighPass.process(speaker) * 0.52;
    }

    private double renderSteppedSpeaker(double dt, double flutter) {
        int activeIndex = stepIndexAt(patternTime);
        double attackAlpha = 1.0 - Math.exp(-dt / 0.006);
        double releaseAlpha = 1.0 - Math.exp(-dt / 0.014);
        double tone = 0.0;
        double totalEnvelope = 0.0;
        for (int index = 0; index < primary.length; index++) {
            boolean hasTone = primary[index] > 0.0
                    || (secondary.length > index && secondary[index] > 0.0);
            double target = index == activeIndex && hasTone ? 1.0 : 0.0;
            double alpha = target > stepEnvelopes[index] ? attackAlpha : releaseAlpha;
            stepEnvelopes[index] += (target - stepEnvelopes[index]) * alpha;
            totalEnvelope += stepEnvelopes[index];

            if (primary[index] > 0.0) {
                stepPrimaryPhases[index] = wrap(stepPrimaryPhases[index]
                        + TWO_PI * primary[index] * flutter * dt);
                tone += oscillator(stepPrimaryPhases[index], primary[index])
                        * stepEnvelopes[index];
                if (profile.getSubharmonicGain() > 0.0F) {
                    stepSubharmonicPhases[index] = wrap(stepSubharmonicPhases[index]
                            + TWO_PI * primary[index] * 0.5 * flutter * dt);
                    tone += Math.sin(stepSubharmonicPhases[index])
                            * profile.getSubharmonicGain() * stepEnvelopes[index];
                }
            }
            if (secondary.length > index && secondary[index] > 0.0) {
                stepSecondaryPhases[index] = wrap(stepSecondaryPhases[index]
                        + TWO_PI * secondary[index] * flutter * dt);
                tone += oscillator(stepSecondaryPhases[index], secondary[index]) * 0.72
                        * stepEnvelopes[index];
            }
        }
        speakerToneEnvelope = Dsp.clamp(totalEnvelope, 0.0, 1.0);
        return tone;
    }

    private double renderSweptSpeaker(double dt, double flutter) {
        Frequencies current = frequenciesAt(patternTime);
        double toneTarget = current.primary > 0.0 || current.secondary > 0.0 ? 1.0 : 0.0;
        double toneEnvelopeAlpha = 1.0 - Math.exp(-dt
                / (toneTarget > speakerToneEnvelope ? 0.006 : 0.014));
        speakerToneEnvelope += (toneTarget - speakerToneEnvelope) * toneEnvelopeAlpha;
        double tone = 0.0;
        if (current.primary > 0.0) {
            primaryPhase = wrap(primaryPhase + TWO_PI * current.primary * flutter * dt);
            tone += oscillator(primaryPhase, current.primary);
            if (profile.getSubharmonicGain() > 0.0F) {
                subharmonicPhase = wrap(subharmonicPhase
                        + TWO_PI * current.primary * 0.5 * flutter * dt);
                tone += Math.sin(subharmonicPhase) * profile.getSubharmonicGain();
            }
        }
        if (current.secondary > 0.0) {
            secondaryPhase = wrap(secondaryPhase + TWO_PI * current.secondary * flutter * dt);
            tone += oscillator(secondaryPhase, current.secondary) * 0.72;
        }
        return tone * speakerToneEnvelope;
    }

    private PhysicalHornUnit[] createAirHorns(float[] frequencies, int seed) {
        PhysicalHornUnit[] result = new PhysicalHornUnit[frequencies.length];
        for (int index = 0; index < frequencies.length; index++) {
            if (frequencies[index] > 0.0F) {
                result[index] = new PhysicalHornUnit(sampleRate, frequencies[index],
                        0.68 + profile.getRasp() * 0.16, profile.getRasp(),
                        PhysicalHornUnit.Type.AIR_TRUMPET, seed + index * 0x1020305,
                        0.92);
            }
        }
        return result;
    }

    private double durationAt(int index) {
        return durations.length == 1 ? durations[0] : durations[index];
    }

    private double oscillator(double phase, double fundamentalFrequency) {
        double value = 0.0;
        for (int harmonic = 0; harmonic < harmonics.length; harmonic++) {
            double frequency = fundamentalFrequency * (harmonic + 1);
            double distance = (frequency - 1_550.0) / 720.0;
            double pressureChamberResponse = profile.getSource()
                    == SirenProfile.Source.ELECTRONIC_SPEAKER
                    ? 0.36 + 1.10 * Math.exp(-distance * distance) : 1.0;
            value += Math.sin(phase * (harmonic + 1) + harmonic * 0.07)
                    * harmonics[harmonic] * pressureChamberResponse;
        }
        return value;
    }

    private double bipolarNoise() {
        int value = randomState;
        value ^= value << 13;
        value ^= value >>> 17;
        value ^= value << 5;
        randomState = value;
        return (value & 0x7FFFFFFF) / (double) Integer.MAX_VALUE * 2.0 - 1.0;
    }

    private double perspectiveOutputScale() {
        if (interior) {
            return INTERIOR_OUTPUT_SCALE;
        }
        return "DE_FIRE".equals(profile.getPresetName())
                ? DE_FIRE_EXTERIOR_OUTPUT_SCALE : 1.0;
    }

    private static double wrap(double phase) {
        return phase >= TWO_PI ? phase % TWO_PI : phase;
    }

    private static final class Frequencies {
        private final double primary;
        private final double secondary;

        private Frequencies(double primary, double secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
    }
}
