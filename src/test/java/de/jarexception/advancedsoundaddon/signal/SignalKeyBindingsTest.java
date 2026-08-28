package de.jarexception.advancedsoundaddon.signal;

import fr.dynamx.addons.basics.client.BasicsAddonController;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SignalKeyBindingsTest {
    @Test
    public void reusesBasicsBindingsWhenAddonIsPresent() {
        SignalKeyBindings.initialize();

        assertTrue(SignalKeyBindings.isUsingBasicsBindings());
        assertSame(BasicsAddonController.hornKey, SignalKeyBindings.horn());
        assertSame(BasicsAddonController.sirenKey, SignalKeyBindings.siren());
    }
}
