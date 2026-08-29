package de.jarexception.advancedsoundaddon.mixin;

import com.jme3.math.Vector3f;
import de.jarexception.advancedsoundaddon.client.AdvancedSoundRuntime;
import fr.dynamx.api.audio.IDynamXSound;
import fr.dynamx.client.sound.DynamXSoundHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Filters replaced horn and siren samples at DynamX's sound boundary. */
@Mixin(value = DynamXSoundHandler.class, remap = false)
public abstract class MixinDynamXSoundHandler {
    @Inject(method = "playSingleSound(Lcom/jme3/math/Vector3f;Ljava/lang/String;FFIF)V",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void advancedsoundaddon$replaceConfiguredHorn(
            Vector3f position, String soundName, float volume, float pitch,
            int attenuationType, float distance, CallbackInfo callback) {
        if (AdvancedSoundRuntime.shouldSuppressLegacyHorn(position, soundName)) {
            callback.cancel();
        }
    }

    @Redirect(method = {
            "playStreamingSound(Lcom/jme3/math/Vector3f;Lfr/dynamx/api/audio/IDynamXSound;IF)V",
            "setMasterVolume(F)V"
    }, at = @At(value = "INVOKE",
            target = "Lfr/dynamx/client/sound/DynamXSoundHandler;setSoundVolume(Lfr/dynamx/api/audio/IDynamXSound;F)V"),
            require = 0, remap = false)
    private void advancedsoundaddon$muteConfiguredSiren(
            DynamXSoundHandler handler, IDynamXSound sound, float volume) {
        handler.setSoundVolume(sound,
                shouldMuteLegacyStream(sound) ? 0.0F : volume);
    }

    @Inject(method = "setSoundVolume(Lfr/dynamx/api/audio/IDynamXSound;F)V",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void advancedsoundaddon$keepReplacedStreamMuted(
            IDynamXSound sound, float volume, CallbackInfo callback) {
        if (!shouldMuteLegacyStream(sound)) return;
        DynamXSoundHandler handler = (DynamXSoundHandler) (Object) this;
        if (handler.getMcSoundSystem() != null) {
            handler.getMcSoundSystem().setVolume(sound.getSoundUniqueName(), 0.0F);
        }
        callback.cancel();
    }

    private static boolean shouldMuteLegacyStream(IDynamXSound sound) {
        return AdvancedSoundRuntime.shouldSuppressLegacySiren(sound)
                || AdvancedSoundRuntime.shouldSuppressLegacyIndicator(sound);
    }
}
