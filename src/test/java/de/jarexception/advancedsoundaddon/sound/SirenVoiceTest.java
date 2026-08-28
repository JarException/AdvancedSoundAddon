package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SirenVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void countryPresetsRetainTheirRequiredTonePairsAndTiming() {
        SirenProfile germanFire = SirenProfile.forPreset("DE_FIRE");
        SirenProfile frenchFire = SirenProfile.forPreset("FR_FIRE");
        SirenProfile frenchSamu = SirenProfile.forPreset("FR_SAMU");
        SirenProfile usWail = SirenProfile.forPreset("US_WAIL");
        SirenProfile usYelp = SirenProfile.forPreset("US_YELP");

        assertArrayEquals(new float[]{458, 608},
                SirenProfile.forPreset("DE_POLICE").getPrimaryFrequenciesHz(), 0.001F);
        assertArrayEquals(new float[]{435, 580}, germanFire.getPrimaryFrequenciesHz(), 0.001F);
        assertArrayEquals(new float[]{450, 600}, germanFire.getSecondaryFrequenciesHz(), 0.001F);
        assertArrayEquals(new float[]{0.75F, 0.75F}, germanFire.getDurationsSeconds(), 0.001F);
        assertArrayEquals(new float[]{435, 488}, frenchFire.getPrimaryFrequenciesHz(), 0.001F);
        assertArrayEquals(new float[]{435, 651}, frenchSamu.getPrimaryFrequenciesHz(), 0.001F);
        assertEquals(SirenProfile.Pattern.SINE, usWail.getPattern());
        assertEquals(SirenProfile.Pattern.TRIANGLE, usYelp.getPattern());
        assertArrayEquals(new float[]{725, 1_800}, usWail.getPrimaryFrequenciesHz(), 0.001F);
        assertTrue(usYelp.getDurationsSeconds()[0] < usWail.getDurationsSeconds()[0]);
        assertEquals("AIR_HORN", germanFire.getSourceName());
        assertEquals("ELECTRONIC_SPEAKER",
                SirenProfile.forPreset("DE_POLICE").getSourceName());
        assertEquals("ELECTRONIC_SPEAKER", usWail.getSourceName());
        assertEquals("MECHANICAL_ROTOR",
                SirenProfile.forPreset("US_Q_SIREN").getSourceName());
    }

    @Test
    public void everyDocumentedCountrySirenResolvesWithoutCustomValues() {
        String[] presets = {"DE_POLICE", "DE_AMBULANCE", "DE_FIRE", "FR_POLICE",
                "FR_GENDARMERIE", "FR_FIRE", "FR_SAMU", "FR_AMBULANCE",
                "US_WAIL", "US_YELP", "US_HI_LO", "US_PRIORITY",
                "US_RUMBLER_WAIL", "US_RUMBLER_YELP", "US_Q_SIREN", "EU_HI_LO"};
        for (String preset : presets) {
            assertEquals(preset, SirenProfile.forPreset(preset).getPresetName());
        }
    }

    @Test
    public void enabledSirenRunsWithTheEngineOffAndFadesAfterSwitchOff() {
        long time = 1_000_000_000L;
        SirenVoice voice = new SirenVoice(SirenProfile.forPreset("DE_POLICE"),
                1.0F, SAMPLE_RATE, telemetry(false, false, time));

        byte[] active = voice.render(telemetry(true, false,
                time + 50_000_000L), CHUNK * 8);
        byte[] release = voice.render(telemetry(false, false,
                time + 100_000_000L), CHUNK * 4);

        assertTrue(energy(active) > 1_000_000L);
        assertTrue("speaker pressure must fade naturally instead of clicking off",
                energy(release) > 100_000L);
        for (int chunk = 0; chunk < 70; chunk++) {
            voice.render(telemetry(false, false,
                    time + 150_000_000L + chunk * 50_000_000L), CHUNK);
        }
        assertTrue(voice.isSilent());
    }

    @Test
    public void rumblerPresetHasLongerReachAndPhysicalLowTone() {
        SirenProfile standard = SirenProfile.forPreset("US_WAIL");
        SirenProfile rumbler = SirenProfile.forPreset("US_RUMBLER_WAIL");

        assertEquals(0.0F, standard.getSubharmonicGain(), 0.0001F);
        assertTrue(rumbler.getSubharmonicGain() > 0.3F);
        assertTrue(rumbler.getAudibleDistance() > standard.getAudibleDistance());
    }

    @Test
    public void cockpitAttenuatesTheExternalSirenSpeaker() {
        long time = 2_000_000_000L;
        SirenProfile profile = SirenProfile.forPreset("FR_GENDARMERIE");
        SirenVoice outside = new SirenVoice(profile, 1.0F, SAMPLE_RATE,
                telemetry(false, false, time));
        SirenVoice inside = new SirenVoice(profile, 1.0F, SAMPLE_RATE,
                telemetry(false, true, time));

        byte[] exterior = outside.render(telemetry(true, false,
                time + 50_000_000L), CHUNK * 8);
        byte[] interior = inside.render(telemetry(true, true,
                time + 50_000_000L), CHUNK * 8);

        assertTrue(energy(interior) < energy(exterior) * 0.60);
    }

    @Test
    public void germanAirValvesOverlapWithoutADeadGapAtToneChange() {
        long time = 3_000_000_000L;
        SirenVoice voice = new SirenVoice(SirenProfile.forPreset("DE_FIRE"),
                1.0F, SAMPLE_RATE, telemetry(false, false, time));
        EngineTelemetry active = telemetry(true, false, time + 50_000_000L);
        double duration = SirenProfile.forPreset("DE_FIRE").getDurationsSeconds()[0];
        voice.render(active, (int) (SAMPLE_RATE * (duration - 0.03)));
        byte[] transition = voice.render(active, (int) (SAMPLE_RATE * 0.08));

        int switchSample = (int) (SAMPLE_RATE * 0.03);
        assertTrue("real pneumatic valves must carry pressure through the tone change",
                windowEnergy(transition, switchSample - 120, 240) > 250_000L);
        assertTrue(windowEnergy(transition, switchSample + 240, 480) > 500_000L);
        assertTrue("linear horn mixing must not create a full-scale switch click",
                maximumAdjacentDelta(transition, switchSample - 240, 720) < 27_000);
    }

    @Test
    public void germanFireExteriorKeepsTheCleanCockpitHeadroom() {
        long time = 3_500_000_000L;
        SirenProfile profile = SirenProfile.forPreset("DE_FIRE");
        SirenVoice outside = new SirenVoice(profile, 0.66F, SAMPLE_RATE,
                telemetry(false, false, time));
        SirenVoice inside = new SirenVoice(profile, 0.66F, SAMPLE_RATE,
                telemetry(false, true, time));
        EngineTelemetry exteriorActive = telemetry(true, false, time + 50_000_000L);
        EngineTelemetry interiorActive = telemetry(true, true, time + 50_000_000L);
        outside.render(exteriorActive, SAMPLE_RATE / 4);
        inside.render(interiorActive, SAMPLE_RATE / 4);
        byte[] exterior = outside.render(exteriorActive, SAMPLE_RATE / 2);
        byte[] interior = inside.render(interiorActive, SAMPLE_RATE / 2);

        assertTrue("the four-bell exterior source must retain headroom before the final mixer",
                peak(exterior) < 20_000);
        assertTrue("outside should remain slightly stronger than the clean cockpit signal",
                energy(exterior) > energy(interior) * 1.02);
        assertTrue("outside must not re-enter the painful saturation range",
                energy(exterior) < energy(interior) * 1.15);
    }

    @Test
    public void frenchWarningSpeakerHasRequiredPenetratingUpperHarmonics() {
        long time = 4_000_000_000L;
        SirenVoice voice = new SirenVoice(SirenProfile.forPreset("FR_POLICE"),
                1.0F, SAMPLE_RATE, telemetry(false, false, time));
        EngineTelemetry active = telemetry(true, false, time + 50_000_000L);
        voice.render(active, SAMPLE_RATE / 5);
        byte[] lowTone = voice.render(active, 8_192);

        double fundamental = magnitudeAt(lowTone, 435);
        assertTrue(magnitudeAt(lowTone, 1_305) > fundamental);
        assertTrue(magnitudeAt(lowTone, 1_740) > fundamental);
    }

    @Test
    public void germanAmbulanceSpeakerMatchesTheBrightRecordedHarmonicBalance() {
        long time = 5_000_000_000L;
        SirenVoice voice = new SirenVoice(SirenProfile.forPreset("DE_AMBULANCE"),
                1.0F, SAMPLE_RATE, telemetry(false, false, time));
        EngineTelemetry active = telemetry(true, false, time + 50_000_000L);
        voice.render(active, SAMPLE_RATE / 4);
        byte[] lowTone = voice.render(active, 8_192);

        double fundamental = peakMagnitudeNear(lowTone, 458);
        double secondMode = peakMagnitudeNear(lowTone, 916);
        double thirdMode = peakMagnitudeNear(lowTone, 1_374);
        double fourthMode = peakMagnitudeNear(lowTone, 1_832);
        assertTrue("second=" + secondMode + " third=" + thirdMode,
                thirdMode > secondMode * 2.5);
        assertTrue("third=" + thirdMode + " fundamental=" + fundamental,
                thirdMode > fundamental * 2.5);
        assertTrue("fourth=" + fourthMode + " fundamental=" + fundamental,
                fourthMode > fundamental * 1.8);
    }

    @Test
    public void everyTwoTonePresetCrossesItsFirstBoundaryWithoutAFullScaleGlitch() {
        String[] presets = {"DE_POLICE", "DE_AMBULANCE", "DE_FIRE", "FR_POLICE",
                "FR_GENDARMERIE", "FR_FIRE", "FR_SAMU", "FR_AMBULANCE",
                "US_HI_LO", "EU_HI_LO"};
        long time = 6_000_000_000L;
        for (String presetName : presets) {
            SirenProfile profile = SirenProfile.forPreset(presetName);
            SirenVoice voice = new SirenVoice(profile, 1.0F, SAMPLE_RATE,
                    telemetry(false, false, time));
            EngineTelemetry active = telemetry(true, false, time + 50_000_000L);
            double firstDuration = profile.getDurationsSeconds()[0];
            voice.render(active, Math.max(1, (int) (SAMPLE_RATE * (firstDuration - 0.02))));
            byte[] transition = voice.render(active, (int) (SAMPLE_RATE * 0.05));
            int switchSample = (int) (SAMPLE_RATE * 0.02);
            assertTrue(presetName + " produces a discontinuity at its tone boundary",
                    maximumAdjacentDelta(transition, switchSample - 240, 480) < 29_000);
        }
    }

    private static EngineTelemetry telemetry(boolean siren, boolean interior, long timestamp) {
        return new EngineTelemetry(0, 7_000, 0, 0, 0, 0,
                false, false, false, 0, false, siren, interior, timestamp);
    }

    private static long energy(byte[] pcm) {
        long result = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            result += Math.abs((int) sample);
        }
        return result;
    }

    private static long windowEnergy(byte[] pcm, int firstSample, int sampleCount) {
        long result = 0;
        int end = Math.min(pcm.length / 2, firstSample + sampleCount);
        for (int sampleIndex = Math.max(0, firstSample); sampleIndex < end; sampleIndex++) {
            int offset = sampleIndex * 2;
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            result += Math.abs((int) sample);
        }
        return result;
    }

    private static int maximumAdjacentDelta(byte[] pcm, int firstSample, int sampleCount) {
        int first = Math.max(1, firstSample);
        int end = Math.min(pcm.length / 2, firstSample + sampleCount);
        int maximum = 0;
        for (int index = first; index < end; index++) {
            int offset = index * 2;
            int previousOffset = offset - 2;
            short current = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            short previous = (short) ((pcm[previousOffset] & 0xFF)
                    | (pcm[previousOffset + 1] << 8));
            maximum = Math.max(maximum, Math.abs((int) current - previous));
        }
        return maximum;
    }

    private static int peak(byte[] pcm) {
        int result = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            result = Math.max(result, Math.abs((int) sample));
        }
        return result;
    }

    private static double magnitudeAt(byte[] pcm, double frequency) {
        double real = 0.0;
        double imaginary = 0.0;
        int count = pcm.length / 2;
        for (int index = 0; index < count; index++) {
            int offset = index * 2;
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            double phase = TWO_PI * frequency * index / SAMPLE_RATE;
            real += sample * Math.cos(phase);
            imaginary -= sample * Math.sin(phase);
        }
        return Math.sqrt(real * real + imaginary * imaginary) / count;
    }

    private static double peakMagnitudeNear(byte[] pcm, double centerFrequency) {
        double peak = 0.0;
        for (double frequency = centerFrequency - 30; frequency <= centerFrequency + 30;
             frequency += 2.0) {
            peak = Math.max(peak, magnitudeAt(pcm, frequency));
        }
        return peak;
    }

    private static final double TWO_PI = Math.PI * 2.0;
}
