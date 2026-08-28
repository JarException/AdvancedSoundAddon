package de.jarexception.advancedsoundaddon.mixin;

import de.jarexception.advancedsoundaddon.client.AdvancedSoundRuntime;
import de.jarexception.advancedsoundaddon.config.AdvancedSoundConfig;
import de.jarexception.advancedsoundaddon.contentpack.EngineProfileResolver;
import fr.dynamx.client.sound.DynamXSoundHandler;
import fr.dynamx.client.sound.SkiddingSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Silences DynamX's looping skid sample only when a procedural replacement exists. */
@Mixin(value = SkiddingSound.class, remap = false)
public abstract class MixinSkiddingSound {
    @Inject(method = "update(Lfr/dynamx/client/sound/DynamXSoundHandler;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void advancedsoundaddon$suppressSampledSkidLoop(
            DynamXSoundHandler soundHandler, CallbackInfo callback) {
        if (!AdvancedSoundConfig.enableTireSqueal) {
            ((SkiddingSound) (Object) this).setVolumeFactor(0.0F);
            callback.cancel();
            return;
        }
        if (AdvancedSoundRuntime.isReplacementAvailable()
                && EngineProfileResolver.resolveTireSqueal(
                ((VehicleSoundAccessor) this).advancedsoundaddon$getVehicleEntity()) != null) {
            ((SkiddingSound) (Object) this).setVolumeFactor(0.0F);
            callback.cancel();
        }
    }
}
