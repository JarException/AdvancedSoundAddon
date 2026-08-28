package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EngineSimGasSystemTest {
    @Test
    public void compressionPerformsWorkAndRaisesPressure() {
        EngineSimGasSystem gas = new EngineSimGasSystem();
        gas.initialize(EngineSimGasSystem.ATMOSPHERE, 0.0005,
                EngineSimGasSystem.ROOM_TEMPERATURE, EngineSimGasSystem.Mix.AIR);
        gas.setVolume(0.00025);
        assertTrue(gas.pressure() > EngineSimGasSystem.ATMOSPHERE * 1.8);
        assertTrue(gas.temperature() > EngineSimGasSystem.ROOM_TEMPERATURE);
    }

    @Test
    public void compressibleFlowMovesMolesTowardLowerPressure() {
        EngineSimGasSystem high = new EngineSimGasSystem();
        EngineSimGasSystem low = new EngineSimGasSystem();
        high.initialize(EngineSimGasSystem.ATMOSPHERE * 2.0, 0.001,
                600.0, EngineSimGasSystem.Mix.EXHAUST);
        low.initialize(EngineSimGasSystem.ATMOSPHERE, 0.001,
                EngineSimGasSystem.ROOM_TEMPERATURE, EngineSimGasSystem.Mix.AIR);
        high.setGeometry(0.25, 0.04);
        low.setGeometry(0.25, 0.04);
        double before = high.moles() + low.moles();
        double flow = high.flowTo(low, EngineSimGasSystem.kCarb(200.0),
                1.0 / 80_000.0, 0.0012, 0.0012);
        assertTrue(flow > 0.0);
        assertEquals(before, high.moles() + low.moles(), before * 1.0E-10);
        assertTrue(Double.isFinite(high.dynamicPressure(1.0)));
        assertTrue(Double.isFinite(low.dynamicPressure(1.0)));
    }
}
