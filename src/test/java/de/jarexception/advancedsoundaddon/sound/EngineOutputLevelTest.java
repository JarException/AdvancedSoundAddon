package de.jarexception.advancedsoundaddon.sound;

import de.jarexception.advancedsoundaddon.client.AdvancedSoundSettings;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** Guards the perceived output body against becoming quiet relative to DynamX effects. */
public class EngineOutputLevelTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void roadEngineOutputHasUsefulRmsWithoutHardClipping() {
        double quietestRms = Double.MAX_VALUE;
        EngineLayout quietestLayout = null;
        double loudestRms = 0.0;
        double highestSaturation = 0.0;
        for (EngineLayout layout : EngineLayout.values()) {
            EngineTelemetry telemetry = new EngineTelemetry(3_200, 7_000, 0.65F, 0.72F,
                    55, 3, true, false, false, System.nanoTime());
            EngineVoice voice = new EngineVoice(EngineProfile.forLayout(layout), SAMPLE_RATE, telemetry);
            voice.render(telemetry, CHUNK * 4);
            byte[] pcm = voice.render(telemetry, CHUNK * 8);
            Level level = mixedLevel(pcm, AdvancedSoundSettings.ENGINE_OUTPUT_GAIN);
            if (level.rms < quietestRms) {
                quietestRms = level.rms;
                quietestLayout = layout;
            }
            loudestRms = Math.max(loudestRms, level.rms);
            highestSaturation = Math.max(highestSaturation, level.saturation);
        }

        double minimumRms = quietestLayout == EngineLayout.I1 ? 0.30 : 0.35;
        assertTrue("quietest engine RMS is too low for " + quietestLayout + ": " + quietestRms,
                quietestRms >= minimumRms);
        assertTrue("engine output is excessively compressed: " + highestSaturation,
                highestSaturation < 0.08);
        assertTrue("loudest engine RMS is implausibly high: " + loudestRms, loudestRms < 0.72);
    }

    private static Level mixedLevel(byte[] pcm, double gain) {
        double squareSum = 0.0;
        int saturated = 0;
        int samples = pcm.length / 2;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short value = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            double mixed = Math.tanh(value / 32768.0 * gain);
            squareSum += mixed * mixed;
            if (Math.abs(mixed) >= 0.95) {
                saturated++;
            }
        }
        return new Level(Math.sqrt(squareSum / samples), saturated / (double) samples);
    }

    private static final class Level {
        final double rms;
        final double saturation;

        Level(double rms, double saturation) {
            this.rms = rms;
            this.saturation = saturation;
        }
    }
}
