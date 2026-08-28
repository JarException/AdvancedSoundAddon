package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CylinderFlowModelTest {
    private static final int SAMPLE_RATE = 48_000;

    @Test
    public void producesContinuousNonRepeatingValveAndRunnerFlow() {
        CylinderFlowModel model = new CylinderFlowModel(EngineLayout.I4, SAMPLE_RATE);
        for (int i = 0; i < 4_000; i++) {
            model.step(3_000, 0.75, 0.82, 1.0, false, false);
        }

        int samplesPerCycle = SAMPLE_RATE * 120 / 3_000;
        double[] firstCycle = renderCycle(model, samplesPerCycle);
        double[] secondCycle = renderCycle(model, samplesPerCycle);
        int changingSamples = 0;
        double cycleDifference = 0.0;
        for (int i = 1; i < firstCycle.length; i++) {
            if (Math.abs(firstCycle[i] - firstCycle[i - 1]) > 1.0E-8) {
                changingSamples++;
            }
            cycleDifference += Math.abs(firstCycle[i] - secondCycle[i]);
        }

        assertTrue("valve/runner pressure must be continuous rather than sparse impulses",
                changingSamples > firstCycle.length * 0.85);
        assertTrue("cycle-to-cycle combustion variation must prevent an exactly repeated waveform",
                cycleDifference > 0.01);
    }

    @Test
    public void compressionAndValveMotionExistDuringStarterCranking() {
        CylinderFlowModel model = new CylinderFlowModel(EngineLayout.I6, SAMPLE_RATE);
        double compression = 0.0;
        double valveActivity = 0.0;
        for (int i = 0; i < SAMPLE_RATE; i++) {
            model.step(210, 0.1, 0.25, 0.35, false, true);
            compression += Math.abs(model.getCompressionRipple());
            valveActivity += model.getValveActivity();
        }
        assertTrue(compression > 1.0);
        assertTrue(valveActivity > 1.0);
    }

    private static double[] renderCycle(CylinderFlowModel model, int samples) {
        double[] result = new double[samples];
        for (int i = 0; i < samples; i++) {
            model.step(3_000, 0.75, 0.82, 1.0, false, false);
            result[i] = model.getBankFlow(0);
        }
        return result;
    }
}
