package de.jarexception.advancedsoundaddon.config;

import net.minecraftforge.common.config.Config;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class AdvancedSoundConfigTest {
    @Test
    public void tireSquealIsAnExplicitForgeSettingAndDefaultsToDisabled() throws Exception {
        Config annotation = AdvancedSoundConfig.class.getAnnotation(Config.class);

        assertNotNull(annotation);
        assertEquals("advancedsoundaddon", annotation.modid());
        assertFalse(AdvancedSoundConfig.enableTireSqueal);
        assertNotNull(AdvancedSoundConfig.class.getField("enableTireSqueal")
                .getAnnotation(Config.Comment.class));
    }
}
