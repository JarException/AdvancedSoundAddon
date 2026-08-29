package de.jarexception.advancedsoundaddon.client;

import com.jme3.math.Vector3f;
import de.jarexception.advancedsoundaddon.contentpack.EngineProfileResolver;
import fr.dynamx.api.audio.IDynamXSound;
import fr.dynamx.client.sound.VehicleSound;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

/** Coordinates legacy-sound suppression with procedural backend availability. */
public final class AdvancedSoundRuntime {
    private static volatile boolean replacementAvailable;

    private AdvancedSoundRuntime() {
    }

    public static boolean isReplacementAvailable() {
        return replacementAvailable;
    }

    static void setReplacementAvailable(boolean available) {
        replacementAvailable = available;
    }

    public static boolean shouldSuppressLegacyHorn(Vector3f position, String soundName) {
        if (!replacementAvailable || position == null || soundName == null) return false;
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.world == null) return false;
            for (Entity candidate : minecraft.world.loadedEntityList) {
                if (!(candidate instanceof BaseVehicleEntity)) continue;
                double dx = candidate.posX - position.x;
                double dy = candidate.posY - position.y;
                double dz = candidate.posZ - position.z;
                if (dx * dx + dy * dy + dz * dz > 0.0625) continue;
                BaseVehicleEntity<?> vehicle = (BaseVehicleEntity<?>) candidate;
                if (EngineProfileResolver.resolveHorn(vehicle) != null
                        && BasicsAddonSignalBridge.matchesLegacyHornSound(vehicle, soundName)) {
                    return true;
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
        return false;
    }

    public static boolean shouldSuppressLegacySiren(IDynamXSound sound) {
        if (!replacementAvailable || sound == null
                || !"fr.dynamx.addons.basics.client.SirenSound"
                .equals(sound.getClass().getName())) {
            return false;
        }
        try {
            String uniqueName = sound.getSoundUniqueName();
            int separator = uniqueName == null ? -1 : uniqueName.indexOf('_');
            if (separator <= 0) return false;
            int entityId = Integer.parseInt(uniqueName.substring(0, separator));
            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.world == null) return false;
            Entity entity = minecraft.world.getEntityByID(entityId);
            return entity instanceof BaseVehicleEntity
                    && EngineProfileResolver.resolveSiren((BaseVehicleEntity<?>) entity) != null;
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean shouldSuppressLegacyIndicator(IDynamXSound sound) {
        if (!replacementAvailable || !(sound instanceof VehicleSound)) {
            return false;
        }
        try {
            BaseVehicleEntity<?> vehicle = vehicleForSound(sound);
            return vehicle != null
                    && BasicsAddonSignalBridge.suppliesIndicators(vehicle)
                    && EngineProfileResolver.resolveIndicator(vehicle) != null
                    && BasicsAddonSignalBridge.matchesLegacyIndicatorSound(
                    vehicle, ((VehicleSound) sound).getSoundName());
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static BaseVehicleEntity<?> vehicleForSound(IDynamXSound sound) {
        String uniqueName = sound.getSoundUniqueName();
        int separator = uniqueName == null ? -1 : uniqueName.indexOf('_');
        if (separator <= 0) return null;
        int entityId = Integer.parseInt(uniqueName.substring(0, separator));
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.world == null) return null;
        Entity entity = minecraft.world.getEntityByID(entityId);
        return entity instanceof BaseVehicleEntity ? (BaseVehicleEntity<?>) entity : null;
    }
}
