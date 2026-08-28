package de.jarexception.advancedsoundaddon;

import de.jarexception.advancedsoundaddon.proxy.CommonProxy;
import fr.dynamx.api.contentpack.DynamXAddon;
import fr.dynamx.common.entities.BaseVehicleEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

@Mod(
        modid = AdvancedSoundAddon.MOD_ID,
        name = AdvancedSoundAddon.NAME,
        version = AdvancedSoundAddon.VERSION,
        clientSideOnly = false,
        acceptableRemoteVersions = "*",
        dependencies = "required-before:dynamxmod"
)
@DynamXAddon(
        modid = AdvancedSoundAddon.MOD_ID,
        name = AdvancedSoundAddon.NAME,
        version = AdvancedSoundAddon.VERSION,
        requiredOnClient = true
)
public final class AdvancedSoundAddon {
    public static final String MOD_ID = "advancedsoundaddon";
    public static final String NAME = "Advanced Sound Addon";
    public static final String VERSION = "1.0.0";

    private static volatile CommonProxy proxy;

    @DynamXAddon.AddonEventSubscriber
    public static void onDynamXAddonInitialized() {
        getProxy().onDynamXReady();
    }

    public static boolean shouldAttachSignalModule(BaseVehicleEntity<?> entity) {
        return getProxy().shouldAttachSignalModule(entity);
    }

    private static CommonProxy getProxy() {
        CommonProxy current = proxy;
        if (current != null) {
            return current;
        }
        synchronized (AdvancedSoundAddon.class) {
            if (proxy == null) {
                proxy = FMLLaunchHandler.side().isClient() ? createClientProxy() : new CommonProxy();
            }
            return proxy;
        }
    }

    private static CommonProxy createClientProxy() {
        try {
            return (CommonProxy) Class.forName("de.jarexception.advancedsoundaddon.proxy.ClientProxy").newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot initialize AdvancedSoundAddon client proxy", exception);
        }
    }
}
