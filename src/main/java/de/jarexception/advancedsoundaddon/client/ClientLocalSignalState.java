package de.jarexception.advancedsoundaddon.client;

final class ClientLocalSignalState {
    private boolean hornActive;
    private boolean sirenActive;

    void update(boolean controllingVehicle, boolean hornAvailable, boolean sirenAvailable,
                boolean hornDown, boolean sirenPressed) {
        hornActive = controllingVehicle && hornAvailable && hornDown;
        if (!sirenAvailable) {
            sirenActive = false;
        } else if (controllingVehicle && sirenPressed) {
            sirenActive = !sirenActive;
        }
    }

    boolean isHornActive() {
        return hornActive;
    }

    boolean isSirenActive() {
        return sirenActive;
    }
}
