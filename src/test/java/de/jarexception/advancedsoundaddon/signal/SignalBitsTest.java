package de.jarexception.advancedsoundaddon.signal;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SignalBitsTest {
    @Test
    public void hornAndSirenCanBeChangedIndependently() {
        int state = SignalBits.set(0, SignalBits.HORN, true);
        assertTrue(SignalBits.isSet(state, SignalBits.HORN));
        assertFalse(SignalBits.isSet(state, SignalBits.SIREN));

        state = SignalBits.set(state, SignalBits.SIREN, true);
        state = SignalBits.set(state, SignalBits.HORN, false);
        assertFalse(SignalBits.isSet(state, SignalBits.HORN));
        assertTrue(SignalBits.isSet(state, SignalBits.SIREN));
    }
}
