package de.jarexception.advancedsoundaddon.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TireSlipTelemetryTest {
    @Test
    public void fullGripProducesNoSlip() {
        assertEquals(0.0F, TireSlipTracker.aggregate(
                new float[]{1.0F, 1.0F, 1.0F, 1.0F}), 0.0001F);
    }

    @Test
    public void oneSlidingWheelDominatesWithoutIgnoringTheOthers() {
        float severity = TireSlipTracker.aggregate(
                new float[]{0.05F, 0.82F, 0.88F, 0.91F});

        assertTrue(severity > 0.72F);
        assertTrue(severity < 0.90F);
    }

    @Test
    public void zeroFilledPrePhysicsArrayDoesNotSquealWhileParked() {
        TireSlipTracker tracker = new TireSlipTracker();
        assertEquals(0.0F, tracker.update(
                new float[]{0.0F, 0.0F, 0.0F, 0.0F}), 0.0001F);
    }

    @Test
    public void fullyLockedWheelsRemainValidAfterFirstPhysicsSync() {
        TireSlipTracker tracker = new TireSlipTracker();
        tracker.update(new float[]{1.0F, 1.0F, 1.0F, 1.0F});

        assertEquals(1.0F, tracker.update(
                new float[]{0.0F, 0.0F, 0.0F, 0.0F}), 0.0001F);
    }
}
