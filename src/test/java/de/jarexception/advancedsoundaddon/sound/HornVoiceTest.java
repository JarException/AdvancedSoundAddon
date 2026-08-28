package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HornVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void presetsCoverDistinctVehicleClasses() {
        HornProfile compact = HornProfile.forPreset("COMPACT_CAR");
        HornProfile sport = HornProfile.forPreset("SPORT_CAR");
        HornProfile truck = HornProfile.forPreset("TRUCK_AIR");
        HornProfile bus = HornProfile.forPreset("BUS_AIR");
        HornProfile marine = HornProfile.forPreset("MARINE");

        assertTrue(truck.getFrequenciesHz()[0] < bus.getFrequenciesHz()[0]);
        assertTrue(bus.getFrequenciesHz()[0] < compact.getFrequenciesHz()[0]);
        assertTrue(compact.getFrequenciesHz()[0] < sport.getFrequenciesHz()[0]);
        assertTrue(marine.getAudibleDistance() > compact.getAudibleDistance());
        assertFalse(Arrays.equals(truck.getFrequenciesHz(), marine.getFrequenciesHz()));
        assertEquals("ELECTRIC_DISC", compact.getSourceName());
        assertEquals("AIR_TRUMPET", truck.getSourceName());
        assertEquals("MARINE_TRUMPET", marine.getSourceName());
    }

    @Test
    public void everyDocumentedHornPresetResolvesWithoutCustomValues() {
        String[] presets = {"COMPACT_CAR", "STANDARD_CAR", "LUXURY_CAR", "SPORT_CAR",
                "CLASSIC_CAR", "MOTORCYCLE", "TRUCK_AIR", "BUS_AIR", "UTILITY", "MARINE"};
        for (String preset : presets) {
            assertEquals(preset, HornProfile.forPreset(preset).getPresetName());
        }
    }

    @Test
    public void oneTickButtonPressProducesACompletePhysicalHornPulse() {
        long time = 1_000_000_000L;
        HornVoice voice = new HornVoice(HornProfile.forPreset("STANDARD_CAR"),
                1.0F, SAMPLE_RATE, telemetry(false, false, time));

        byte[] attack = voice.render(telemetry(true, false, time + 50_000_000L), CHUNK);
        byte[] tail = voice.render(telemetry(false, false, time + 100_000_000L), CHUNK * 8);

        assertTrue(energy(attack) > 100_000L);
        assertTrue("a networked one-tick input must still play the configured horn body",
                energy(tail) > 1_000_000L);
    }

    @Test
    public void holdingTheButtonSustainsOneContinuousHornUntilRelease() {
        long time = 2_000_000_000L;
        HornVoice voice = new HornVoice(HornProfile.forPreset("UTILITY"),
                1.0F, SAMPLE_RATE, telemetry(false, false, time));

        long energy = 0;
        for (int tick = 1; tick <= 55; tick++) {
            energy += energy(voice.render(telemetry(true, false,
                    time + tick * 50_000_000L), CHUNK));
        }

        assertTrue(energy > 1_000_000L);
        assertFalse("a held input must sustain one continuous horn", voice.isSilent());

        for (int tick = 56; tick <= 75; tick++) {
            voice.render(telemetry(false, false,
                    time + tick * 50_000_000L), CHUNK);
        }
        assertTrue("releasing the button must finish the pressure tail", voice.isSilent());
    }

    @Test
    public void resyncDuringAOneTickPressDoesNotLoseTheHorn() {
        long time = 3_000_000_000L;
        HornVoice voice = new HornVoice(HornProfile.forPreset("TRUCK_AIR"),
                0.60F, SAMPLE_RATE, telemetry(false, false, time));
        EngineTelemetry pressed = telemetry(true, false, time + 50_000_000L);

        voice.requestResync(pressed);
        byte[] pcm = voice.render(pressed, CHUNK * 5);

        assertTrue("attaching an audible voice during the press must preserve the event",
                energy(pcm) > 1_000_000L);
        assertEquals(82.0F, voice.getAudibleDistance(), 0.0001F);
    }

    @Test
    public void cockpitAttenuatesExternalHornPressure() {
        long time = 4_000_000_000L;
        HornProfile profile = HornProfile.forPreset("LUXURY_CAR");
        HornVoice outside = new HornVoice(profile, 1.0F, SAMPLE_RATE,
                telemetry(false, false, time));
        HornVoice inside = new HornVoice(profile, 1.0F, SAMPLE_RATE,
                telemetry(false, true, time));

        byte[] exterior = outside.render(telemetry(true, false,
                time + 50_000_000L), CHUNK * 6);
        byte[] interior = inside.render(telemetry(true, true,
                time + 50_000_000L), CHUNK * 6);

        assertTrue(energy(interior) < energy(exterior) * 0.60);
    }

    @Test
    public void diaphragmModelProducesStrongOvertonesInsteadOfOneSterileSine() {
        long time = 5_000_000_000L;
        HornVoice voice = new HornVoice(HornProfile.forPreset("UTILITY"),
                1.0F, SAMPLE_RATE, telemetry(false, false, time));
        EngineTelemetry active = telemetry(true, false, time + 50_000_000L);
        voice.render(active, SAMPLE_RATE / 5);
        byte[] body = voice.render(active, 8_192);

        double fundamental = magnitudeAt(body, 360);
        double second = magnitudeAt(body, 720);
        double third = magnitudeAt(body, 1_080);
        assertTrue("a physical diaphragm needs audible upper modes",
                second > fundamental * 0.07);
        assertTrue("the horn bell must retain more than one overtone",
                third > fundamental * 0.025);
    }

    private static EngineTelemetry telemetry(boolean horn, boolean interior, long timestamp) {
        return new EngineTelemetry(0, 7_000, 0, 0, 0, 0,
                false, false, false, 0, horn, false, interior, timestamp);
    }

    private static long energy(byte[] pcm) {
        long result = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            result += Math.abs((int) sample);
        }
        return result;
    }

    private static double magnitudeAt(byte[] pcm, double frequency) {
        double real = 0.0;
        double imaginary = 0.0;
        int count = pcm.length / 2;
        for (int index = 0; index < count; index++) {
            short sample = (short) ((pcm[index * 2] & 0xFF) | (pcm[index * 2 + 1] << 8));
            double phase = Math.PI * 2.0 * frequency * index / SAMPLE_RATE;
            real += sample * Math.cos(phase);
            imaginary -= sample * Math.sin(phase);
        }
        return Math.sqrt(real * real + imaginary * imaginary) / count;
    }
}
