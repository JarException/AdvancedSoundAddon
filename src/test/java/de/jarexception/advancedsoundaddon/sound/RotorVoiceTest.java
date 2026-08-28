package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RotorVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void presetsRepresentDistinctTwoAndFourBladePassRates() {
        RotorProfile bell = RotorProfile.forPreset("HELICOPTER_2_BLADE");
        RotorProfile ec145 = RotorProfile.forPreset("HELICOPTER_4_BLADE");

        assertEquals(2, bell.getBladeCount());
        assertEquals(4, ec145.getBladeCount());
        assertEquals(13.13F, bell.getNominalBladePassHz(), 0.05F);
        assertEquals(25.53F, ec145.getNominalBladePassHz(), 0.05F);
        assertEquals(4.32F, bell.getOutputGain(), 0.001F);
        assertEquals(3.68F, ec145.getOutputGain(), 0.001F);
    }

    @Test
    public void rotorLayerProducesBladeSlapAndMixesIndependentlyFromTurbine() {
        EngineTelemetry running = telemetry(6_650, true);
        EngineProfile turbine = EngineProfile.forPreset("TURBOSHAFT");
        EngineVoice turbineOnly = new EngineVoice(turbine, SAMPLE_RATE, running);
        EngineVoice withRotor = new EngineVoice(turbine,
                RotorProfile.forPreset("HELICOPTER_2_BLADE"), SAMPLE_RATE, running);

        turbineOnly.render(running, CHUNK * 5);
        withRotor.render(running, CHUNK * 5);
        byte[] turbinePcm = turbineOnly.render(running, CHUNK * 8);
        byte[] rotorPcm = withRotor.render(running, CHUNK * 8);

        assertTrue(energy(rotorPcm) > 1_000_000L);
        assertFalse("rotor layer must not be baked into the turboshaft voice",
                Arrays.equals(turbinePcm, rotorPcm));
    }

    @Test
    public void rotorCoastsDownAfterEngineStop() {
        RotorVoice voice = new RotorVoice(RotorProfile.forPreset("HELICOPTER_4_BLADE"),
                SAMPLE_RATE, telemetry(6_500, true));
        EngineTelemetry stopped = telemetry(0, false);
        for (int index = 0; index < 560; index++) {
            voice.render(stopped, CHUNK);
        }
        assertTrue(voice.isSilent());
    }

    private static EngineTelemetry telemetry(float rpm, boolean engineOn) {
        return new EngineTelemetry(rpm, 7_000, 0.72F, 0.82F,
                45, 1, engineOn, false, false, System.nanoTime());
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
