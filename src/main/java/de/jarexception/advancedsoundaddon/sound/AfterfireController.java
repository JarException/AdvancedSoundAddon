package de.jarexception.advancedsoundaddon.sound;

/** Schedules procedural exhaust ignitions after high-RPM throttle lift. */
final class AfterfireController {
    private final AfterfireProfile profile;

    private float previousThrottle;
    private double remainingSeconds;
    private double nextEventSeconds;
    private int randomState = 0x13579BDF;

    AfterfireController(AfterfireProfile profile, EngineTelemetry initialTelemetry) {
        this.profile = profile;
        previousThrottle = initialTelemetry.throttle;
    }

    void observe(EngineTelemetry telemetry, double currentRpm) {
        float throttle = Math.max(0.0F, Math.min(1.0F, telemetry.throttle));
        boolean throttleReleased = previousThrottle >= 0.55F && throttle <= 0.08F;
        previousThrottle = throttle;

        if (!telemetry.engineOn || throttle > 0.12F) {
            cancel();
            return;
        }
        double rpmFraction = currentRpm / Math.max(500.0, telemetry.maxRpm);
        if (throttleReleased && rpmFraction >= profile.getMinimumRpmFraction()) {
            remainingSeconds = profile.getDurationSeconds() * (0.86 + randomUnit() * 0.28);
            nextEventSeconds = profile.getInitialDelaySeconds() * (0.72 + randomUnit() * 0.56);
        }
    }

    double step(double currentRpm, double maximumRpm, double dt) {
        if (remainingSeconds <= 0.0) {
            return 0.0;
        }
        double rpmFraction = currentRpm / Math.max(500.0, maximumRpm);
        if (rpmFraction < profile.getMinimumRpmFraction() * 0.72) {
            cancel();
            return 0.0;
        }

        remainingSeconds = Math.max(0.0, remainingSeconds - dt);
        nextEventSeconds -= dt;
        if (nextEventSeconds > 0.0 || remainingSeconds <= 0.0) {
            return 0.0;
        }

        double baseInterval = 1.0 / profile.getEventsPerSecond();
        nextEventSeconds = baseInterval * (0.55 + randomUnit() * 0.90);
        double tailStrength = 0.46 + 0.54 * Dsp.clamp(
                remainingSeconds / profile.getDurationSeconds(), 0.0, 1.0);
        double rpmStrength = 0.72 + 0.28 * Dsp.smoothStep(
                (rpmFraction - profile.getMinimumRpmFraction())
                        / Math.max(0.01, 0.82 - profile.getMinimumRpmFraction()));
        double irregularity = 0.82 + randomUnit() * 0.36;
        return profile.getEventEnergy() * tailStrength * rpmStrength * irregularity;
    }

    void synchronize(EngineTelemetry telemetry) {
        previousThrottle = telemetry.throttle;
        cancel();
    }

    private void cancel() {
        remainingSeconds = 0.0;
        nextEventSeconds = 0.0;
    }

    private double randomUnit() {
        int x = randomState;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        randomState = x;
        return (x & 0x7FFFFFFF) / (double) Integer.MAX_VALUE;
    }
}
