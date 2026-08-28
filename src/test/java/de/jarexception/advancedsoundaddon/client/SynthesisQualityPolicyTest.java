package de.jarexception.advancedsoundaddon.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SynthesisQualityPolicyTest {
    @Test
    public void nearestV8RetainsFullFluidQuality() {
        assertEquals(8, SynthesisQualityPolicy.fluidSubsteps(0, 8));
    }

    @Test
    public void highCylinderCountsStayInsideV8WorkBudget() {
        assertEquals(6, SynthesisQualityPolicy.fluidSubsteps(0, 10));
        assertEquals(5, SynthesisQualityPolicy.fluidSubsteps(0, 12));
        assertEquals(4, SynthesisQualityPolicy.fluidSubsteps(0, 16));
    }

    @Test
    public void distanceStillReducesQualityForEveryLayout() {
        assertEquals(6, SynthesisQualityPolicy.fluidSubsteps(3, 8));
        assertEquals(4, SynthesisQualityPolicy.fluidSubsteps(6, 8));
        assertEquals(4, SynthesisQualityPolicy.fluidSubsteps(6, 16));
    }

    @Test
    public void backgroundModeKeepsEveryVoiceInsideRealtimeBudget() {
        assertEquals(4, SynthesisQualityPolicy.fluidSubsteps(0, 4, false));
        assertEquals(4, SynthesisQualityPolicy.fluidSubsteps(0, 8, false));
        assertEquals(4, SynthesisQualityPolicy.fluidSubsteps(0, 16, false));
    }
}
