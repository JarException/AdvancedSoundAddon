package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TireSquealVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void presetsRepresentDistinctTyreConstructions() {
        TireSquealProfile street = TireSquealProfile.forPreset("STREET_TIRE");
        TireSquealProfile performance = TireSquealProfile.forPreset("PERFORMANCE_TIRE");
        TireSquealProfile slick = TireSquealProfile.forPreset("RACE_SLICK");
        TireSquealProfile heavy = TireSquealProfile.forPreset("HEAVY_TIRE");

        assertTrue(heavy.getPrimaryModeHz() < street.getPrimaryModeHz());
        assertTrue(street.getPrimaryModeHz() < performance.getPrimaryModeHz());
        assertTrue(performance.getPrimaryModeHz() < slick.getPrimaryModeHz());
        assertTrue(slick.getActivationSlip() < street.getActivationSlip());
        assertEquals("STREET_TIRE", TireSquealProfile.defaultProfile().getPresetName());
    }

    @Test
    public void tyreFrictionIsBroadbandInsteadOfAWhistlingPureTone() {
        long time = 1_500_000_000L;
        TireSquealVoice voice = new TireSquealVoice(
                TireSquealProfile.forPreset("PERFORMANCE_TIRE"), SAMPLE_RATE,
                telemetry(62, 0.0F, 0.0F, false, false, time));
        byte[] pcm = voice.render(
                telemetry(62, 0.96F, 0.15F, false, false, time + 50_000_000L),
                CHUNK * 8);

        double concentration = strongestSpectralBinShare(pcm, CHUNK * 4, CHUNK * 4);
        assertTrue("rubber friction must not collapse into a flute-like spectral line",
                concentration < 0.12);
    }

    @Test
    public void realWheelSlipExcitesSyntheticContactPatchAndBeltModes() {
        long time = 1_000_000_000L;
        TireSquealVoice voice = new TireSquealVoice(
                TireSquealProfile.forPreset("PERFORMANCE_TIRE"), SAMPLE_RATE,
                telemetry(68, 0.0F, 0.0F, false, false, time));

        byte[] pcm = voice.render(
                telemetry(65, 0.94F, 0.35F, false, false, time + 50_000_000L),
                CHUNK * 8);

        assertTrue("DynamX wheel slip must create an audible procedural tyre layer",
                energy(pcm) > 1_000_000L);
        assertTrue(!voice.isSilent());
    }

    @Test
    public void moderateSlipKeepsTheGoodRubberScrubWithoutEmergencyScream() {
        long time = 1_800_000_000L;
        TireSquealVoice voice = new TireSquealVoice(
                TireSquealProfile.forPreset("PERFORMANCE_TIRE"), SAMPLE_RATE,
                telemetry(58, 0.0F, 0.0F, false, false, time));

        byte[] pcm = voice.render(
                telemetry(58, 0.78F, 0.15F, false, false, time + 50_000_000L),
                CHUNK * 8);

        assertTrue("moderate sliding must retain the broadband scrub layer",
                energy(pcm) > 100_000L);
        assertEquals("the loud scream is reserved for genuinely severe slip",
                0.0, voice.getSquealIntensity(), 0.000001);
    }

    @Test
    public void severeSlidingAddsALouderScreamAboveTheScrubLayer() {
        long time = 1_900_000_000L;
        TireSquealProfile profile = TireSquealProfile.forPreset("PERFORMANCE_TIRE");
        TireSquealVoice moderate = new TireSquealVoice(profile, SAMPLE_RATE,
                telemetry(70, 0.0F, 0.0F, false, false, time));
        TireSquealVoice severe = new TireSquealVoice(profile, SAMPLE_RATE,
                telemetry(70, 0.0F, 0.0F, false, false, time));

        byte[] scrub = moderate.render(telemetry(70, 0.80F, 0.0F,
                false, false, time + 50_000_000L), CHUNK * 10);
        byte[] scream = severe.render(telemetry(70, 0.99F, 0.0F,
                true, false, time + 50_000_000L), CHUNK * 10);

        assertTrue("full lock-up must engage the separate squeal layer",
                severe.getSquealIntensity() > 0.80);
        assertTrue("severe squeal must overtake the underlying scrub",
                energy(scream) > energy(scrub) * 2.0);
    }

    @Test
    public void normalRollingGripRemainsExactlySilent() {
        long time = 2_000_000_000L;
        TireSquealVoice voice = new TireSquealVoice(
                TireSquealProfile.forPreset("STREET_TIRE"), SAMPLE_RATE,
                telemetry(48, 0.0F, 0.2F, false, false, time));

        byte[] pcm = voice.render(
                telemetry(50, 0.18F, 0.2F, false, false, time + 50_000_000L),
                CHUNK * 5);

        assertEquals(0L, energy(pcm));
        assertTrue(voice.isSilent());
    }

    @Test
    public void drivenWheelspinCanSquealBeforeChassisBuildsSpeed() {
        long time = 3_000_000_000L;
        TireSquealVoice voice = new TireSquealVoice(
                TireSquealProfile.forPreset("STREET_TIRE"), SAMPLE_RATE,
                telemetry(0, 0.0F, 0.0F, false, false, time));

        byte[] pcm = voice.render(
                telemetry(0.8F, 0.99F, 1.0F, false, false, time + 50_000_000L),
                CHUNK * 8);

        assertTrue("a real driven-wheel burnout must not require chassis speed",
                energy(pcm) > 700_000L);
    }

    @Test
    public void skidSoundFadesOutAfterGripReturns() {
        long time = 4_000_000_000L;
        TireSquealVoice voice = new TireSquealVoice(
                TireSquealProfile.forPreset("RACE_SLICK"), SAMPLE_RATE,
                telemetry(90, 0.0F, 0.0F, false, false, time));
        voice.render(telemetry(82, 0.97F, 0.0F, true, false,
                time + 50_000_000L), CHUNK * 5);

        EngineTelemetry grip = telemetry(76, 0.05F, 0.0F, false, false,
                time + 100_000_000L);
        for (int chunk = 0; chunk < 45; chunk++) {
            voice.render(grip, CHUNK);
        }

        assertTrue("tyre modes must stop after the contact patch regains grip", voice.isSilent());
    }

    @Test
    public void cockpitAttenuatesExternalTyreContactNoise() {
        long time = 5_000_000_000L;
        TireSquealProfile profile = TireSquealProfile.forPreset("PERFORMANCE_TIRE");
        TireSquealVoice exterior = new TireSquealVoice(profile, SAMPLE_RATE,
                telemetry(55, 0.0F, 0.0F, false, false, time));
        TireSquealVoice interior = new TireSquealVoice(profile, SAMPLE_RATE,
                telemetry(55, 0.0F, 0.0F, false, true, time));

        byte[] outside = exterior.render(telemetry(55, 0.96F, 0.0F,
                false, false, time + 50_000_000L), CHUNK * 6);
        byte[] inside = interior.render(telemetry(55, 0.96F, 0.0F,
                false, true, time + 50_000_000L), CHUNK * 6);

        assertTrue("the cabin must attenuate tyre contact noise",
                energy(inside) < energy(outside) * 0.55);
    }

    private static EngineTelemetry telemetry(float speed, float tireSlip, float throttle,
                                             boolean brakeApplied, boolean interior,
                                             long timestamp) {
        return new EngineTelemetry(2_800, 7_000, throttle, 0.45F, speed, 3,
                true, false, brakeApplied, tireSlip, interior, timestamp);
    }

    private static long energy(byte[] pcm) {
        long energy = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            energy += Math.abs((int) sample);
        }
        return energy;
    }

    private static double strongestSpectralBinShare(byte[] pcm, int startSample,
                                                     int sampleCount) {
        double strongest = 0.0;
        double total = 0.0;
        int firstBin = (int) Math.ceil(250.0 * sampleCount / SAMPLE_RATE);
        int lastBin = (int) Math.floor(8_000.0 * sampleCount / SAMPLE_RATE);
        for (int bin = firstBin; bin <= lastBin; bin++) {
            double real = 0.0;
            double imaginary = 0.0;
            for (int sample = 0; sample < sampleCount; sample++) {
                int pcmOffset = (startSample + sample) * 2;
                short value = (short) ((pcm[pcmOffset] & 0xFF)
                        | (pcm[pcmOffset + 1] << 8));
                double window = 0.5 - 0.5 * Math.cos(TWO_PI * sample
                        / (sampleCount - 1.0));
                double phase = TWO_PI * bin * sample / sampleCount;
                real += value * window * Math.cos(phase);
                imaginary -= value * window * Math.sin(phase);
            }
            double power = real * real + imaginary * imaginary;
            strongest = Math.max(strongest, power);
            total += power;
        }
        return strongest / Math.max(1.0, total);
    }

    private static final double TWO_PI = Math.PI * 2.0;
}
