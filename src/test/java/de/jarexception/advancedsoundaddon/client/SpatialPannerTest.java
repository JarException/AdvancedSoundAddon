package de.jarexception.advancedsoundaddon.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpatialPannerTest {
    @Test
    public void yawZeroFacesSouthAndPansWestToTheRight() {
        assertEquals(1.0F, SpatialPanner.pan(-5.0, 0.0, 0.0F), 0.0001F);
        assertEquals(-1.0F, SpatialPanner.pan(5.0, 0.0, 0.0F), 0.0001F);
    }

    @Test
    public void yawNinetyFacesWestAndPansNorthToTheRight() {
        assertEquals(1.0F, SpatialPanner.pan(0.0, -5.0, 90.0F), 0.0001F);
        assertEquals(-1.0F, SpatialPanner.pan(0.0, 5.0, 90.0F), 0.0001F);
    }

    @Test
    public void sourceAtListenerStaysCentered() {
        assertEquals(0.0F, SpatialPanner.pan(0.0, 0.0, 217.0F), 0.0001F);
    }
}
