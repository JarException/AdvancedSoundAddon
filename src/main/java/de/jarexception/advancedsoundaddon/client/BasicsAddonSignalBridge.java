package de.jarexception.advancedsoundaddon.client;

import fr.dynamx.common.entities.BaseVehicleEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional reflection bridge to BasicsAddon without making it a hard dependency. */
public final class BasicsAddonSignalBridge {
    private static final String MODULE_CLASS_NAME =
            "fr.dynamx.addons.basics.common.modules.BasicsAddonModule";
    private static final Class<?> MODULE_CLASS;
    private static final Method PLAY_KLAXON;
    private static final Method IS_SIREN_ON;
    private static final Method HAS_KLAXON;
    private static final Method HAS_SIREN;
    private static final Method GET_INFOS;
    private static final Field KLAXON_SOUND;

    static {
        Class<?> moduleClass = null;
        Method playKlaxon = null;
        Method isSirenOn = null;
        Method hasKlaxon = null;
        Method hasSiren = null;
        Method getInfos = null;
        Field klaxonSound = null;
        try {
            moduleClass = Class.forName(MODULE_CLASS_NAME, false,
                    BasicsAddonSignalBridge.class.getClassLoader());
            playKlaxon = moduleClass.getMethod("playKlaxon");
            isSirenOn = moduleClass.getMethod("isSirenOn");
            hasKlaxon = moduleClass.getMethod("hasKlaxon");
            hasSiren = moduleClass.getMethod("hasSiren");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            moduleClass = null;
            playKlaxon = null;
            isSirenOn = null;
            hasKlaxon = null;
            hasSiren = null;
        }
        if (moduleClass != null) {
            try {
                getInfos = moduleClass.getMethod("getInfos");
                klaxonSound = getInfos.getReturnType().getField("klaxonSound");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                getInfos = null;
                klaxonSound = null;
            }
        }
        MODULE_CLASS = moduleClass;
        PLAY_KLAXON = playKlaxon;
        IS_SIREN_ON = isSirenOn;
        HAS_KLAXON = hasKlaxon;
        HAS_SIREN = hasSiren;
        GET_INFOS = getInfos;
        KLAXON_SOUND = klaxonSound;
    }

    private BasicsAddonSignalBridge() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static SignalState read(BaseVehicleEntity<?> entity) {
        if (MODULE_CLASS == null || entity == null) return SignalState.INACTIVE;
        try {
            Object module = module(entity);
            if (module == null) return SignalState.INACTIVE;
            return new SignalState((Boolean) PLAY_KLAXON.invoke(module),
                    (Boolean) IS_SIREN_ON.invoke(module));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return SignalState.INACTIVE;
        }
    }

    public static boolean suppliesHorn(BaseVehicleEntity<?> entity) {
        return capability(entity, HAS_KLAXON);
    }

    public static boolean suppliesSiren(BaseVehicleEntity<?> entity) {
        return capability(entity, HAS_SIREN);
    }

    static boolean matchesLegacyHornSound(BaseVehicleEntity<?> entity, String soundName) {
        if (MODULE_CLASS == null || GET_INFOS == null || KLAXON_SOUND == null
                || entity == null || soundName == null) return false;
        try {
            Object module = module(entity);
            if (module == null) return false;
            Object infos = GET_INFOS.invoke(module);
            return infos != null && soundName.equals(KLAXON_SOUND.get(infos));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object module(BaseVehicleEntity<?> entity) {
        return entity.getModuleByType((Class) MODULE_CLASS);
    }

    private static boolean capability(BaseVehicleEntity<?> entity, Method method) {
        if (MODULE_CLASS == null || method == null || entity == null) return false;
        try {
            Object module = module(entity);
            return module != null && Boolean.TRUE.equals(method.invoke(module));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    static final class SignalState {
        static final SignalState INACTIVE = new SignalState(false, false);

        final boolean hornActive;
        final boolean sirenActive;

        SignalState(boolean hornActive, boolean sirenActive) {
            this.hornActive = hornActive;
            this.sirenActive = sirenActive;
        }
    }
}
