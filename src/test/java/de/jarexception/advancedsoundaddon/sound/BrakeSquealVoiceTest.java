package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BrakeSquealVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void presetsRepresentDistinctBrakeConstructions() {
        BrakeSquealProfile classic = BrakeSquealProfile.forPreset("CLASSIC_DISC");
        BrakeSquealProfile ceramic = BrakeSquealProfile.forPreset("CARBON_CERAMIC");
        BrakeSquealProfile drum = BrakeSquealProfile.forPreset("OLD_DRUM");

        assertEquals("CLASSIC_DISC", classic.getPresetName());
        assertEquals("CARBON_CERAMIC", ceramic.getPresetName());
        assertEquals("OLD_DRUM", drum.getPresetName());
        assertTrue(drum.getPrimaryModeHz() < classic.getPrimaryModeHz());
        assertTrue(classic.getPrimaryModeHz() < ceramic.getPrimaryModeHz());
        assertTrue(drum.getMaximumSpeedKmh() < ceramic.getMaximumSpeedKmh());
    }

    @Test
    public void brakingAtRoadSpeedExcitesSyntheticFrictionModes() {
        long time = 1_000_000_000L;
        BrakeSquealVoice voice = new BrakeSquealVoice(
                BrakeSquealProfile.forPreset("CARBON_CERAMIC"), SAMPLE_RATE,
                telemetry(62, false, time));

        byte[] pcm = voice.render(telemetry(56, true, time + 100_000_000L), CHUNK * 8);

        assertTrue("braking must excite audible synthetic modes", energy(pcm) > 1_000_000L);
        assertTrue(!voice.isSilent());
    }

    @Test
    public void coastingWithoutBrakingDoesNotCreateSqueal() {
        long time = 2_000_000_000L;
        BrakeSquealVoice voice = new BrakeSquealVoice(
                BrakeSquealProfile.forPreset("CLASSIC_DISC"), SAMPLE_RATE,
                telemetry(38, false, time));

        byte[] pcm = voice.render(telemetry(31, false, time + 100_000_000L), CHUNK * 4);

        assertEquals(0L, energy(pcm));
        assertTrue(voice.isSilent());
    }

    @Test
    public void inferredTemperatureRisesWithSustainedBraking() {
        long time = 3_000_000_000L;
        BrakeSquealVoice voice = new BrakeSquealVoice(
                BrakeSquealProfile.forPreset("CARBON_CERAMIC"), SAMPLE_RATE,
                telemetry(72, false, time));
        double cold = voice.getEstimatedTemperatureC();

        for (int tick = 1; tick <= 80; tick++) {
            voice.render(telemetry(68, true, time + tick * 250_000_000L), 128);
        }

        assertTrue("brake temperature must be inferred without native telemetry",
                voice.getEstimatedTemperatureC() > cold + 25.0);
    }

    @Test
    public void squealFadesOutAfterBrakeRelease() {
        long time = 4_000_000_000L;
        BrakeSquealVoice voice = new BrakeSquealVoice(
                BrakeSquealProfile.forPreset("OLD_DRUM"), SAMPLE_RATE,
                telemetry(22, false, time));
        voice.render(telemetry(17, true, time + 100_000_000L), CHUNK * 4);

        EngineTelemetry released = telemetry(16, false, time + 200_000_000L);
        for (int chunk = 0; chunk < 45; chunk++) {
            voice.render(released, CHUNK);
        }

        assertTrue("friction modes must stop shortly after brake release", voice.isSilent());
    }

    private static EngineTelemetry telemetry(float speed, boolean brakeApplied, long timestamp) {
        return new EngineTelemetry(2_400, 7_000, 0, 0.25F, speed, 3,
                true, false, brakeApplied, false, timestamp);
    }

    private static long energy(byte[] pcm) {
        long energy = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            energy += Math.abs((int) sample);
        }
        return energy;
    }
}
