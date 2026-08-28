package de.jarexception.advancedsoundaddon.signal;

import de.jarexception.advancedsoundaddon.client.BasicsAddonSignalBridge;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.common.entities.BaseVehicleEntity;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;

/** Driver controls for the native signal module. */
final class VehicleSignalController implements InvocationHandler {
    private final BaseVehicleEntity<?> entity;
    private final VehicleSignalModule module;

    VehicleSignalController(BaseVehicleEntity<?> entity, VehicleSignalModule module) {
        this.entity = entity;
        this.module = module;
    }

    IVehicleController asVehicleController() {
        return (IVehicleController) Proxy.newProxyInstance(
                IVehicleController.class.getClassLoader(),
                new Class<?>[]{IVehicleController.class}, this);
    }

    private void update() {
        boolean legacyHorn = BasicsAddonSignalBridge.suppliesHorn(entity);
        boolean legacySiren = BasicsAddonSignalBridge.suppliesSiren(entity);

        if (module.hasHorn()) {
            module.setHornActive(!legacyHorn && SignalKeyBindings.horn().isKeyDown());
        }
        if (module.hasSiren()) {
            if (legacySiren) {
                module.setSirenActive(false);
            } else if (SignalKeyBindings.siren().isPressed()) {
                module.setSirenActive(!module.isSirenActive());
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "update":
                update();
                return null;
            case "getHudCssStyles":
                return Collections.emptyList();
            case "createHud":
                return null;
            case "toString":
                return "VehicleSignalController{" + entity.getInfoName() + '}';
            case "hashCode":
                return System.identityHashCode(proxy);
            case "equals":
                return proxy == (args == null ? null : args[0]);
            default:
                throw new UnsupportedOperationException(method.toString());
        }
    }
}
