package de.jarexception.advancedsoundaddon.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import paulscode.sound.Library;
import paulscode.sound.Source;
import paulscode.sound.SoundSystem;

import java.lang.reflect.Field;

/** Resolves Minecraft's live Paulscode instance across mapped and obfuscated fields. */
final class MinecraftSoundSystemAccess {
    private static volatile Field soundManagerField;
    private static volatile Field soundSystemField;
    private static volatile Field soundLibraryField;

    private MinecraftSoundSystemAccess() {
    }

    static SoundSystem get() throws ReflectiveOperationException {
        SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
        if (handler == null) {
            return null;
        }
        Field managerField = soundManagerField;
        if (managerField == null) {
            managerField = findAssignableField(SoundHandler.class, SoundManager.class);
            soundManagerField = managerField;
        }
        SoundManager manager = (SoundManager) managerField.get(handler);
        if (manager == null) {
            return null;
        }

        Field systemField = soundSystemField;
        if (systemField == null) {
            systemField = findAssignableField(SoundManager.class, SoundSystem.class);
            soundSystemField = systemField;
        }
        return (SoundSystem) systemField.get(manager);
    }

    static String getLibraryClassName(SoundSystem system) throws ReflectiveOperationException {
        Library library = getLibrary(system);
        return library == null ? "initializing" : library.getClass().getName();
    }

    static Source getSource(SoundSystem system, String sourceName) throws ReflectiveOperationException {
        Library library = getLibrary(system);
        return library == null ? null : library.getSource(sourceName);
    }

    private static Library getLibrary(SoundSystem system) throws ReflectiveOperationException {
        if (system == null) {
            return null;
        }
        Field libraryField = soundLibraryField;
        if (libraryField == null) {
            libraryField = findAssignableField(SoundSystem.class, Library.class);
            soundLibraryField = libraryField;
        }
        return (Library) libraryField.get(system);
    }

    static boolean hasOpenAlBackend(SoundSystem system) throws ReflectiveOperationException {
        return "paulscode.sound.libraries.LibraryLWJGLOpenAL".equals(getLibraryClassName(system));
    }

    private static Field findAssignableField(Class<?> owner, Class<?> fieldBaseType)
            throws NoSuchFieldException {
        for (Field field : owner.getDeclaredFields()) {
            if (fieldBaseType.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("No " + fieldBaseType.getName() + " field in " + owner.getName());
    }
}
