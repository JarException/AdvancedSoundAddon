package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AfterfireControllerTest {
    private static final double DT = 1.0 / 48_000.0;

    @Test
    public void highRpmThrottleLiftProducesIrregularOverrunEvents() {
        EngineTelemetry throttle = telemetry(4_200, 1.0F, true);
        AfterfireController controller = new AfterfireController(
                AfterfireProfile.forPreset("AGGRESSIVE"), throttle);

        controller.observe(telemetry(4_200, 0.0F, true), 4_200);
        int events = countEvents(controller, 4_200, 1.2);

        assertTrue("aggressive overrun should create a short crackle cluster", events >= 4);
        assertTrue("overrun must finish instead of becoming a loop", events <= 10);
        assertEquals(0.0, controller.step(4_200, 7_000, DT), 0.0);
    }

    @Test
    public void lowRpmLiftDoesNotPop() {
        EngineTelemetry throttle = telemetry(1_100, 1.0F, true);
        AfterfireController controller = new AfterfireController(
                AfterfireProfile.forPreset("SPORT"), throttle);

        controller.observe(telemetry(1_100, 0.0F, true), 1_100);

        assertEquals(0, countEvents(controller, 1_100, 1.0));
    }

    @Test
    public void reappliedThrottleCancelsTheRemainingBurst() {
        EngineTelemetry throttle = telemetry(4_800, 1.0F, true);
        AfterfireController controller = new AfterfireController(
                AfterfireProfile.forPreset("AGGRESSIVE"), throttle);
        controller.observe(telemetry(4_800, 0.0F, true), 4_800);
        countEvents(controller, 4_800, 0.15);

        controller.observe(telemetry(4_800, 1.0F, true), 4_800);

        assertEquals(0, countEvents(controller, 4_800, 1.2));
    }

    @Test
    public void stoppedEngineCannotTriggerAfterfire() {
        EngineTelemetry throttle = telemetry(4_200, 1.0F, true);
        AfterfireController controller = new AfterfireController(
                AfterfireProfile.forPreset("RACE"), throttle);

        controller.observe(telemetry(4_200, 0.0F, false), 4_200);

        assertEquals(0, countEvents(controller, 4_200, 0.8));
    }

    private static int countEvents(AfterfireController controller, double rpm, double seconds) {
        int events = 0;
        int samples = (int) Math.round(seconds / DT);
        for (int sample = 0; sample < samples; sample++) {
            if (controller.step(rpm, 7_000, DT) > 0.0) {
                events++;
            }
        }
        return events;
    }

    private static EngineTelemetry telemetry(float rpm, float throttle, boolean engineOn) {
        return new EngineTelemetry(rpm, 7_000, throttle, throttle > 0 ? 0.82F : 0.08F,
                60, 4, engineOn, false, false, System.nanoTime());
    }
}
