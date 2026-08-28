package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

public class DspTest {
    @Test
    public void fftConvolverMatchesDirectFirAcrossBlockBoundaries() {
        double[] impulse = {0.75, -0.25, 0.125, 0.0625};
        double[] input = new double[2_300];
        for (int i = 0; i < input.length; i++) {
            input[i] = Math.sin(i * 0.071) + Math.cos(i * 0.013) * 0.25;
        }

        double[] expected = directConvolution(input, impulse);
        double[] actual = new Dsp.FftConvolver(impulse).process(input);
        assertArrayEquals(expected, actual, 1.0E-9);
    }

    @Test
    public void engineSimImpulseResponseDecodesAndResamples() {
        double[] response = EngineSimImpulseResponse.mildExhaust(48_000);
        org.junit.Assert.assertEquals(2_060, response.length);
        org.junit.Assert.assertTrue(maximumAbsolute(response) > 0.009);
        org.junit.Assert.assertTrue(maximumAbsolute(response) <= 0.011);

        double[] sportResponse = EngineSimSportImpulseResponse.create(48_000);
        org.junit.Assert.assertEquals(6_508, sportResponse.length);
        org.junit.Assert.assertTrue(maximumAbsolute(sportResponse) > 0.009);
        org.junit.Assert.assertTrue(maximumAbsolute(sportResponse) <= 0.011);

        double[] openI4 = EngineSimOpenImpulseResponse.create(48_000);
        org.junit.Assert.assertEquals(2_201, openI4.length);
        org.junit.Assert.assertTrue(maximumAbsolute(openI4) > 0.009);

        double[] smoothV8 = EngineSimImpulseResponses.forLayout(EngineLayout.V8_CROSSPLANE, 48_000);
        org.junit.Assert.assertEquals(7_518, smoothV8.length);
        org.junit.Assert.assertTrue(maximumAbsolute(smoothV8) > 0.0009);

        double[] clearV10 = EngineSimImpulseResponses.forLayout(EngineLayout.V10, 48_000);
        org.junit.Assert.assertEquals(6_508, clearV10.length);
        org.junit.Assert.assertTrue(maximumAbsolute(clearV10) > 0.009);
    }

    @Test
    public void dieselProfilesUsePresenceRichRumbleControlledExhaustResponse() {
        double[] petrolI4 = EngineSimImpulseResponses.forProfile(
                EngineProfile.forLayout(EngineLayout.I4), 48_000);
        double[] dieselI4 = EngineSimImpulseResponses.forProfile(
                EngineProfile.forPreset("I4_DIESEL"), 48_000);
        double[] dieselI6 = EngineSimImpulseResponses.forProfile(
                EngineProfile.forPreset("I6_DIESEL"), 48_000);

        org.junit.Assert.assertFalse(Arrays.equals(petrolI4, dieselI4));
        org.junit.Assert.assertArrayEquals(dieselI4, dieselI6, 0.0);
        org.junit.Assert.assertEquals(6_508, dieselI4.length);
        org.junit.Assert.assertTrue(EngineProfile.forPreset("I4_DIESEL").isCompressionIgnition());
        org.junit.Assert.assertFalse(EngineProfile.forLayout(EngineLayout.I4).isCompressionIgnition());
    }

    @Test
    public void roadTripleUsesAProductionMufflerInsteadOfTheMotorcycleKernel() {
        double[] road = EngineSimImpulseResponses.forProfile(
                EngineProfile.forPreset("I3_ROAD"), 48_000);
        double[] sport = EngineSimImpulseResponses.forProfile(
                EngineProfile.forPreset("I3_SPORT"), 48_000);

        org.junit.Assert.assertFalse("road and sport triples require distinct exhaust responses",
                Arrays.equals(road, sport));
    }

    @Test
    public void luxuryAndRaceVariantsUseDifferentExhaustSystems() {
        double[] luxuryV12 = EngineSimImpulseResponses.forProfile(
                EngineProfile.forPreset("V12_LUXURY"), 48_000);
        double[] raceV12 = EngineSimImpulseResponses.forProfile(
                EngineProfile.forPreset("V12_RACE"), 48_000);
        double[] luxuryV8 = EngineSimImpulseResponses.forProfile(
                EngineProfile.forPreset("V8_LUXURY_TURBO"), 48_000);
        double[] muscleV8 = EngineSimImpulseResponses.forProfile(
                EngineProfile.forPreset("V8_MUSCLE"), 48_000);

        org.junit.Assert.assertFalse(Arrays.equals(luxuryV12, raceV12));
        org.junit.Assert.assertFalse(Arrays.equals(luxuryV8, muscleV8));
        org.junit.Assert.assertEquals(2_060, luxuryV12.length);
        org.junit.Assert.assertEquals(6_508, raceV12.length);
    }

    @Test
    public void partitionedConvolverMatchesDirectFirForLongResponse() {
        double[] impulse = new double[1_173];
        for (int i = 0; i < impulse.length; i++) {
            impulse[i] = Math.sin(i * 0.037) * Math.exp(-i / 310.0);
        }
        double[] input = new double[2_304];
        for (int i = 0; i < input.length; i++) {
            input[i] = Math.sin(i * 0.071) + Math.cos(i * 0.013) * 0.25;
        }
        assertArrayEquals(directConvolution(input, impulse),
                new Dsp.PartitionedConvolver(impulse).process(input), 1.0E-8);
    }

    private static double[] directConvolution(double[] input, double[] impulse) {
        double[] result = new double[input.length];
        for (int sample = 0; sample < input.length; sample++) {
            for (int tap = 0; tap < impulse.length && tap <= sample; tap++) {
                result[sample] += input[sample - tap] * impulse[tap];
            }
        }
        return result;
    }

    private static double maximumAbsolute(double[] samples) {
        double maximum = 0.0;
        for (double sample : samples) {
            maximum = Math.max(maximum, Math.abs(sample));
        }
        return maximum;
    }
}
