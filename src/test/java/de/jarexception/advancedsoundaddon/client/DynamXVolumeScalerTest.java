package de.jarexception.advancedsoundaddon.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DynamXVolumeScalerTest {
    @Test
    public void dynamXPercentageScalesProceduralEngineLinearly() {
        float engineGain = AdvancedSoundSettings.ENGINE_OUTPUT_GAIN;
        assertEquals(0.0F, DynamXVolumeScaler.apply(engineGain, 0.0F), 0.0001F);
        assertEquals(engineGain * 0.10F, DynamXVolumeScaler.apply(engineGain, 0.10F), 0.0001F);
        assertEquals(engineGain * 0.50F, DynamXVolumeScaler.apply(engineGain, 0.50F), 0.0001F);
        assertEquals(engineGain, DynamXVolumeScaler.apply(engineGain, 1.0F), 0.0001F);
    }

    @Test
    public void invalidDynamXValuesAreClampedToNativeRange() {
        assertEquals(0.0F, DynamXVolumeScaler.apply(0.8F, -2.0F), 0.0001F);
        assertEquals(0.8F, DynamXVolumeScaler.apply(0.8F, 3.0F), 0.0001F);
    }
}
