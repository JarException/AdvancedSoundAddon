package de.jarexception.advancedsoundaddon.proxy;

import fr.dynamx.common.entities.BaseVehicleEntity;

public class CommonProxy {
    public void onDynamXReady() {
    }

    public boolean shouldAttachSignalModule(BaseVehicleEntity<?> entity) {
        return true;
    }
}
