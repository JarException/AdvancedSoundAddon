package de.jarexception.advancedsoundaddon.client;

/** Applies DynamX's native linear 0..1 sound-volume setting. */
final class DynamXVolumeScaler {
    private DynamXVolumeScaler() {
    }

    static float apply(float sourceGain, float dynamXVolume) {
        float clampedVolume = Math.max(0.0F, Math.min(1.0F, dynamXVolume));
        return Math.max(0.0F, sourceGain) * clampedVolume;
    }
}
