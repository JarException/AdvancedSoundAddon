package de.jarexception.advancedsoundaddon.client;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClientLocalSignalStateTest {
    @Test
    public void hornFollowsKeyOnlyWhileDriving() {
        ClientLocalSignalState state = new ClientLocalSignalState();

        state.update(true, true, false, true, false);
        assertTrue(state.isHornActive());

        state.update(false, true, false, true, false);
        assertFalse(state.isHornActive());
    }

    @Test
    public void sirenTogglesAndPersists() {
        ClientLocalSignalState state = new ClientLocalSignalState();

        state.update(true, false, true, false, true);
        assertTrue(state.isSirenActive());

        state.update(false, false, true, false, false);
        assertTrue(state.isSirenActive());

        state.update(true, false, true, false, true);
        assertFalse(state.isSirenActive());
    }

    @Test
    public void unavailableSirenIsReset() {
        ClientLocalSignalState state = new ClientLocalSignalState();
        state.update(true, false, true, false, true);

        state.update(true, false, false, false, false);

        assertFalse(state.isSirenActive());
    }
}
