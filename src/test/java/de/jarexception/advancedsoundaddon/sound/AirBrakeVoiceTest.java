package de.jarexception.advancedsoundaddon.sound;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AirBrakeVoiceTest {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    @Test
    public void truckAndBusPresetsHaveDistinctPneumaticCharacters() {
        AirBrakeProfile truck = AirBrakeProfile.forPreset("TRUCK_AIR_BRAKE");
        AirBrakeProfile bus = AirBrakeProfile.forPreset("BUS_AIR_BRAKE");

        assertEquals("TRUCK_AIR_BRAKE", truck.getPresetName());
        assertEquals("BUS_AIR_BRAKE", bus.getPresetName());
        assertTrue(truck.getBodyResonanceHz() < bus.getBodyResonanceHz());
        assertTrue(truck.getNozzleCutoffHz() < bus.getNozzleCutoffHz());
    }

    @Test
    public void reachingAStopCreatesASyntheticPressureRelease() {
        long time = 1_000_000_000L;
        AirBrakeVoice voice = new AirBrakeVoice(AirBrakeProfile.forPreset("TRUCK_AIR_BRAKE"),
                SAMPLE_RATE, telemetry(34, false, 0, time));

        EngineTelemetry stopped = telemetry(0.4F, true, 0, time + 50_000_000L);
        byte[] pcm = voice.render(stopped, CHUNK * 4);

        assertTrue("stopping must produce an audible generated air release", energy(pcm) > 2_000_000L);
        assertTrue("the generated event must have an active tail", !voice.isSilent());
    }

    @Test
    public void releasingTheBrakeAtStandstillCreatesTheShortTschhh() {
        long time = 2_000_000_000L;
        AirBrakeVoice voice = new AirBrakeVoice(AirBrakeProfile.forPreset("BUS_AIR_BRAKE"),
                SAMPLE_RATE, telemetry(0, true, 0, time));

        byte[] pcm = voice.render(telemetry(0, false, 0, time + 500_000_000L), CHUNK * 3);

        assertTrue("brake release must produce an audible generated hiss", energy(pcm) > 1_000_000L);
    }

    @Test
    public void releasingServiceBrakeWhileStillMovingDoesNotRepeatTheFullHiss() {
        long time = 2_500_000_000L;
        AirBrakeVoice voice = new AirBrakeVoice(AirBrakeProfile.forPreset("TRUCK_AIR_BRAKE"),
                SAMPLE_RATE, telemetry(28, true, 0, time));

        byte[] firstRelease = voice.render(telemetry(24, false, 0, time + 500_000_000L), CHUNK);
        voice.render(telemetry(21, true, 0, time + 1_000_000_000L), CHUNK);
        byte[] secondRelease = voice.render(telemetry(18, false, 0,
                time + 1_500_000_000L), CHUNK);

        assertEquals("brake taps during the approach must stay silent", 0L,
                energy(firstRelease) + energy(secondRelease));
        assertTrue(voice.isSilent());
    }

    @Test
    public void gradualDepartureTriggersWithoutAnyHandbrakeTransition() {
        long time = 2_800_000_000L;
        AirBrakeVoice voice = new AirBrakeVoice(AirBrakeProfile.forPreset("BUS_AIR_BRAKE"),
                SAMPLE_RATE, telemetry(0, false, 0, time));

        voice.render(telemetry(0.35F, false, 0, time + 250_000_000L), 256);
        voice.render(telemetry(0.68F, false, 0, time + 500_000_000L), 256);
        byte[] pcm = voice.render(telemetry(0.92F, false, 0,
                time + 750_000_000L), CHUNK * 3);

        assertTrue("moving away must release air without a handbrake transition",
                energy(pcm) > 1_000_000L);
    }

    @Test
    public void brakeReleaseAtStandstillIsNotDuplicatedWhenVehicleMovesAway() {
        long time = 2_900_000_000L;
        AirBrakeVoice voice = new AirBrakeVoice(AirBrakeProfile.forPreset("TRUCK_AIR_BRAKE"),
                SAMPLE_RATE, telemetry(0, true, 0, time));

        EngineTelemetry released = telemetry(0, false, 0, time + 500_000_000L);
        voice.render(released, CHUNK);
        for (int chunk = 0; chunk < 120; chunk++) {
            voice.render(released, CHUNK);
        }
        voice.render(telemetry(0, false, 0, time + 3_000_000_000L), 256);
        byte[] departure = voice.render(telemetry(0.95F, false, 0,
                time + 3_500_000_000L), CHUNK * 3);

        assertTrue("a release already heard at standstill must not play again on departure",
                energy(departure) < 10_000L);
    }

    @Test
    public void chargedStationaryVehicleCanPurgeItsReservoir() {
        long time = 3_000_000_000L;
        AirBrakeVoice voice = new AirBrakeVoice(AirBrakeProfile.forPreset("TRUCK_AIR_BRAKE"),
                SAMPLE_RATE, telemetry(0, false, 0, time));
        long energy = 0;
        for (int tick = 1; tick <= 34; tick++) {
            byte[] pcm = voice.render(telemetry(0, false, 0,
                    time + tick * 500_000_000L), 256);
            energy += energy(pcm);
        }

        assertTrue("compressor governor must eventually purge while idling", energy > 500_000L);
    }

    private static EngineTelemetry telemetry(float speed, boolean brakeApplied,
                                             float throttle, long timestamp) {
        return new EngineTelemetry(760, 2_500, throttle, 0.18F, speed, 1,
                true, false, brakeApplied, false, timestamp);
    }

    private static long energy(byte[] pcm) {
        long energy = 0;
        for (int offset = 0; offset < pcm.length; offset += 2) {
            short sample = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            energy += Math.abs((int) sample);
        }
        return energy;
    }
}
