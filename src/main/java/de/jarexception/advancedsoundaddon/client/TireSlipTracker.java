package de.jarexception.advancedsoundaddon.client;

/** Rejects DynamX's zero-filled pre-sync wheel array, then tracks real skid severity. */
final class TireSlipTracker {
    private boolean synchronizedOnce;

    float update(float[] skidInfos) {
        if (skidInfos == null || skidInfos.length == 0) {
            return 0.0F;
        }
        for (float skidInfo : skidInfos) {
            if (Float.isFinite(skidInfo) && skidInfo > 0.001F) {
                synchronizedOnce = true;
                break;
            }
        }
        if (!synchronizedOnce) {
            return 0.0F;
        }
        return aggregate(skidInfos);
    }

    static float aggregate(float[] skidInfos) {
        if (skidInfos == null || skidInfos.length == 0) {
            return 0.0F;
        }
        double strongest = 0.0;
        double sum = 0.0;
        int valid = 0;
        for (float skidInfo : skidInfos) {
            if (!Float.isFinite(skidInfo)) {
                continue;
            }
            double severity = 1.0 - clamp(skidInfo, 0.0, 1.0);
            strongest = Math.max(strongest, severity);
            sum += severity;
            valid++;
        }
        if (valid == 0) {
            return 0.0F;
        }
        double average = sum / valid;
        return (float) clamp(strongest * 0.88 + average * 0.12, 0.0, 1.0);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
