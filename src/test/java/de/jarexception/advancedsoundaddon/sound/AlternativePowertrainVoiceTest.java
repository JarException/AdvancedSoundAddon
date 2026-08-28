package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AlternativePowertrainVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void electricPresetHasNoCombustionPatternAndRisesWithMotorSpeed() {
        EngineProfile profile = EngineProfile.forPreset("ELECTRIC");
        assertEquals(EnginePowertrain.ELECTRIC, profile.getPowertrain());
        assertTrue(profile.getFiringPattern() == null);

        EngineTelemetry low = telemetry(900, true, 0.25F, 0.30F, 18);
        EngineVoice voice = new EngineVoice(profile, SAMPLE_RATE, low);
        voice.render(low, CHUNK * 4);
        byte[] lowPcm = voice.render(low, CHUNK * 4);

        EngineTelemetry high = telemetry(5_500, true, 0.80F, 0.82F, 115);
        voice.render(high, CHUNK * 8);
        byte[] highPcm = voice.render(high, CHUNK * 4);

        assertTrue(energy(lowPcm) > 300_000L);
        assertTrue(energy(highPcm) > energy(lowPcm));
        assertTrue(lowPassZeroCrossings(highPcm) > lowPassZeroCrossings(lowPcm) * 3 / 2);
        assertTrue("electric drive must not collapse into one piercing pure tone",
                dominantToneRatio(highPcm) < 1.05);
    }

    @Test
    public void electricPresetDoesNotBeepAtCombustionStyleIdleWhileParked() {
        EngineProfile profile = EngineProfile.forPreset("ELECTRIC");
        EngineTelemetry parked = telemetry(1_200, true, 0, 0, 0);
        EngineVoice voice = new EngineVoice(profile, SAMPLE_RATE, parked);
        byte[] parkedPcm = voice.render(parked, CHUNK * 4);

        EngineTelemetry moving = telemetry(3_500, true, 0.55F, 0.62F, 72);
        voice.render(moving, CHUNK * 6);
        byte[] movingPcm = voice.render(moving, CHUNK * 4);

        assertTrue("parked EV electronics should remain much quieter than its driven motor",
                energy(parkedPcm) * 4 < energy(movingPcm));
    }

    @Test
    public void turboshaftPresetProducesSmoothSyntheticSpoolAndStops() {
        EngineProfile profile = EngineProfile.forPreset("TURBOSHAFT");
        EngineTelemetry running = telemetry(4_800, true, 0.75F, 0.80F, 40);
        EngineVoice voice = new EngineVoice(profile, SAMPLE_RATE, running);
        byte[] pcm = voice.render(running, CHUNK * 6);
        assertTrue(energy(pcm) > 1_000_000L);

        EngineTelemetry stopped = telemetry(0, false, 0, 0, 0);
        for (int index = 0; index < 420; index++) {
            voice.render(stopped, CHUNK);
        }
        assertTrue(voice.isSilent());
    }

    private static EngineTelemetry telemetry(float rpm, boolean on, float throttle, float load,
                                             float speedKmh) {
        return new EngineTelemetry(rpm, 7_000, throttle, load, speedKmh, 3,
                on, false, false, System.nanoTime());
    }

    private static long energy(byte[] pcm) {
        long energy = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            energy += Math.abs((int) sample);
        }
        return energy;
    }

    private static int lowPassZeroCrossings(byte[] pcm) {
        double alpha = 1.0 - Math.exp(-2.0 * Math.PI * 1_900.0 / SAMPLE_RATE);
        double filtered = 0.0;
        double previous = 0.0;
        int crossings = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            filtered += alpha * (sample - filtered);
            if ((filtered < 0 && previous >= 0) || (filtered >= 0 && previous < 0)) {
                crossings++;
            }
            previous = filtered;
        }
        return crossings;
    }

    private static double dominantToneRatio(byte[] pcm) {
        int samples = pcm.length / 2;
        double squareSum = 0.0;
        double peakAmplitude = 0.0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            squareSum += sample * (double) sample;
        }
        for (int frequency = 80; frequency <= 5_000; frequency += 35) {
            double real = 0.0;
            double imaginary = 0.0;
            for (int index = 0; index < samples; index++) {
                int offset = index * 2;
                short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
                double phase = 2.0 * Math.PI * frequency * index / SAMPLE_RATE;
                real += sample * Math.cos(phase);
                imaginary -= sample * Math.sin(phase);
            }
            peakAmplitude = Math.max(peakAmplitude,
                    Math.sqrt(real * real + imaginary * imaginary) * 2.0 / samples);
        }
        return peakAmplitude / Math.sqrt(squareSum / samples);
    }
}
