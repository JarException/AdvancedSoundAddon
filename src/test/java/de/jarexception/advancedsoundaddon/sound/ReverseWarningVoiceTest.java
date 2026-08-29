package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReverseWarningVoiceTest {
    private static final int SAMPLE_RATE = 48_000;

    @Test
    public void presetsRepresentTonalAndBroadbandHardware() {
        ReverseWarningProfile tonal = ReverseWarningProfile.forPreset("TONAL_BEEPER");
        ReverseWarningProfile broadband = ReverseWarningProfile.forPreset("BROADBAND");

        assertEquals(ReverseWarningProfile.Source.TONAL, tonal.getSource());
        assertEquals(1_250.0F, tonal.getCenterFrequencyHz(), 0.01F);
        assertEquals(60.0F, tonal.getPulsesPerMinute(), 0.01F);
        assertEquals(ReverseWarningProfile.Source.BROADBAND, broadband.getSource());
        assertTrue(broadband.getBandLowHz() < broadband.getBandHighHz());
    }

    @Test
    public void warningSoundsOnlyWithPropulsionOnAndReverseSelected() {
        ReverseWarningVoice voice = new ReverseWarningVoice(
                ReverseWarningProfile.forPreset("TONAL_BEEPER"), SAMPLE_RATE,
                telemetry(1, true));

        assertEquals(0L, energy(voice.render(telemetry(1, true), 4_096)));
        assertEquals(0L, energy(voice.render(telemetry(-1, false), 4_096)));

        byte[] reversing = voice.render(telemetry(-1, true), 4_096);
        assertTrue(energy(reversing) > 1_000_000L);
        assertFalse(voice.isSilent());
    }

    @Test
    public void tonalWarningContainsAnAudibleGapEachCycle() {
        ReverseWarningVoice voice = new ReverseWarningVoice(
                ReverseWarningProfile.forPreset("TONAL_BEEPER"), SAMPLE_RATE,
                telemetry(-1, true));

        byte[] cycle = voice.render(telemetry(-1, true), SAMPLE_RATE);
        long soundingEnergy = energy(cycle, SAMPLE_RATE / 10, SAMPLE_RATE * 4 / 10);
        long gapEnergy = energy(cycle, SAMPLE_RATE * 6 / 10, SAMPLE_RATE * 9 / 10);

        assertTrue(soundingEnergy > 10_000_000L);
        assertTrue(gapEnergy < soundingEnergy / 100);
    }

    @Test
    public void broadbandWarningProducesItsOwnGeneratedSignal() {
        ReverseWarningVoice voice = new ReverseWarningVoice(
                ReverseWarningProfile.forPreset("BROADBAND"), SAMPLE_RATE,
                telemetry(-1, true));

        assertTrue(energy(voice.render(telemetry(-1, true), 8_192)) > 1_000_000L);
    }

    private static EngineTelemetry telemetry(int gear, boolean engineOn) {
        return new EngineTelemetry(engineOn ? 800 : 0, 2_500, 0, 0.1F,
                gear < 0 ? -4 : 0, gear, engineOn, false, false,
                System.nanoTime());
    }

    private static long energy(byte[] pcm) {
        return energy(pcm, 0, pcm.length / 2);
    }

    private static long energy(byte[] pcm, int firstSample, int lastSample) {
        long energy = 0;
        for (int sampleIndex = firstSample; sampleIndex < lastSample; sampleIndex++) {
            int offset = sampleIndex * 2;
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            energy += Math.abs((int) sample);
        }
        return energy;
    }
}
