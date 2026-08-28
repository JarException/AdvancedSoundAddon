package fr.dynamx.addons.basics.client;

import net.minecraft.client.settings.KeyBinding;

/** Minimal optional-addon fixture used to verify reflection compatibility. */
public final class BasicsAddonController {
    public static final KeyBinding hornKey = new KeyBinding("Basics horn", 37, "DynamX basics");
    public static final KeyBinding sirenKey = new KeyBinding("Basics siren", 23, "DynamX basics");

    private BasicsAddonController() {
    }
}
