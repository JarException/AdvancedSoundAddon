package de.jarexception.advancedsoundaddon.client;

import fr.dynamx.api.events.EventPhase;
import fr.dynamx.api.events.VehicleEntityEvent;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class AdvancedSoundEventHandler {
    private final ProceduralAudioManager manager;

    public AdvancedSoundEventHandler(ProceduralAudioManager manager) {
        this.manager = manager;
    }

    @SubscribeEvent
    public void onVehicleSounds(VehicleEntityEvent.UpdateSounds event) {
        if (event.getEventPhase() == EventPhase.PRE && event.getEntity() instanceof BaseVehicleEntity) {
            manager.observe((BaseVehicleEntity<?>) event.getEntity(), event.getModule());
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            manager.tick();
        }
    }

    @SubscribeEvent
    public void onSoundLoad(SoundLoadEvent event) {
        manager.onSoundReload();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            manager.clear();
        }
    }
}
