package de.jarexception.advancedsoundaddon.mixin;

import de.jarexception.advancedsoundaddon.client.AdvancedSoundRuntime;
import fr.dynamx.common.entities.modules.engines.BasicEngineModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BasicEngineModule.class, remap = false)
public abstract class MixinBasicEngineModule {
    @Inject(method = "updateSounds()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void advancedsoundaddon$suppressSampledEngineLoops(CallbackInfo callback) {
        if (AdvancedSoundRuntime.isReplacementAvailable()) {
            callback.cancel();
        }
    }

    @Inject(method = "playStartingSound()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void advancedsoundaddon$suppressSampledStarter(CallbackInfo callback) {
        if (AdvancedSoundRuntime.isReplacementAvailable()) {
            callback.cancel();
        }
    }
}
