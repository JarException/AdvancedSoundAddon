package de.jarexception.advancedsoundaddon.client;

/** Fixed parameters for realtime synthesis and streaming. */
public final class AdvancedSoundSettings {
    public static final int SAMPLE_RATE = 48_000;
    public static final int CHUNK_SAMPLES = 1_024;
    public static final int MAX_VOICES = 8;
    public static final float AUDIBLE_DISTANCE = 56.0F;
    public static final float ENGINE_OUTPUT_GAIN = 3.0F;
    public static final int OUTPUT_BUFFER_CHUNKS = 24;
    public static final int OUTPUT_TARGET_CHUNKS = 10;
    public static final int BACKGROUND_OUTPUT_TARGET_CHUNKS = 20;

    private AdvancedSoundSettings() {
    }
}
