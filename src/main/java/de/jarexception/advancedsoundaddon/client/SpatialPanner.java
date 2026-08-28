package de.jarexception.advancedsoundaddon.client;

/** Converts Minecraft coordinates into stereo pan values. */
final class SpatialPanner {
    private SpatialPanner() {
    }

    static float pan(double dx, double dz, float playerYawDegrees) {
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        if (horizontalDistance < 0.001) {
            return 0.0F;
        }

        double yaw = Math.toRadians(playerYawDegrees);
        double rightX = -Math.cos(yaw);
        double rightZ = -Math.sin(yaw);
        return (float) clamp((dx * rightX + dz * rightZ) / horizontalDistance, -1.0, 1.0);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
