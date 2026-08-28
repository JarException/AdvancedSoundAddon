package de.jarexception.advancedsoundaddon.sound;

/** Optional overrun-combustion character selected by a vehicle definition. */
public final class AfterfireProfile {
    private final String presetName;
    private final float minimumRpmFraction;
    private final float durationSeconds;
    private final float initialDelaySeconds;
    private final float eventsPerSecond;
    private final float eventEnergy;

    private AfterfireProfile(String presetName, float minimumRpmFraction,
                             float durationSeconds, float initialDelaySeconds,
                             float eventsPerSecond, float eventEnergy) {
        this.presetName = presetName;
        this.minimumRpmFraction = minimumRpmFraction;
        this.durationSeconds = durationSeconds;
        this.initialDelaySeconds = initialDelaySeconds;
        this.eventsPerSecond = eventsPerSecond;
        this.eventEnergy = eventEnergy;
    }

    public static AfterfireProfile forPreset(String presetName) {
        switch (presetName) {
            case "SPORT":
                return new AfterfireProfile("SPORT", 0.28F,
                        0.62F, 0.055F, 3.5F, 0.76F);
            case "AGGRESSIVE":
                return new AfterfireProfile("AGGRESSIVE", 0.22F,
                        1.02F, 0.032F, 7.2F, 1.04F);
            case "RACE":
                return new AfterfireProfile("RACE", 0.36F,
                        0.48F, 0.022F, 4.6F, 1.34F);
            default:
                throw new IllegalArgumentException("Unknown AfterfirePreset " + presetName);
        }
    }

    public String getPresetName() { return presetName; }
    public float getMinimumRpmFraction() { return minimumRpmFraction; }
    public float getDurationSeconds() { return durationSeconds; }
    public float getInitialDelaySeconds() { return initialDelaySeconds; }
    public float getEventsPerSecond() { return eventsPerSecond; }
    public float getEventEnergy() { return eventEnergy; }
}
