package de.jarexception.advancedsoundaddon.proxy;

import de.jarexception.advancedsoundaddon.client.AdvancedSoundEventHandler;
import de.jarexception.advancedsoundaddon.client.AdvancedSoundSettings;
import de.jarexception.advancedsoundaddon.client.ProceduralAudioManager;
import de.jarexception.advancedsoundaddon.config.AdvancedSoundConfig;
import de.jarexception.advancedsoundaddon.signal.SignalKeyBindings;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ClientProxy extends CommonProxy {
    private static final Logger LOGGER = LogManager.getLogger("AdvancedSoundAddon");

    private boolean initialized;
    private ProceduralAudioManager manager;

    @Override
    public synchronized void onDynamXReady() {
        if (initialized) {
            return;
        }
        initialized = true;
        SignalKeyBindings.initialize();
        manager = new ProceduralAudioManager();
        MinecraftForge.EVENT_BUS.register(new AdvancedSoundEventHandler(manager));
        LOGGER.info("Procedural DynamX engine audio initialized: {} Hz PCM, gain={}, up to {} requested synth voices, "
                        + "signal controls={}, tyre squeal={}; "
                        + "DynamX samples remain enabled until a real OpenAL backend is verified",
                AdvancedSoundSettings.SAMPLE_RATE, AdvancedSoundSettings.ENGINE_OUTPUT_GAIN,
                AdvancedSoundSettings.MAX_VOICES,
                SignalKeyBindings.isUsingBasicsBindings() ? "BasicsAddon keybindings" : "native keybindings",
                AdvancedSoundConfig.enableTireSqueal ? "enabled" : "disabled");
    }

    @Override
    public boolean shouldAttachSignalModule(BaseVehicleEntity<?> entity) {
        if (entity == null || !entity.world.isRemote) {
            return true;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.isSingleplayer()) {
            return true;
        }
        try {
            if (minecraft.getConnection() == null) {
                return false;
            }
            NetworkDispatcher dispatcher = NetworkDispatcher.get(
                    minecraft.getConnection().getNetworkManager());
            return dispatcher != null && dispatcher.getModList() != null
                    && dispatcher.getModList().containsKey("advancedsoundaddon");
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
