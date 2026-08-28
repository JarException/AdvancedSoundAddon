package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TurbochargerVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void gtRTurboSpoolsAudiblyWithRpmAndLoad() {
        TurbochargerProfile profile = turboFor("V6_TWIN_TURBO");
        EngineTelemetry idle = telemetry(900, 0.04F, 0.08F, true);
        TurbochargerVoice voice = new TurbochargerVoice(profile, SAMPLE_RATE, idle);

        byte[] idlePcm = voice.render(idle, CHUNK * 4);
        EngineTelemetry accelerating = telemetry(5_200, 0.95F, 0.96F, true);
        for (int chunk = 0; chunk < 28; chunk++) {
            voice.render(accelerating, CHUNK);
        }
        byte[] boostPcm = voice.render(accelerating, CHUNK * 4);

        assertTrue("boost must be clearly audible over idle compressor noise",
                energy(boostPcm) > Math.max(120_000L, energy(idlePcm) * 8));
        assertTrue("compressor output must retain headroom", peak(boostPcm) < 26_000);
        assertTrue("compressor must be a broad mechanical air sound, not an electric pure tone",
                dominantToneRatio(boostPcm) < 0.72);
    }

    @Test
    public void throttleLiftCreatesAReleaseTransientThatDecays() {
        TurbochargerProfile profile = turboFor("V6_TWIN_TURBO");
        EngineTelemetry boost = telemetry(5_400, 1.0F, 1.0F, true);
        TurbochargerVoice voice = new TurbochargerVoice(profile, SAMPLE_RATE, boost);
        voice.render(boost, CHUNK * 4);

        EngineTelemetry lift = telemetry(5_250, 0.0F, 0.06F, true);
        byte[] release = voice.render(lift, CHUNK * 2);
        byte[] decayed = release;
        for (int chunk = 0; chunk < 48; chunk++) {
            decayed = voice.render(lift, CHUNK * 2);
        }

        assertTrue("pressure release must create an audible transient",
                energy(release) > 80_000L);
        assertTrue("release and remaining spool must decay cleanly",
                energy(release) > energy(decayed) * 4);
    }

    @Test
    public void gtRRetainsAnAudibleDescendingCompressorSweepAfterLift() {
        TurbochargerProfile profile = turboFor("V6_TWIN_TURBO");
        EngineTelemetry boost = telemetry(5_700, 1.0F, 1.0F, true);
        TurbochargerVoice voice = new TurbochargerVoice(profile, SAMPLE_RATE, boost);
        voice.render(boost, CHUNK * 4);

        EngineTelemetry lift = telemetry(5_500, 0.0F, 0.06F, true);
        byte[] earlyRelease = voice.render(lift, CHUNK * 4);
        for (int chunk = 0; chunk < 20; chunk++) {
            voice.render(lift, CHUNK);
        }
        byte[] lateRelease = voice.render(lift, CHUNK * 4);

        double earlyFrequency = strongestFrequency(earlyRelease, 2_500, 9_500);
        double lateFrequency = strongestFrequency(lateRelease, 1_500, 8_500);
        assertTrue("release compressor sweep must remain audible",
                energy(lateRelease) > 100_000L);
        assertTrue("release compressor frequency must sweep downward: early="
                        + earlyFrequency + ", late=" + lateFrequency,
                earlyFrequency > lateFrequency * 1.18);
    }

    @Test
    public void stoppedTurboEventuallyReportsSilence() {
        TurbochargerProfile profile = turboFor("V6_TWIN_TURBO");
        EngineTelemetry boost = telemetry(5_000, 0.9F, 0.95F, true);
        TurbochargerVoice voice = new TurbochargerVoice(profile, SAMPLE_RATE, boost);
        EngineTelemetry stopped = telemetry(0, 0, 0, false);

        for (int chunk = 0; chunk < 360; chunk++) {
            voice.render(stopped, CHUNK);
        }
        assertTrue(voice.isSilent());
    }

    private static TurbochargerProfile turboFor(String enginePreset) {
        return TurbochargerProfile.forEngineProfile(EngineProfile.forPreset(enginePreset));
    }

    private static EngineTelemetry telemetry(float rpm, float throttle, float load,
                                             boolean engineOn) {
        return new EngineTelemetry(rpm, 7_000, throttle, load, 60, 3,
                engineOn, false, false, System.nanoTime());
    }

    private static long energy(byte[] pcm) {
        long energy = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            energy += Math.abs((int) sample(pcm, offset));
        }
        return energy;
    }

    private static int peak(byte[] pcm) {
        int peak = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            peak = Math.max(peak, Math.abs((int) sample(pcm, offset)));
        }
        return peak;
    }

    private static short sample(byte[] pcm, int offset) {
        return (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
    }

    private static double dominantToneRatio(byte[] pcm) {
        int samples = pcm.length / 2;
        double squareSum = 0.0;
        double peakAmplitude = 0.0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short value = sample(pcm, offset);
            squareSum += value * (double) value;
        }
        for (int frequency = 500; frequency <= 10_000; frequency += 45) {
            double real = 0.0;
            double imaginary = 0.0;
            for (int index = 0; index < samples; index++) {
                short value = sample(pcm, index * 2);
                double phase = 2.0 * Math.PI * frequency * index / SAMPLE_RATE;
                real += value * Math.cos(phase);
                imaginary -= value * Math.sin(phase);
            }
            peakAmplitude = Math.max(peakAmplitude,
                    Math.sqrt(real * real + imaginary * imaginary) * 2.0 / samples);
        }
        return peakAmplitude / Math.sqrt(squareSum / samples);
    }

    private static double strongestFrequency(byte[] pcm, int minimumHz, int maximumHz) {
        int samples = pcm.length / 2;
        double strongestHz = minimumHz;
        double strongestPower = 0.0;
        for (int frequency = minimumHz; frequency <= maximumHz; frequency += 35) {
            double real = 0.0;
            double imaginary = 0.0;
            for (int index = 0; index < samples; index++) {
                double window = 0.5 - 0.5 * Math.cos(2.0 * Math.PI * index
                        / Math.max(1, samples - 1));
                short value = sample(pcm, index * 2);
                double phase = 2.0 * Math.PI * frequency * index / SAMPLE_RATE;
                real += value * window * Math.cos(phase);
                imaginary -= value * window * Math.sin(phase);
            }
            double power = real * real + imaginary * imaginary;
            if (power > strongestPower) {
                strongestPower = power;
                strongestHz = frequency;
            }
        }
        return strongestHz;
    }
}
