package de.jarexception.advancedsoundaddon.client;

/** Keeps high-cylinder-count voices inside the same realtime budget as a V8. */
final class SynthesisQualityPolicy {
    private static final int FLUID_WORK_BUDGET = 64;

    private SynthesisQualityPolicy() {
    }

    static int fluidSubsteps(int distanceRank, int cylinderCount) {
        return fluidSubsteps(distanceRank, cylinderCount, true);
    }

    static int fluidSubsteps(int distanceRank, int cylinderCount, boolean foreground) {
        if (!foreground) {
            return 4;
        }
        int distanceQuality = distanceRank < 2 ? 8 : (distanceRank < 5 ? 6 : 4);
        if (cylinderCount <= 0) {
            return distanceQuality;
        }
        int cylinderQuality = Math.max(4, Math.min(8, FLUID_WORK_BUDGET / cylinderCount));
        return Math.min(distanceQuality, cylinderQuality);
    }
}
