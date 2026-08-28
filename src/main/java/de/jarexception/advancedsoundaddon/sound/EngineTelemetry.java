package de.jarexception.advancedsoundaddon.sound;

public final class EngineTelemetry {
    public final float rpm;
    public final float maxRpm;
    public final float throttle;
    public final float load;
    public final float speedKmh;
    public final int gear;
    public final boolean engineOn;
    public final boolean revLimiter;
    public final boolean brakeApplied;
    public final float tireSlip;
    public final boolean hornActive;
    public final boolean sirenActive;
    public final boolean interior;
    public final long timestampNanos;

    public EngineTelemetry(float rpm, float maxRpm, float throttle, float load,
                           float speedKmh, int gear, boolean engineOn,
                           boolean revLimiter, boolean interior, long timestampNanos) {
        this(rpm, maxRpm, throttle, load, speedKmh, gear, engineOn,
                revLimiter, false, 0.0F, interior, timestampNanos);
    }

    public EngineTelemetry(float rpm, float maxRpm, float throttle, float load,
                           float speedKmh, int gear, boolean engineOn,
                           boolean revLimiter, boolean brakeApplied, boolean interior,
                           long timestampNanos) {
        this(rpm, maxRpm, throttle, load, speedKmh, gear, engineOn,
                revLimiter, brakeApplied, 0.0F, interior, timestampNanos);
    }

    public EngineTelemetry(float rpm, float maxRpm, float throttle, float load,
                           float speedKmh, int gear, boolean engineOn,
                           boolean revLimiter, boolean brakeApplied, float tireSlip,
                           boolean interior, long timestampNanos) {
        this(rpm, maxRpm, throttle, load, speedKmh, gear, engineOn, revLimiter,
                brakeApplied, tireSlip, false, false, interior, timestampNanos);
    }

    public EngineTelemetry(float rpm, float maxRpm, float throttle, float load,
                           float speedKmh, int gear, boolean engineOn,
                           boolean revLimiter, boolean brakeApplied, float tireSlip,
                           boolean hornActive, boolean sirenActive,
                           boolean interior, long timestampNanos) {
        this.rpm = clamp(rpm, 0.0F, Math.max(1.0F, maxRpm * 1.1F));
        this.maxRpm = Math.max(1.0F, maxRpm);
        this.throttle = clamp(throttle, 0.0F, 1.0F);
        this.load = clamp(load, 0.0F, 1.0F);
        this.speedKmh = speedKmh;
        this.gear = gear;
        this.engineOn = engineOn;
        this.revLimiter = revLimiter;
        this.brakeApplied = brakeApplied;
        this.tireSlip = clamp(tireSlip, 0.0F, 1.0F);
        this.hornActive = hornActive;
        this.sirenActive = sirenActive;
        this.interior = interior;
        this.timestampNanos = timestampNanos;
    }

    public static EngineTelemetry stopped(float maxRpm) {
        return new EngineTelemetry(0, maxRpm, 0, 0, 0, 0, false, false, false, System.nanoTime());
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
