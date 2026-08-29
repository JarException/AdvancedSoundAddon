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
    private static final Method HAS_TURN_SIGNALS;
    private static final Method IS_TURN_SIGNAL_LEFT_ON;
    private static final Method IS_TURN_SIGNAL_RIGHT_ON;
    private static final Method GET_INFOS;
    private static final Field KLAXON_SOUND;
    private static final Field INDICATORS_SOUND;

    static {
        Class<?> moduleClass = null;
        Method playKlaxon = null;
        Method isSirenOn = null;
        Method hasKlaxon = null;
        Method hasSiren = null;
        Method hasTurnSignals = null;
        Method isTurnSignalLeftOn = null;
        Method isTurnSignalRightOn = null;
        Method getInfos = null;
        Field klaxonSound = null;
        Field indicatorsSound = null;
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
                hasTurnSignals = moduleClass.getMethod("hasTurnSignals");
                isTurnSignalLeftOn = moduleClass.getMethod("isTurnSignalLeftOn");
                isTurnSignalRightOn = moduleClass.getMethod("isTurnSignalRightOn");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                hasTurnSignals = null;
                isTurnSignalLeftOn = null;
                isTurnSignalRightOn = null;
            }
            try {
                getInfos = moduleClass.getMethod("getInfos");
            } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                getInfos = null;
            }
            if (getInfos != null) {
                try {
                    klaxonSound = getInfos.getReturnType().getField("klaxonSound");
                } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                    klaxonSound = null;
                }
                try {
                    indicatorsSound = getInfos.getReturnType().getField("indicatorsSound");
                } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
                    indicatorsSound = null;
                }
            } else {
                klaxonSound = null;
                indicatorsSound = null;
            }
        }
        MODULE_CLASS = moduleClass;
        PLAY_KLAXON = playKlaxon;
        IS_SIREN_ON = isSirenOn;
        HAS_KLAXON = hasKlaxon;
        HAS_SIREN = hasSiren;
        HAS_TURN_SIGNALS = hasTurnSignals;
        IS_TURN_SIGNAL_LEFT_ON = isTurnSignalLeftOn;
        IS_TURN_SIGNAL_RIGHT_ON = isTurnSignalRightOn;
        GET_INFOS = getInfos;
        KLAXON_SOUND = klaxonSound;
        INDICATORS_SOUND = indicatorsSound;
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
                    (Boolean) IS_SIREN_ON.invoke(module),
                    invokeBoolean(module, IS_TURN_SIGNAL_LEFT_ON),
                    invokeBoolean(module, IS_TURN_SIGNAL_RIGHT_ON));
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

    public static boolean suppliesIndicators(BaseVehicleEntity<?> entity) {
        return capability(entity, HAS_TURN_SIGNALS);
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

    static boolean matchesLegacyIndicatorSound(BaseVehicleEntity<?> entity, String soundName) {
        return matchesInfoSound(entity, soundName, INDICATORS_SOUND);
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

    private static boolean invokeBoolean(Object target, Method method) {
        if (target == null || method == null) return false;
        try {
            return Boolean.TRUE.equals(method.invoke(target));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean matchesInfoSound(BaseVehicleEntity<?> entity, String soundName,
                                            Field soundField) {
        if (MODULE_CLASS == null || GET_INFOS == null || soundField == null
                || entity == null || soundName == null) return false;
        try {
            Object module = module(entity);
            if (module == null) return false;
            Object infos = GET_INFOS.invoke(module);
            return infos != null && soundName.equals(soundField.get(infos));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    static final class SignalState {
        static final SignalState INACTIVE = new SignalState(false, false, false, false);

        final boolean hornActive;
        final boolean sirenActive;
        final boolean indicatorLeftActive;
        final boolean indicatorRightActive;

        SignalState(boolean hornActive, boolean sirenActive,
                    boolean indicatorLeftActive, boolean indicatorRightActive) {
            this.hornActive = hornActive;
            this.sirenActive = sirenActive;
            this.indicatorLeftActive = indicatorLeftActive;
            this.indicatorRightActive = indicatorRightActive;
        }
    }
}
