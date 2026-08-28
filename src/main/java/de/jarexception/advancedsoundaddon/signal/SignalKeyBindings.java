package de.jarexception.advancedsoundaddon.signal;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;

/** Independent controls used when BasicsAddon does not provide the feature. */
public final class SignalKeyBindings {
    private static final String BASICS_CONTROLLER =
            "fr.dynamx.addons.basics.client.BasicsAddonController";
    private static final String CATEGORY = "key.categories.advancedsoundaddon";

    private static KeyBinding horn;
    private static KeyBinding siren;
    private static boolean initialized;
    private static boolean usingBasicsBindings;

    private SignalKeyBindings() {
    }

    public static synchronized void initialize() {
        if (initialized) return;
        initialized = true;

        KeyBinding[] basicsBindings = findBasicsBindings();
        if (basicsBindings != null) {
            horn = basicsBindings[0];
            siren = basicsBindings[1];
            usingBasicsBindings = true;
            return;
        }

        horn = new KeyBinding("key.advancedsoundaddon.horn", Keyboard.KEY_K, CATEGORY);
        siren = new KeyBinding("key.advancedsoundaddon.siren", Keyboard.KEY_I, CATEGORY);
        ClientRegistry.registerKeyBinding(horn);
        ClientRegistry.registerKeyBinding(siren);
    }

    public static KeyBinding horn() {
        ensureInitialized();
        return horn;
    }

    public static KeyBinding siren() {
        ensureInitialized();
        return siren;
    }

    public static boolean isUsingBasicsBindings() {
        ensureInitialized();
        return usingBasicsBindings;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    private static KeyBinding[] findBasicsBindings() {
        try {
            Class<?> controller = Class.forName(BASICS_CONTROLLER, true,
                    SignalKeyBindings.class.getClassLoader());
            Field hornField = controller.getField("hornKey");
            Field sirenField = controller.getField("sirenKey");
            Object basicsHorn = hornField.get(null);
            Object basicsSiren = sirenField.get(null);
            if (basicsHorn instanceof KeyBinding && basicsSiren instanceof KeyBinding) {
                return new KeyBinding[]{(KeyBinding) basicsHorn, (KeyBinding) basicsSiren};
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
        }
        return null;
    }
}
