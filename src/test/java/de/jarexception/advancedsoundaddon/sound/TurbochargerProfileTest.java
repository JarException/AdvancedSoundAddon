package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TurbochargerProfileTest {
    @Test
    public void naturallyAspiratedSuperchargedAndElectricProfilesStayTurboFree() {
        assertNull(turboFor("V8_MUSCLE"));
        assertNull(turboFor("V8_SUPERCHARGED_MODERN"));
        assertNull(turboFor("ELECTRIC"));
    }

    @Test
    public void gtRTwinTurboIsMoreExposedThanLuxuryTurbo() {
        TurbochargerProfile gtR = turboFor("V6_TWIN_TURBO");
        TurbochargerProfile luxury = turboFor("V8_LUXURY_TURBO");

        assertNotNull(gtR);
        assertNotNull(luxury);
        assertEquals("GT_R_TWIN_TURBO", gtR.getPresetName());
        assertEquals(2, gtR.getCompressorCount());
        assertTrue(gtR.getWhistleGain() > luxury.getWhistleGain() * 2.0F);
        assertTrue(gtR.getReleaseGain() > luxury.getReleaseGain() * 3.0F);
        assertTrue(gtR.getReleaseDurationSeconds() > luxury.getReleaseDurationSeconds() * 1.8F);
    }

    @Test
    public void hypercarHasFourCompressorsAndDieselsKeepRestrainedTurboProfiles() {
        TurbochargerProfile hypercar = turboFor("W16_HYPERCAR");
        TurbochargerProfile refinedDiesel = turboFor("I4_DIESEL_REFINED");
        TurbochargerProfile heavyDiesel = turboFor("I6_HEAVY_DIESEL");

        assertNotNull(hypercar);
        assertEquals(4, hypercar.getCompressorCount());
        assertNotNull(refinedDiesel);
        assertNotNull(heavyDiesel);
        assertTrue(refinedDiesel.getReleaseGain() < 0.01F);
        assertTrue(heavyDiesel.getWhistleGain() < hypercar.getWhistleGain());
    }

    private static TurbochargerProfile turboFor(String enginePreset) {
        return TurbochargerProfile.forEngineProfile(EngineProfile.forPreset(enginePreset));
    }
}
