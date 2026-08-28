package de.jarexception.advancedsoundaddon.signal;

/** Compact synchronized state shared by native horn and siren controls. */
final class SignalBits {
    static final int HORN = 1;
    static final int SIREN = 2;

    private SignalBits() {
    }

    static boolean isSet(int state, int bit) {
        return (state & bit) != 0;
    }

    static int set(int state, int bit, boolean enabled) {
        return enabled ? state | bit : state & ~bit;
    }
}
