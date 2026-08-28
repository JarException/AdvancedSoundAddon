package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class EngineVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void synthesizesStartRunLimiterAndStopWithoutSamples() {
        EngineProfile profile = EngineProfile.forLayout(EngineLayout.V8_CROSSPLANE);
        EngineTelemetry off = telemetry(0, false, false, 0, 0);
        EngineVoice voice = new EngineVoice(profile, SAMPLE_RATE, off);

        EngineTelemetry starting = telemetry(850, true, false, 0.25F, 0.30F);
        long energy = 0;
        int clipped = 0;
        int rendered = 0;
        for (int i = 0; i < 34; i++) {
            byte[] pcm = voice.render(starting, CHUNK);
            energy += absoluteEnergy(pcm);
            clipped += clippedSamples(pcm);
            rendered += CHUNK;
        }
        assertTrue("synthetic start should carry audible energy", energy > 2_000_000L);
        assertTrue("soft limiter should prevent sustained clipping", clipped < rendered / 100);
        assertFalse(voice.isSilent());

        EngineTelemetry limiter = telemetry(6_980, true, true, 1.0F, 0.95F);
        byte[] limited = voice.render(limiter, CHUNK * 4);
        assertTrue(absoluteEnergy(limited) > 1_000_000L);

        EngineTelemetry stopped = telemetry(0, false, false, 0, 0);
        for (int i = 0; i < 110; i++) {
            voice.render(stopped, CHUNK);
        }
        assertTrue("stop sequence and resonator tail should eventually finish", voice.isSilent());
    }

    @Test
    public void layoutsProduceDifferentPcmAtIdenticalRpm() {
        EngineTelemetry running = telemetry(3_200, true, false, 0.72F, 0.78F);
        EngineVoice inlineFour = new EngineVoice(EngineProfile.forLayout(EngineLayout.I4), SAMPLE_RATE, running);
        EngineVoice crossplaneV8 = new EngineVoice(EngineProfile.forLayout(EngineLayout.V8_CROSSPLANE), SAMPLE_RATE, running);

        byte[] i4 = inlineFour.render(running, CHUNK * 3);
        byte[] v8 = crossplaneV8.render(running, CHUNK * 3);
        assertFalse(Arrays.equals(i4, v8));
        assertNotEquals(zeroCrossings(i4), zeroCrossings(v8));
    }

    @Test
    public void roadTripleIsQuieterAndLessMotorcycleBrightThanSportTriple() {
        EngineTelemetry running = telemetry(1_050, true, false, 0.18F, 0.34F);
        EngineVoice road = new EngineVoice(EngineProfile.forPreset("I3_ROAD"),
                SAMPLE_RATE, running);
        EngineVoice sport = new EngineVoice(EngineProfile.forPreset("I3_SPORT"),
                SAMPLE_RATE, running);
        road.render(running, CHUNK * 4);
        sport.render(running, CHUNK * 4);
        byte[] roadPcm = road.render(running, CHUNK * 10);
        byte[] sportPcm = sport.render(running, CHUNK * 10);

        double roadEnergy = meanSquare(roadPcm);
        double sportEnergy = meanSquare(sportPcm);
        double roadPresence = highPassMeanSquare(roadPcm, SAMPLE_RATE, 1_500) / roadEnergy;
        double sportPresence = highPassMeanSquare(sportPcm, SAMPLE_RATE, 1_500) / sportEnergy;
        assertTrue("production road triple must be quieter before its lower profile gain",
                roadEnergy < sportEnergy * 0.80);
        assertTrue("production road triple must not retain motorcycle-like high-mid bark",
                roadPresence < sportPresence * 0.25);
    }

    @Test
    public void sameLayoutVehicleCharactersRemainAudiblyDistinct() {
        assertCharacterDifference("I3_CITY", "I3_TURBO_ROAD", 1_800, 6_500);
        assertCharacterDifference("I6_LUXURY_SPORT", "I6_PERFORMANCE", 3_200, 7_200);
        assertCharacterDifference("V8_LUXURY_TURBO", "V8_MUSCLE", 2_800, 6_500);
        assertCharacterDifference("V12_LUXURY", "V12_RACE", 3_200, 7_500);
    }

    @Test
    public void runningEngineRetainsCombustionPresenceAboveTwoKilohertz() {
        EngineTelemetry running = telemetry(4_200, true, false, 0.82F, 0.88F);
        EngineVoice voice = new EngineVoice(EngineProfile.forLayout(EngineLayout.I4), SAMPLE_RATE, running);
        voice.render(running, CHUNK * 2);
        byte[] pcm = voice.render(running, CHUNK * 8);

        double fullBand = meanSquare(pcm);
        double highBand = highPassMeanSquare(pcm, SAMPLE_RATE, 2_000.0);
        double ratio = highBand / fullBand;
        assertTrue("combustion/valvetrain presence must not be filtered into a dull low-frequency loop; ratio=" + ratio,
                ratio > 0.020);
    }

    @Test
    public void exhaustPipelineRejectsDcAndSubBassSpeakerPlugArtifacts() {
        EngineTelemetry running = telemetry(4_200, true, false, 0.82F, 0.88F);
        EngineVoice voice = new EngineVoice(EngineProfile.forLayout(EngineLayout.I6), SAMPLE_RATE, running);
        voice.render(running, CHUNK * 3);
        byte[] pcm = voice.render(running, CHUNK * 8);

        double fullBand = meanSquare(pcm);
        double subBass = lowPassMeanSquare(pcm, SAMPLE_RATE, 55.0);
        double dc = mean(pcm);
        assertTrue("10 Hz DC rejection must keep waveform centered", dc * dc / fullBand < 0.001);
        double subBassRatio = subBass / fullBand;
        assertTrue("sub-bass pressure steps must not dominate the engine signal; ratio=" + subBassRatio,
                subBassRatio < 0.060);
    }

    @Test
    public void everySupportedLayoutProducesDistinctStableAudio() {
        EngineTelemetry running = telemetry(3_600, true, false, 0.74F, 0.82F);
        Set<Integer> fingerprints = new HashSet<>();
        for (EngineLayout layout : EngineLayout.values()) {
            EngineVoice voice = new EngineVoice(EngineProfile.forLayout(layout), SAMPLE_RATE, running);
            voice.render(running, CHUNK * 2);
            byte[] pcm = voice.render(running, CHUNK * 3);
            assertTrue(layout + " must produce audible PCM", absoluteEnergy(pcm) > 1_000_000L);
            int saturated = 0;
            for (int i = 0; i < pcm.length; i += 2) {
                int value = Math.abs((short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8)));
                if (value >= 31_400) saturated++;
            }
            assertTrue(layout + " must not sit on the PCM limiter; saturated=" + saturated,
                    saturated < pcm.length / 2_000);
            fingerprints.add(Arrays.hashCode(pcm));
        }
        assertEquals("firing order, bank routing and gas geometry must distinguish every layout",
                EngineLayout.values().length, fingerprints.size());
    }

    @Test
    public void dieselPresetsKeepContinuousBodyWithoutDominantRumbleOrBroadbandBuzz() {
        String[] presets = {"I4_DIESEL", "I4_DIESEL_REFINED", "I4_DIESEL_UTILITY",
                "I4_DIESEL_OFFROAD", "I6_DIESEL", "I6_BUS_DIESEL",
                "I6_TRUCK_DIESEL", "I6_HEAVY_DIESEL", "V8_DIESEL",
                "V8_DIESEL_ARMORED"};
        byte[][] outputs = new byte[presets.length][];
        for (int index = 0; index < presets.length; index++) {
            EngineProfile profile = EngineProfile.forPreset(presets[index]);
            float maximumRpm = profile.resolveAcousticMaxRpm(5_500);
            EngineTelemetry running = new EngineTelemetry(maximumRpm * 0.64F, maximumRpm,
                    0.68F, 0.82F, 35, 3, true, false, false, System.nanoTime());
            EngineVoice voice = new EngineVoice(profile,
                    SAMPLE_RATE, running);
            voice.render(running, CHUNK * 3);
            outputs[index] = voice.render(running, CHUNK * 8);

            double fullBand = meanSquare(outputs[index]);
            double lowBandRatio = lowPassMeanSquare(outputs[index], SAMPLE_RATE, 70.0) / fullBand;
            double presenceRatio = highPassMeanSquare(outputs[index], SAMPLE_RATE, 900.0) / fullBand;
            double buzzRatio = highPassMeanSquare(outputs[index], SAMPLE_RATE, 3_000.0) / fullBand;
            assertTrue(presets[index] + " must not be dominated by exhaust rumble; ratio=" + lowBandRatio,
                    lowBandRatio < 0.25);
            assertTrue(presets[index] + " must retain exhaust and mechanical presence; ratio=" + presenceRatio,
                    presenceRatio > 0.020);
            assertTrue(presets[index] + " must not contain a dominant chainsaw-like broadband layer; ratio=" + buzzRatio,
                    buzzRatio < 0.16);
            assertTrue(presets[index] + " must not sit on the PCM limiter",
                    clippedSamples(outputs[index]) < outputs[index].length / 200);
        }
        assertFalse("I4 and I6 diesel firing must remain distinct", Arrays.equals(outputs[0], outputs[4]));
        assertFalse("road and truck I6 tuning must remain distinct", Arrays.equals(outputs[4], outputs[6]));
    }

    @Test
    public void dieselIdleHasCompressedLowMidBodyRatherThanSparseSawPulses() {
        String[] presets = {"I4_DIESEL", "I4_DIESEL_REFINED", "I4_DIESEL_UTILITY",
                "I4_DIESEL_OFFROAD", "I6_DIESEL", "I6_BUS_DIESEL",
                "I6_TRUCK_DIESEL", "I6_HEAVY_DIESEL", "V8_DIESEL",
                "V8_DIESEL_ARMORED"};
        for (String preset : presets) {
            EngineProfile profile = EngineProfile.forPreset(preset);
            float maximumRpm = profile.resolveAcousticMaxRpm(5_500);
            EngineTelemetry idle = new EngineTelemetry(profile.getIdleRpm(), maximumRpm,
                    0.10F, 0.30F, 0, 0, true, false, false, System.nanoTime());
            EngineVoice voice = new EngineVoice(profile, SAMPLE_RATE, idle);
            voice.render(idle, CHUNK * 4);
            byte[] pcm = voice.render(idle, CHUNK * 12);

            double fullBand = meanSquare(pcm);
            double crestFactor = maximumAbsoluteSample(pcm) / Math.sqrt(fullBand);
            double lowMidBody = lowPassMeanSquare(pcm, SAMPLE_RATE, 180.0) / fullBand;
            double subBass = lowPassMeanSquare(pcm, SAMPLE_RATE, 70.0) / fullBand;
            double chainsawBuzz = highPassMeanSquare(pcm, SAMPLE_RATE, 3_000.0) / fullBand;
            assertTrue(preset + " idle pressure peaks must be rounded; crest=" + crestFactor,
                    crestFactor < 3.8);
            assertTrue(preset + " idle must retain a continuous block/exhaust body; ratio=" + lowMidBody,
                    lowMidBody > 0.28);
            assertTrue(preset + " idle must not collapse into sub-bass helicopter rumble; ratio=" + subBass,
                    subBass < 0.16);
            assertTrue(preset + " idle must not regress to broadband chainsaw buzz; ratio=" + chainsawBuzz,
                    chainsawBuzz < 0.03);

            double[] windows = windowMeanSquares(pcm, SAMPLE_RATE / 25);
            double[] sorted = windows.clone();
            Arrays.sort(sorted);
            assertTrue(preset + " idle must not drop out between combustion events",
                    sorted[0] > sorted[sorted.length / 2] * 0.05);
        }
    }

    @Test
    public void performanceLayoutsKeepContinuousLowRpmBodyAtRealtimeQuality() {
        String[] presets = {"V8_FLATPLANE", "V8_FLATPLANE_TURBO", "V8_FLATPLANE_RACE",
                "V12", "V12_LUXURY", "V12_RACE", "W16", "W16_HYPERCAR"};
        int[] fluidSubsteps = {8, 8, 8, 5, 5, 5, 4, 4};
        for (int presetIndex = 0; presetIndex < presets.length; presetIndex++) {
            EngineProfile profile = EngineProfile.forPreset(presets[presetIndex]);
            EngineTelemetry idle = new EngineTelemetry(profile.getIdleRpm(), 8_000,
                    0.0F, 0.07F, 0, 0, true, false, false, System.nanoTime());
            EngineVoice voice = new EngineVoice(profile, SAMPLE_RATE, idle);
            voice.setFluidSubsteps(fluidSubsteps[presetIndex]);
            voice.render(idle, CHUNK * 4);
            byte[] pcm = voice.render(idle, CHUNK * 12);

            double[] windows = windowMeanSquares(pcm, SAMPLE_RATE / 50);
            double[] sorted = windows.clone();
            Arrays.sort(sorted);
            double median = sorted[sorted.length / 2];
            double quietest = sorted[0];
            assertTrue(presets[presetIndex] + " idle must not contain 20 ms dropouts; quietest/median="
                            + quietest / median,
                    quietest > median * 0.18);
        }
    }

    @Test
    public void foregroundToBackgroundQualitySwitchStaysContinuous() {
        EngineProfile profile = EngineProfile.forPreset("V8_CROSSPLANE");
        EngineTelemetry running = telemetry(3_500, true, false, 0.72F, 0.80F);
        EngineVoice voice = new EngineVoice(profile, SAMPLE_RATE, running);
        voice.setFluidSubsteps(8);
        voice.render(running, CHUNK * 4);

        voice.setFluidSubsteps(4);
        byte[] pcm = voice.render(running, CHUNK * 12);
        double[] windows = windowMeanSquares(pcm, SAMPLE_RATE / 100);
        double[] sorted = windows.clone();
        Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];

        assertTrue("background quality transition must not introduce a dropout",
                windows[0] > median * 0.18);
        assertTrue("background quality transition must retain continuous output",
                sorted[0] > median * 0.15);
        assertTrue("background quality transition must not introduce clipping",
                clippedSamples(pcm) < pcm.length / 500);
    }

    @Test
    public void configuredAfterfireChangesExhaustOnlyAfterAHighRpmThrottleLift() {
        EngineProfile profile = EngineProfile.forPreset("I6");
        EngineTelemetry throttle = telemetry(4_200, true, false, 1.0F, 0.84F);
        EngineVoice standard = new EngineVoice(profile, SAMPLE_RATE, throttle);
        EngineVoice withAfterfire = new EngineVoice(profile, null, null, null,
                AfterfireProfile.forPreset("AGGRESSIVE"), SAMPLE_RATE, throttle);

        byte[] steadyAfterfireVoice = null;
        for (int chunk = 0; chunk < 4; chunk++) {
            byte[] standardChunk = standard.render(throttle, CHUNK);
            steadyAfterfireVoice = withAfterfire.render(throttle, CHUNK);
            assertTrue("afterfire configuration must not alter steady throttle",
                    Arrays.equals(standardChunk, steadyAfterfireVoice));
        }

        EngineTelemetry released = telemetry(4_150, true, false, 0.0F, 0.08F);
        long differenceEnergy = 0;
        byte[] releasedAfterfire = new byte[42 * CHUNK * 2];
        for (int chunk = 0; chunk < 42; chunk++) {
            byte[] standardChunk = standard.render(released, CHUNK);
            byte[] afterfireChunk = withAfterfire.render(released, CHUNK);
            differenceEnergy += absoluteDifferenceEnergy(
                    standardChunk, afterfireChunk);
            System.arraycopy(afterfireChunk, 0, releasedAfterfire,
                    chunk * afterfireChunk.length, afterfireChunk.length);
        }
        assertTrue("afterfire must create an audible exhaust transient after release",
                differenceEnergy > 1_000_000L);
        double steadyRms = medianWindowRms(steadyAfterfireVoice, SAMPLE_RATE / 200);
        double popRms = maximumWindowRms(releasedAfterfire, SAMPLE_RATE / 200);
        double pressurePeak = maximumAbsoluteSample(releasedAfterfire);
        assertTrue("aggressive afterfire must retain a sustained pressure body; ratio="
                        + popRms / steadyRms,
                popRms > steadyRms * 1.85);
        assertTrue("aggressive afterfire pressure peak should exceed four times the engine RMS; ratio="
                        + pressurePeak / steadyRms,
                pressurePeak > steadyRms * 4.0);
        assertEquals("loud afterfire must retain limiter headroom",
                0, clippedSamples(releasedAfterfire));
    }

    @Test
    public void configuredTireLayerIsTransparentUntilDynamXReportsSlip() {
        EngineProfile profile = EngineProfile.forPreset("I6");
        long time = 7_000_000_000L;
        EngineTelemetry grip = new EngineTelemetry(3_200, 7_000, 0.32F, 0.58F,
                62, 3, true, false, false, 0.0F, false, time);
        EngineVoice standard = new EngineVoice(profile, SAMPLE_RATE, grip);
        EngineVoice withTires = new EngineVoice(profile, null, null, null,
                null, TireSquealProfile.forPreset("PERFORMANCE_TIRE"),
                SAMPLE_RATE, grip);

        for (int chunk = 0; chunk < 4; chunk++) {
            assertTrue("an inactive tyre layer must leave the motor bit-identical",
                    Arrays.equals(standard.render(grip, CHUNK),
                            withTires.render(grip, CHUNK)));
        }

        EngineTelemetry sliding = new EngineTelemetry(3_200, 7_000, 0.32F, 0.58F,
                58, 3, true, false, false, 0.96F, false,
                time + 50_000_000L);
        long difference = 0;
        byte[] tireOutput = null;
        for (int chunk = 0; chunk < 8; chunk++) {
            byte[] engine = standard.render(sliding, CHUNK);
            tireOutput = withTires.render(sliding, CHUNK);
            difference += absoluteDifferenceEnergy(engine, tireOutput);
        }

        assertTrue("real wheel slip must add an audible tyre layer", difference > 1_000_000L);
        assertTrue("the tyre layer must retain mixer headroom",
                clippedSamples(tireOutput) < tireOutput.length / 100);
    }

    @Test
    public void inactiveHornAndSirenProfilesLeaveTheEngineBitIdentical() {
        EngineProfile profile = EngineProfile.forPreset("I6");
        EngineTelemetry running = telemetry(3_200, true, false, 0.42F, 0.58F);
        EngineVoice standard = new EngineVoice(profile, SAMPLE_RATE, running);
        EngineVoice withSignals = new EngineVoice(profile, null, null, null,
                null, null, HornProfile.forPreset("LUXURY_CAR"),
                SirenProfile.forPreset("DE_POLICE"), SAMPLE_RATE, running);

        for (int chunk = 0; chunk < 5; chunk++) {
            assertTrue("opt-in signal profiles must be transparent while controls are inactive",
                    Arrays.equals(standard.render(running, CHUNK),
                            withSignals.render(running, CHUNK)));
        }
    }

    @Test
    public void configuredSignalsRemainAudibleWithThePowertrainOff() {
        long time = 8_000_000_000L;
        EngineTelemetry stopped = new EngineTelemetry(0, 7_000, 0, 0,
                0, 0, false, false, false, 0,
                false, false, false, time);
        EngineVoice voice = new EngineVoice(EngineProfile.forPreset("ELECTRIC"),
                null, null, null, null, null,
                HornProfile.forPreset("TRUCK_AIR"), SirenProfile.forPreset("FR_FIRE"),
                SAMPLE_RATE, stopped);
        EngineTelemetry signaling = new EngineTelemetry(0, 7_000, 0, 0,
                0, 0, false, false, false, 0,
                true, true, false, time + 50_000_000L);

        byte[] pcm = voice.render(signaling, CHUNK * 8);

        assertTrue("horn and siren controls must not depend on the engine being on",
                absoluteEnergy(pcm) > 1_000_000L);
        assertTrue(voice.getActiveSignalDistance() >= 108.0F);
        assertFalse(voice.isSilent());
    }

    private static EngineTelemetry telemetry(float rpm, boolean on, boolean limiter, float throttle, float load) {
        return new EngineTelemetry(rpm, 7_000, throttle, load, 40, 3, on, limiter, false, System.nanoTime());
    }

    private static long absoluteEnergy(byte[] pcm) {
        long result = 0;
        for (int i = 0; i < pcm.length; i += 2) {
            short sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            result += Math.abs((int) sample);
        }
        return result;
    }

    private static int clippedSamples(byte[] pcm) {
        int result = 0;
        for (int i = 0; i < pcm.length; i += 2) {
            short sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            if (Math.abs((int) sample) >= 31_400) {
                result++;
            }
        }
        return result;
    }

    private static long absoluteDifferenceEnergy(byte[] left, byte[] right) {
        long result = 0;
        for (int offset = 0; offset < left.length; offset += 2) {
            short leftSample = (short) ((left[offset] & 0xFF) | (left[offset + 1] << 8));
            short rightSample = (short) ((right[offset] & 0xFF) | (right[offset + 1] << 8));
            result += Math.abs(leftSample - rightSample);
        }
        return result;
    }

    private static int maximumAbsoluteSample(byte[] pcm) {
        int maximum = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            maximum = Math.max(maximum, Math.abs((int) sample));
        }
        return maximum;
    }

    private static int zeroCrossings(byte[] pcm) {
        int result = 0;
        int previous = 0;
        for (int i = 0; i < pcm.length; i += 2) {
            short sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            if ((sample < 0 && previous >= 0) || (sample >= 0 && previous < 0)) {
                result++;
            }
            previous = sample;
        }
        return result;
    }

    private static double meanSquare(byte[] pcm) {
        double sum = 0.0;
        int samples = pcm.length / 2;
        for (int i = 0; i < pcm.length; i += 2) {
            double sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            sum += sample * sample;
        }
        return sum / samples;
    }

    private static double[] windowMeanSquares(byte[] pcm, int windowSamples) {
        int sampleCount = pcm.length / 2;
        int windowCount = sampleCount / windowSamples;
        double[] result = new double[windowCount];
        for (int window = 0; window < windowCount; window++) {
            double sum = 0.0;
            int firstSample = window * windowSamples;
            for (int sampleIndex = firstSample;
                 sampleIndex < firstSample + windowSamples; sampleIndex++) {
                int offset = sampleIndex * 2;
                double sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
                sum += sample * sample;
            }
            result[window] = sum / windowSamples;
        }
        return result;
    }

    private static double medianWindowRms(byte[] pcm, int windowSamples) {
        double[] windows = windowMeanSquares(pcm, windowSamples);
        Arrays.sort(windows);
        return Math.sqrt(windows[windows.length / 2]);
    }

    private static void assertCharacterDifference(String firstPreset, String secondPreset,
                                                  float rpm, float maximumRpm) {
        EngineTelemetry running = new EngineTelemetry(rpm, maximumRpm,
                0.68F, 0.78F, 48, 3, true, false, false, System.nanoTime());
        EngineVoice first = new EngineVoice(EngineProfile.forPreset(firstPreset), SAMPLE_RATE, running);
        EngineVoice second = new EngineVoice(EngineProfile.forPreset(secondPreset), SAMPLE_RATE, running);
        first.render(running, CHUNK * 4);
        second.render(running, CHUNK * 4);
        byte[] firstPcm = first.render(running, CHUNK * 8);
        byte[] secondPcm = second.render(running, CHUNK * 8);

        double difference = 0.0;
        double reference = 0.0;
        for (int offset = 0; offset < firstPcm.length; offset += 2) {
            short firstSample = (short) ((firstPcm[offset] & 0xFF) | (firstPcm[offset + 1] << 8));
            short secondSample = (short) ((secondPcm[offset] & 0xFF) | (secondPcm[offset + 1] << 8));
            difference += Math.abs(firstSample - secondSample);
            reference += Math.max(Math.abs(firstSample), Math.abs(secondSample));
        }
        double normalizedDifference = difference / Math.max(1.0, reference);
        assertTrue(firstPreset + " and " + secondPreset
                        + " must not collapse into the same rendered character; difference="
                        + normalizedDifference,
                normalizedDifference > 0.08);
    }

    private static double maximumWindowRms(byte[] pcm, int windowSamples) {
        double maximum = 0.0;
        for (double meanSquare : windowMeanSquares(pcm, windowSamples)) {
            maximum = Math.max(maximum, meanSquare);
        }
        return Math.sqrt(maximum);
    }

    private static double highPassMeanSquare(byte[] pcm, double sampleRate, double cutoff) {
        double alpha = 1.0 - Math.exp(-2.0 * Math.PI * cutoff / sampleRate);
        double lowPass = 0.0;
        double sum = 0.0;
        int samples = pcm.length / 2;
        for (int i = 0; i < pcm.length; i += 2) {
            double sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            lowPass += alpha * (sample - lowPass);
            double highPass = sample - lowPass;
            sum += highPass * highPass;
        }
        return sum / samples;
    }

    private static double lowPassMeanSquare(byte[] pcm, double sampleRate, double cutoff) {
        double alpha = 1.0 - Math.exp(-2.0 * Math.PI * cutoff / sampleRate);
        double lowPass1 = 0.0;
        double lowPass2 = 0.0;
        double sum = 0.0;
        int samples = pcm.length / 2;
        for (int i = 0; i < pcm.length; i += 2) {
            double sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            lowPass1 += alpha * (sample - lowPass1);
            lowPass2 += alpha * (lowPass1 - lowPass2);
            sum += lowPass2 * lowPass2;
        }
        return sum / samples;
    }

    private static double mean(byte[] pcm) {
        double sum = 0.0;
        int samples = pcm.length / 2;
        for (int i = 0; i < pcm.length; i += 2) {
            sum += (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
        }
        return sum / samples;
    }
}
