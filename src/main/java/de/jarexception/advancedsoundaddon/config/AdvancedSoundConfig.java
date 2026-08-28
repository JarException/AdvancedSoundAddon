package de.jarexception.advancedsoundaddon.config;

import de.jarexception.advancedsoundaddon.AdvancedSoundAddon;
import net.minecraftforge.common.config.Config;

/** Forge-managed user settings that control optional global sound layers. */
@Config(modid = AdvancedSoundAddon.MOD_ID, name = AdvancedSoundAddon.MOD_ID)
public final class AdvancedSoundConfig {
    @Config.Name("enableTireSqueal")
    @Config.Comment({
            "Enables procedural tyre scrub and squeal for all wheeled vehicles.",
            "When false, both Advanced Sound Addon tyre synthesis and DynamX's skid loop are muted."
    })
    public static boolean enableTireSqueal = false;

    private AdvancedSoundConfig() {
    }
}
