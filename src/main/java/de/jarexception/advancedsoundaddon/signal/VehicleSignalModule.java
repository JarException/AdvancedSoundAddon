package de.jarexception.advancedsoundaddon.signal;

import fr.dynamx.api.entities.modules.IPhysicsModule;
import fr.dynamx.api.entities.modules.IVehicleController;
import fr.dynamx.api.network.sync.EntityVariable;
import fr.dynamx.api.network.sync.SynchronizationRules;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.physics.entities.AbstractEntityPhysicsHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

/** Synchronizes procedural horn and siren state between vehicle clients. */
public final class VehicleSignalModule
        implements IPhysicsModule<AbstractEntityPhysicsHandler<?, ?>>, IEntityAdditionalSpawnData {
    private static final String NBT_KEY = "AdvancedSoundSignals";

    private final boolean hornAvailable;
    private final boolean sirenAvailable;
    private final EntityVariable<Integer> state =
            new EntityVariable<>(SynchronizationRules.CONTROLS_TO_SPECTATORS, 0);
    private final IVehicleController controller;

    public VehicleSignalModule(BaseVehicleEntity<?> entity, boolean hornAvailable, boolean sirenAvailable) {
        this.hornAvailable = hornAvailable;
        this.sirenAvailable = sirenAvailable;
        this.controller = entity.world.isRemote
                ? new VehicleSignalController(entity, this).asVehicleController() : null;
    }

    public boolean hasHorn() {
        return hornAvailable;
    }

    public boolean hasSiren() {
        return sirenAvailable;
    }

    public boolean isHornActive() {
        return hornAvailable && SignalBits.isSet(state.get(), SignalBits.HORN);
    }

    public void setHornActive(boolean active) {
        state.set(SignalBits.set(state.get(), SignalBits.HORN, hornAvailable && active));
    }

    public boolean isSirenActive() {
        return sirenAvailable && SignalBits.isSet(state.get(), SignalBits.SIREN);
    }

    public void setSirenActive(boolean active) {
        state.set(SignalBits.set(state.get(), SignalBits.SIREN, sirenAvailable && active));
    }

    @Override
    public IVehicleController createNewController() {
        return controller;
    }

    @Override
    public void removePassenger(Entity passenger) {
        setHornActive(false);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger(NBT_KEY, sirenAvailable ? state.get() & SignalBits.SIREN : 0);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        state.set(sirenAvailable ? tag.getInteger(NBT_KEY) & SignalBits.SIREN : 0);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeByte(state.get() & availableBits());
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        state.set(additionalData.readUnsignedByte() & availableBits());
    }

    private int availableBits() {
        return (hornAvailable ? SignalBits.HORN : 0)
                | (sirenAvailable ? SignalBits.SIREN : 0);
    }
}
