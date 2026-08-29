package de.jarexception.advancedsoundaddon.client;

import de.jarexception.advancedsoundaddon.contentpack.EngineProfileResolver;
import de.jarexception.advancedsoundaddon.config.AdvancedSoundConfig;
import de.jarexception.advancedsoundaddon.sound.AirBrakeProfile;
import de.jarexception.advancedsoundaddon.sound.AfterfireProfile;
import de.jarexception.advancedsoundaddon.sound.BrakeSquealProfile;
import de.jarexception.advancedsoundaddon.sound.EngineProfile;
import de.jarexception.advancedsoundaddon.sound.EngineTelemetry;
import de.jarexception.advancedsoundaddon.sound.EngineVoice;
import de.jarexception.advancedsoundaddon.sound.RotorProfile;
import de.jarexception.advancedsoundaddon.sound.ReverseWarningProfile;
import de.jarexception.advancedsoundaddon.sound.TireSquealProfile;
import de.jarexception.advancedsoundaddon.sound.HornProfile;
import de.jarexception.advancedsoundaddon.sound.IndicatorProfile;
import de.jarexception.advancedsoundaddon.sound.SirenProfile;
import de.jarexception.advancedsoundaddon.signal.SignalKeyBindings;
import de.jarexception.advancedsoundaddon.signal.VehicleSignalModule;
import fr.dynamx.api.entities.VehicleEntityProperties;
import fr.dynamx.client.ClientProxy;
import fr.dynamx.common.contentpack.type.vehicle.BaseEngineInfo;
import fr.dynamx.common.contentpack.parts.BasePartSeat;
import fr.dynamx.common.contentpack.parts.PartEntitySeat;
import fr.dynamx.common.entities.BaseVehicleEntity;
import fr.dynamx.common.entities.modules.SeatsModule;
import fr.dynamx.common.entities.modules.WheelsModule;
import fr.dynamx.common.entities.modules.engines.BasicEngineModule;
import fr.dynamx.common.entities.modules.engines.HelicopterEngineModule;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.Display;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystem;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProceduralAudioManager {
    private static final Logger LOGGER = LogManager.getLogger("AdvancedSoundAddon/Audio");
    private static final AudioFormat OUTPUT_FORMAT = new AudioFormat(
            AdvancedSoundSettings.SAMPLE_RATE, 16, 2, true, false);
    private static final int OUTPUT_CHUNK_BYTES = AdvancedSoundSettings.CHUNK_SAMPLES
            * OUTPUT_FORMAT.getFrameSize();
    private static final int MAX_REFILL_CHUNKS_PER_PUMP = 4;

    private final ConcurrentHashMap<UUID, VehicleVoice> voices = new ConcurrentHashMap<>();
    private final ScheduledExecutorService audioExecutor;
    private final ExecutorService synthesisExecutor;
    private final Object outputLock = new Object();
    private final List<VehicleVoice> activeVoices = new ArrayList<>(AdvancedSoundSettings.MAX_VOICES);
    private final List<Future<RenderedVoice>> pendingRenders = new ArrayList<>(AdvancedSoundSettings.MAX_VOICES);
    private final double[] leftMix = new double[AdvancedSoundSettings.CHUNK_SAMPLES];
    private final double[] rightMix = new double[AdvancedSoundSettings.CHUNK_SAMPLES];
    private final byte[] stereoMix = new byte[OUTPUT_CHUNK_BYTES];
    private volatile SoundSystem soundSystem;
    private volatile SourceDataLine pcmOutput;
    private volatile long outputFrames;
    private volatile boolean outputStarved;
    private volatile float outputMasterGain = 1.0F;
    private volatile boolean streamingDisabled;
    private volatile boolean closed;
    private volatile boolean windowFocused = true;
    private boolean tireSquealGloballyEnabled = AdvancedSoundConfig.enableTireSqueal;
    private boolean soundReflectionFailureLogged;
    private String rejectedBackendName;
    private long clientTick;

    public ProceduralAudioManager() {
        this.audioExecutor = Executors.newSingleThreadScheduledExecutor(new AudioThreadFactory());
        int synthesisThreads = Math.max(1, Math.min(AdvancedSoundSettings.MAX_VOICES, Math.min(8,
                Runtime.getRuntime().availableProcessors() - 1)));
        this.synthesisExecutor = Executors.newFixedThreadPool(synthesisThreads,
                new SynthesisThreadFactory());
        LOGGER.debug("Procedural powertrain/rotor synthesis workers={}", synthesisThreads);
        this.audioExecutor.scheduleAtFixedRate(this::pumpAudioSafely, 0, 12, TimeUnit.MILLISECONDS);
    }

    public void observe(BaseVehicleEntity<?> entity, BasicEngineModule module) {
        if (closed || entity == null || module == null) {
            return;
        }
        BaseEngineInfo engineInfo = module.getEngineInfo();
        if (engineInfo == null) {
            return;
        }

        float engineMaxRpm = Math.max(500.0F,
                finiteOr(engineInfo.getMaxRevs(), 7_000.0F));
        float normalizedRpm = finiteOr(module.getEngineProperty(VehicleEntityProperties.EnumEngineProperties.REVS), 0.0F);
        if (module instanceof HelicopterEngineModule) {
            normalizedRpm = finiteOr(((HelicopterEngineModule) module).getPower(), normalizedRpm);
        }
        float speed = finiteOr(module.getEngineProperty(VehicleEntityProperties.EnumEngineProperties.SPEED), 0.0F);
        int gear = Math.round(finiteOr(module.getEngineProperty(VehicleEntityProperties.EnumEngineProperties.ACTIVE_GEAR), 0.0F));
        boolean engineOn = module.isEngineStarted();
        boolean reverseGear = gear < 0;
        boolean brakeApplied = module.isHandBraking()
                || (speed > 0.05F && ((!reverseGear && module.isReversing())
                || (reverseGear && module.isAccelerating())));
        float throttle = engineOn && (module.isAccelerating() || module.isReversing()) ? 1.0F : 0.0F;
        if (engineOn && module instanceof HelicopterEngineModule) {
            throttle = clamp(((HelicopterEngineModule) module).getPower(), 0.0F, 1.0F);
        }

        String engineName = engineInfo.getFullName();
        String vehicleName = entity.getInfoName();
        UUID id = entity.getUniqueID();
        VehicleVoice voice = voices.get(id);
        if (voice != null && !voice.engineName.equals(engineName)) {
            removeVoice(id, voice);
            voice = null;
        }

        EngineProfile profile = voice == null
                ? EngineProfileResolver.resolve(entity, engineInfo)
                : voice.synthesizer.getProfile();
        RotorProfile rotorProfile = voice == null
                ? EngineProfileResolver.resolveRotor(entity) : null;
        AirBrakeProfile airBrakeProfile = voice == null
                ? EngineProfileResolver.resolveAirBrake(entity) : null;
        ReverseWarningProfile reverseWarningProfile = voice == null
                ? EngineProfileResolver.resolveReverseWarning(entity) : null;
        BrakeSquealProfile brakeSquealProfile = voice == null
                ? EngineProfileResolver.resolveBrakeSqueal(entity) : null;
        AfterfireProfile afterfireProfile = voice == null
                ? EngineProfileResolver.resolveAfterfire(entity) : null;
        TireSquealProfile tireSquealProfile = voice == null && tireSquealGloballyEnabled
                ? EngineProfileResolver.resolveTireSqueal(entity) : null;
        HornProfile hornProfile = voice == null
                ? EngineProfileResolver.resolveHorn(entity) : null;
        SirenProfile sirenProfile = voice == null
                ? EngineProfileResolver.resolveSiren(entity) : null;
        boolean indicatorsAvailable = voice == null
                ? BasicsAddonSignalBridge.suppliesIndicators(entity) : voice.indicatorEnabled;
        IndicatorProfile indicatorProfile = voice == null && indicatorsAvailable
                ? EngineProfileResolver.resolveIndicator(entity) : null;
        boolean observeSignals = voice == null
                ? hornProfile != null || sirenProfile != null || indicatorProfile != null
                : voice.hornEnabled || voice.sirenEnabled || voice.indicatorEnabled;
        BasicsAddonSignalBridge.SignalState signalState = observeSignals
                ? BasicsAddonSignalBridge.read(entity)
                : BasicsAddonSignalBridge.SignalState.INACTIVE;
        ClientLocalSignalState localSignals = voice == null
                ? new ClientLocalSignalState() : voice.localSignals;
        if (observeSignals) {
            VehicleSignalModule nativeSignals = entity.getModuleByType(VehicleSignalModule.class);
            if (nativeSignals != null) {
                boolean hornActive = BasicsAddonSignalBridge.suppliesHorn(entity)
                        ? signalState.hornActive : nativeSignals.isHornActive();
                boolean sirenActive = BasicsAddonSignalBridge.suppliesSiren(entity)
                        ? signalState.sirenActive : nativeSignals.isSirenActive();
                signalState = new BasicsAddonSignalBridge.SignalState(hornActive, sirenActive,
                        signalState.indicatorLeftActive, signalState.indicatorRightActive);
            } else {
                boolean basicsHorn = BasicsAddonSignalBridge.suppliesHorn(entity);
                boolean basicsSiren = BasicsAddonSignalBridge.suppliesSiren(entity);
                boolean controllingVehicle = isLocalDriver(entity);
                localSignals.update(controllingVehicle,
                        (voice == null ? hornProfile != null : voice.hornEnabled) && !basicsHorn,
                        (voice == null ? sirenProfile != null : voice.sirenEnabled) && !basicsSiren,
                        controllingVehicle && SignalKeyBindings.horn().isKeyDown(),
                        controllingVehicle && !basicsSiren
                                && SignalKeyBindings.siren().isPressed());
                signalState = new BasicsAddonSignalBridge.SignalState(
                        signalState.hornActive || localSignals.isHornActive(),
                        signalState.sirenActive || localSignals.isSirenActive(),
                        signalState.indicatorLeftActive, signalState.indicatorRightActive);
            }
        }
        float maxRpm = Math.max(500.0F, profile.resolveAcousticMaxRpm(engineMaxRpm));
        float rpm = profile.mapAcousticRpm(normalizedRpm, engineMaxRpm);
        TireSlipTracker tireSlipTracker = voice == null
                ? new TireSlipTracker() : voice.tireSlipTracker;
        float tireSlip = tireSquealGloballyEnabled
                ? tireSlipTracker.update(readSkidInfos(entity)) : 0.0F;
        OccupantAcoustics occupantAcoustics = occupantAcoustics(entity);
        if (voice == null) {
            EngineTelemetry initial = new EngineTelemetry(rpm, maxRpm, throttle,
                    estimateLoad(null, rpm, maxRpm, throttle, speed, gear, engineOn),
                    speed, gear, engineOn, engineOn && throttle > 0.5F && normalizedRpm >= 0.985F,
                    brakeApplied, tireSlip, signalState.hornActive, signalState.sirenActive,
                    signalState.indicatorLeftActive, signalState.indicatorRightActive,
                    occupantAcoustics.occupant, occupantAcoustics.seatGain,
                    occupantAcoustics.interior, System.nanoTime());
            VehicleVoice created = new VehicleVoice(entity, engineName, profile, rotorProfile,
                    airBrakeProfile, reverseWarningProfile, brakeSquealProfile,
                    afterfireProfile, tireSquealProfile,
                    hornProfile, sirenProfile, indicatorProfile,
                    tireSlipTracker, localSignals, initial, clientTick);
            VehicleVoice existing = voices.putIfAbsent(id, created);
            voice = existing == null ? created : existing;
            if (existing == null) {
                int cylinders = profile.getFiringPattern() == null ? 0 : profile.getFiringPattern().getCylinderCount();
                int banks = profile.getFiringPattern() == null ? 0 : profile.getFiringPattern().getBankCount();
                LOGGER.debug("Observed DynamX engine {} on {}: profile={}, rotor={}, airBrake={}, reverseWarning={}, brakeSqueal={}, afterfire={}, tireSqueal={}, horn={}, siren={}, indicator={}, cylinders={}, banks={}, rpm={}/{}, engineOn={}",
                        engineName, vehicleName, profile.getPresetName(),
                        rotorProfile == null ? "none" : rotorProfile.getPresetName(),
                        airBrakeProfile == null ? "none" : airBrakeProfile.getPresetName(),
                        reverseWarningProfile == null ? "none" : reverseWarningProfile.getPresetName(),
                        brakeSquealProfile == null ? "none" : brakeSquealProfile.getPresetName(),
                        afterfireProfile == null ? "none" : afterfireProfile.getPresetName(),
                        tireSquealProfile == null ? "none" : tireSquealProfile.getPresetName(),
                        hornProfile == null ? "none" : hornProfile.getPresetName(),
                        sirenProfile == null ? "none" : sirenProfile.getPresetName(),
                        indicatorProfile == null ? "none" : indicatorProfile.getPresetName(),
                        cylinders, banks,
                        Math.round(rpm), Math.round(maxRpm), engineOn);
            }
        }

        EngineTelemetry previous = voice.telemetry;
        float load = estimateLoad(previous, rpm, maxRpm, throttle, speed, gear, engineOn);
        EngineTelemetry telemetry = new EngineTelemetry(rpm, maxRpm, throttle, load, speed, gear,
                engineOn, engineOn && throttle > 0.5F && normalizedRpm >= 0.985F,
                brakeApplied, tireSlip, signalState.hornActive, signalState.sirenActive,
                signalState.indicatorLeftActive, signalState.indicatorRightActive,
                occupantAcoustics.occupant, occupantAcoustics.seatGain,
                occupantAcoustics.interior, System.nanoTime());
        if (voice.lastEngineOn != engineOn) {
            LOGGER.debug("Engine transition source={} engine={} {} -> {}, rpm={}, throttle={}, load={}, gear={}, speed={}",
                    voice.sourceName, voice.engineName, voice.lastEngineOn, engineOn,
                    Math.round(rpm), throttle, load, gear, speed);
            voice.lastEngineOn = engineOn;
            voice.transitionTick = clientTick;
        }
        voice.entity = entity;
        voice.telemetry = telemetry;
        voice.lastSeenTick = clientTick;
    }

    public void tick() {
        if (closed) {
            return;
        }
        clientTick++;
        boolean configuredTireSqueal = AdvancedSoundConfig.enableTireSqueal;
        if (configuredTireSqueal != tireSquealGloballyEnabled) {
            tireSquealGloballyEnabled = configuredTireSqueal;
            clear();
            LOGGER.info("Tyre squeal config changed: {}. Vehicle audio voices will be rebuilt.",
                    configuredTireSqueal ? "enabled" : "disabled");
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean latestWindowFocused = Display.isActive();
        if (latestWindowFocused != windowFocused) {
            windowFocused = latestWindowFocused;
            LOGGER.debug("Minecraft window focus changed: focused={}, PCM target={} chunks",
                    latestWindowFocused, outputTargetChunks());
        }
        EntityPlayer player = minecraft.player;
        SoundSystem latest = findUsableSoundSystem();
        if (latest != soundSystem) {
            switchSoundSystem(latest);
        }
        if (player == null || minecraft.world == null || latest == null) {
            return;
        }

        List<VehicleVoice> candidates = new ArrayList<>();
        List<VehicleVoice> expired = new ArrayList<>();
        for (VehicleVoice voice : voices.values()) {
            BaseVehicleEntity<?> entity = voice.entity;
            if (entity == null || entity.isDead || entity.world != minecraft.world || clientTick - voice.lastSeenTick > 100) {
                expired.add(voice);
                continue;
            }
            double dx = entity.posX - player.posX;
            double dy = entity.posY - player.posY;
            double dz = entity.posZ - player.posZ;
            voice.distanceSquared = dx * dx + dy * dy + dz * dz;
            voice.audibleDistance = Math.max(AdvancedSoundSettings.AUDIBLE_DISTANCE,
                    voice.synthesizer.getActiveSignalDistance());
            if (voice.hornEnabled && voice.telemetry.hornActive) {
                voice.audibleDistance = Math.max(voice.audibleDistance, voice.hornDistance);
            }
            if (voice.sirenEnabled && voice.telemetry.sirenActive) {
                voice.audibleDistance = Math.max(voice.audibleDistance, voice.sirenDistance);
            }
            double maxDistanceSquared = voice.audibleDistance * voice.audibleDistance;
            boolean activeTireInput = voice.tireSquealEnabled && voice.telemetry.tireSlip > 0.60F;
            boolean activeSignalInput = (voice.hornEnabled && voice.telemetry.hornActive)
                    || (voice.sirenEnabled && voice.telemetry.sirenActive)
                    || (voice.indicatorEnabled && voice.telemetry.vehicleOccupant
                    && (voice.telemetry.indicatorLeftActive
                    || voice.telemetry.indicatorRightActive));
            if (voice.distanceSquared <= maxDistanceSquared
                    && (voice.telemetry.engineOn || activeTireInput || activeSignalInput
                    || !voice.synthesizer.isSilent())) {
                candidates.add(voice);
            }
        }
        candidates.sort(Comparator.comparingDouble(value -> value.distanceSquared));

        Set<UUID> selected = new HashSet<>();
        int limit = Math.min(AdvancedSoundSettings.MAX_VOICES, candidates.size());
        for (int i = 0; i < limit; i++) {
            VehicleVoice voice = candidates.get(i);
            EngineProfile profile = voice.synthesizer.getProfile();
            int cylinders = profile.getFiringPattern() == null
                    ? 0 : profile.getFiringPattern().getCylinderCount();
            voice.synthesizer.setFluidSubsteps(
                    SynthesisQualityPolicy.fluidSubsteps(i, cylinders, windowFocused));
            selected.add(voice.id);
            if (!voice.attached) {
                attach(latest, voice);
            }
            updateSpatialState(latest, voice, player);
            verifyStreamingSource(voice);
            if (streamingDisabled) {
                return;
            }
        }

        for (VehicleVoice voice : voices.values()) {
            if (voice.attached && !selected.contains(voice.id)) {
                detach(latest, voice);
            }
        }
        for (VehicleVoice voice : expired) {
            removeVoice(voice.id, voice);
        }
    }

    public void onSoundReload() {
        streamingDisabled = false;
        rejectedBackendName = null;
        switchSoundSystem(null);
    }

    public void clear() {
        SoundSystem current = soundSystem;
        if (current != null) {
            for (VehicleVoice voice : voices.values()) {
                detach(current, voice);
            }
        }
        voices.clear();
    }

    public void shutdown() {
        closed = true;
        AdvancedSoundRuntime.setReplacementAvailable(false);
        clear();
        audioExecutor.shutdownNow();
        synthesisExecutor.shutdownNow();
        closePcmOutput();
    }

    private void pumpAudio() {
        if (closed || streamingDisabled) {
            return;
        }
        SoundSystem current = soundSystem;
        if (current == null) {
            return;
        }

        SourceDataLine output = ensurePcmOutput();
        if (output == null) {
            return;
        }

        activeVoices.clear();
        for (VehicleVoice voice : voices.values()) {
            if (voice.attached && voice.attachedSystem == current) {
                activeVoices.add(voice);
            }
        }
        if (activeVoices.isEmpty()) {
            suspendEmptyOutput(output);
            return;
        }

        int bufferBytes = output.getBufferSize();
        int availableBytes = output.available();
        int queuedBytes = AudioBufferPolicy.queuedBytes(bufferBytes, availableBytes);
        int targetChunks = outputTargetChunks();
        if (outputFrames > 0 && queuedBytes == 0 && output.isRunning()) {
            synchronized (outputLock) {
                if (pcmOutput == output && output.isOpen()) {
                    output.stop();
                    output.flush();
                }
            }
            availableBytes = output.available();
            queuedBytes = AudioBufferPolicy.queuedBytes(bufferBytes, availableBytes);
            if (!outputStarved) {
                LOGGER.warn("JavaSound output buffer underrun; rebuilding the PCM reserve for {} voices",
                        activeVoices.size());
            }
            outputStarved = true;
        }
        if (!AudioBufferPolicy.needsRefill(bufferBytes, availableBytes, OUTPUT_CHUNK_BYTES,
                targetChunks)) {
            startPrimedOutput(output, queuedBytes, bufferBytes, targetChunks);
            return;
        }
        int refillChunks = Math.min(MAX_REFILL_CHUNKS_PER_PUMP,
                AudioBufferPolicy.chunksToRefill(bufferBytes, availableBytes,
                        OUTPUT_CHUNK_BYTES, targetChunks));
        for (int chunk = 0; chunk < refillChunks; chunk++) {
            renderAndWriteChunk(output);
        }
        int queuedAfterWrite = AudioBufferPolicy.queuedBytes(bufferBytes, output.available());
        startPrimedOutput(output, queuedAfterWrite, bufferBytes, targetChunks);
    }

    private void renderAndWriteChunk(SourceDataLine output) {
        Arrays.fill(leftMix, 0.0);
        Arrays.fill(rightMix, 0.0);
        pendingRenders.clear();
        for (VehicleVoice voice : activeVoices) {
            pendingRenders.add(synthesisExecutor.submit(() -> {
                EngineTelemetry renderTelemetry = applyDoppler(voice.telemetry, voice.dopplerFactor);
                byte[] pcm = voice.synthesizer.render(renderTelemetry, AdvancedSoundSettings.CHUNK_SAMPLES);
                return new RenderedVoice(voice, pcm, voice.leftGain, voice.rightGain);
            }));
        }
        for (Future<RenderedVoice> render : pendingRenders) {
            RenderedVoice rendered = awaitRender(render);
            VehicleVoice voice = rendered.voice;
            byte[] mono = rendered.pcm;
            voice.fedChunks++;
            float leftGain = rendered.leftGain;
            float rightGain = rendered.rightGain;
            for (int sample = 0; sample < AdvancedSoundSettings.CHUNK_SAMPLES; sample++) {
                int offset = sample * 2;
                short value = (short) ((mono[offset] & 0xFF) | (mono[offset + 1] << 8));
                leftMix[sample] += value * leftGain;
                rightMix[sample] += value * rightGain;
            }
        }

        double voiceNormalization = 1.0 / Math.sqrt(Math.max(1, activeVoices.size()));
        double master = clamp(outputMasterGain, 0.0F, 1.0F);
        for (int sample = 0; sample < AdvancedSoundSettings.CHUNK_SAMPLES; sample++) {
            short leftSample = softClip(leftMix[sample] * voiceNormalization * master);
            short rightSample = softClip(rightMix[sample] * voiceNormalization * master);
            int offset = sample * 4;
            stereoMix[offset] = (byte) (leftSample & 0xFF);
            stereoMix[offset + 1] = (byte) ((leftSample >>> 8) & 0xFF);
            stereoMix[offset + 2] = (byte) (rightSample & 0xFF);
            stereoMix[offset + 3] = (byte) ((rightSample >>> 8) & 0xFF);
        }

        synchronized (outputLock) {
            if (pcmOutput == output && output.isOpen()) {
                int written = output.write(stereoMix, 0, stereoMix.length);
                outputFrames += written / OUTPUT_FORMAT.getFrameSize();
            }
        }
    }

    private void startPrimedOutput(SourceDataLine output, int queuedBytes, int bufferBytes,
                                   int targetChunks) {
        int targetBytes = AudioBufferPolicy.targetQueuedBytes(bufferBytes, OUTPUT_CHUNK_BYTES,
                targetChunks);
        if (queuedBytes < targetBytes || output.isRunning()) {
            return;
        }
        synchronized (outputLock) {
            if (pcmOutput == output && output.isOpen() && !output.isRunning()) {
                output.start();
                outputStarved = false;
            }
        }
    }

    private int outputTargetChunks() {
        return windowFocused
                ? AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS
                : AdvancedSoundSettings.BACKGROUND_OUTPUT_TARGET_CHUNKS;
    }

    private void suspendEmptyOutput(SourceDataLine output) {
        if (!output.isRunning() && output.available() == output.getBufferSize()) {
            return;
        }
        synchronized (outputLock) {
            if (pcmOutput == output && output.isOpen()) {
                output.stop();
                output.flush();
                outputStarved = false;
            }
        }
    }

    private static RenderedVoice awaitRender(Future<RenderedVoice> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Engine synthesis interrupted", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Engine synthesis failed", cause);
        }
    }

    private void pumpAudioSafely() {
        try {
            pumpAudio();
        } catch (Throwable throwable) {
            failPcmOutput("continuous JavaSound PCM pump failed", throwable);
        }
    }

    private SourceDataLine ensurePcmOutput() {
        SourceDataLine existing = pcmOutput;
        if (existing != null && existing.isOpen()) {
            return existing;
        }
        synchronized (outputLock) {
            existing = pcmOutput;
            if (existing != null && existing.isOpen()) {
                return existing;
            }
            try {
                DataLine.Info lineInfo = new DataLine.Info(SourceDataLine.class, OUTPUT_FORMAT);
                SourceDataLine created = (SourceDataLine) AudioSystem.getLine(lineInfo);
                int requestedBufferBytes = OUTPUT_CHUNK_BYTES * AdvancedSoundSettings.OUTPUT_BUFFER_CHUNKS;
                created.open(OUTPUT_FORMAT, requestedBufferBytes);
                pcmOutput = created;
                outputFrames = 0;
                outputStarved = false;
                Mixer.Info defaultMixer = AudioSystem.getMixer(null).getMixerInfo();
                AdvancedSoundRuntime.setReplacementAvailable(true);
                LOGGER.info("JavaSound output ready: mixer='{}', line={}, format='{}', bufferBytes={}",
                        defaultMixer.getName(), created.getClass().getName(), OUTPUT_FORMAT, created.getBufferSize());
                return created;
            } catch (LineUnavailableException | IllegalArgumentException exception) {
                failPcmOutput("cannot open continuous JavaSound output " + OUTPUT_FORMAT, exception);
                return null;
            }
        }
    }

    private void closePcmOutput() {
        synchronized (outputLock) {
            SourceDataLine current = pcmOutput;
            pcmOutput = null;
            outputFrames = 0;
            outputStarved = false;
            if (current != null) {
                try {
                    current.stop();
                    current.flush();
                } finally {
                    current.close();
                }
            }
        }
    }

    private void failPcmOutput(String reason, Throwable throwable) {
        if (streamingDisabled) {
            return;
        }
        streamingDisabled = true;
        AdvancedSoundRuntime.setReplacementAvailable(false);
        closePcmOutput();
        LOGGER.error("Procedural output failed open: {}. DynamX engine samples remain enabled.", reason, throwable);
    }

    private static EngineTelemetry applyDoppler(EngineTelemetry telemetry, float factor) {
        if (Math.abs(factor - 1.0F) < 0.001F) {
            return telemetry;
        }
        return new EngineTelemetry(telemetry.rpm * factor, telemetry.maxRpm,
                telemetry.throttle, telemetry.load, telemetry.speedKmh, telemetry.gear,
                telemetry.engineOn, telemetry.revLimiter, telemetry.brakeApplied,
                telemetry.tireSlip, telemetry.hornActive, telemetry.sirenActive,
                telemetry.interior, telemetry.timestampNanos);
    }

    private static short softClip(double sample) {
        double normalized = sample / 32768.0;
        return (short) Math.round(Math.tanh(normalized) * 32767.0);
    }

    private void attach(SoundSystem system, VehicleVoice voice) {
        if (voice.everAttached || clientTick - voice.transitionTick > 2) {
            voice.synthesizer.requestResync(voice.telemetry);
        }
        voice.attachedAtTick = clientTick;
        voice.confirmedPlaying = false;
        voice.attachedSystem = system;
        voice.attached = true;
        if (!voice.everAttached) {
            LOGGER.debug("Attached JavaSound PCM voice {} for {} ({})",
                    voice.sourceName, voice.engineName, voice.synthesizer.getProfile().getPresetName());
        }
        voice.everAttached = true;
    }

    private void detach(SoundSystem system, VehicleVoice voice) {
        if (!voice.attached) {
            return;
        }
        voice.attached = false;
        voice.attachedSystem = null;
        voice.confirmedPlaying = false;
    }

    private void updateSpatialState(SoundSystem system, VehicleVoice voice, EntityPlayer player) {
        BaseVehicleEntity<?> entity = voice.entity;
        boolean playerVehicle = entity.isRidingOrBeingRiddenBy(player);
        float dynamXVolume = ClientProxy.SOUND_HANDLER == null ? 1.0F : ClientProxy.SOUND_HANDLER.getMasterVolume();
        float sourceGain = AdvancedSoundSettings.ENGINE_OUTPUT_GAIN
                * voice.synthesizer.getProfile().getOutputGain();
        sourceGain = DynamXVolumeScaler.apply(sourceGain, dynamXVolume);

        if (playerVehicle) {
            voice.leftGain = sourceGain;
            voice.rightGain = sourceGain;
        } else {
            double dx = entity.posX - player.posX;
            double dz = entity.posZ - player.posZ;
            double distance = Math.sqrt(voice.distanceSquared);
            float attenuation = (float) clamp(1.0 - distance / voice.audibleDistance, 0.0, 1.0);
            float pan = SpatialPanner.pan(dx, dz, player.rotationYaw);
            float spatialGain = sourceGain * attenuation;
            voice.leftGain = spatialGain * (1.0F - Math.max(0.0F, pan) * 0.82F);
            voice.rightGain = spatialGain * (1.0F + Math.min(0.0F, pan) * 0.82F);
        }
        voice.dopplerFactor = calculateDoppler(entity, player);
        outputMasterGain = clamp(system.getMasterVolume(), 0.0F, 1.0F);
    }

    private void switchSoundSystem(SoundSystem replacement) {
        AdvancedSoundRuntime.setReplacementAvailable(false);
        closePcmOutput();
        soundSystem = replacement;
        for (VehicleVoice voice : voices.values()) {
            voice.attached = false;
            voice.attachedSystem = null;
            voice.confirmedPlaying = false;
        }
        if (replacement != null) {
            LOGGER.debug("Verified Minecraft OpenAL; initializing independent continuous JavaSound PCM output");
        }
    }

    private SoundSystem findUsableSoundSystem() {
        if (streamingDisabled) {
            return null;
        }
        SoundSystem candidate = findSoundSystem();
        if (candidate == null) {
            return null;
        }
        try {
            if (MinecraftSoundSystemAccess.hasOpenAlBackend(candidate)) {
                rejectedBackendName = null;
                return candidate;
            }
            String backend = MinecraftSoundSystemAccess.getLibraryClassName(candidate);
            if (!backend.equals(rejectedBackendName) && !"initializing".equals(backend)) {
                rejectedBackendName = backend;
                LOGGER.error("Procedural replacement disabled: Minecraft audio backend is {}. "
                        + "DynamX engine samples will not be suppressed.", backend);
            }
        } catch (ReflectiveOperationException exception) {
            if (!soundReflectionFailureLogged) {
                soundReflectionFailureLogged = true;
                LOGGER.error("Cannot verify Minecraft's OpenAL backend; keeping DynamX engine samples enabled", exception);
            }
        }
        return null;
    }

    private void verifyStreamingSource(VehicleVoice voice) {
        if (voice.confirmedPlaying || !voice.telemetry.engineOn || clientTick - voice.attachedAtTick < 4) {
            return;
        }
        SourceDataLine output = pcmOutput;
        if (output != null && output.isOpen() && output.isRunning() && voice.fedChunks > 0 && outputFrames > 0) {
            voice.confirmedPlaying = true;
            LOGGER.debug("Verified continuous JavaSound PCM writes for {}", voice.engineName);
            return;
        }
        if (clientTick - voice.attachedAtTick >= 40) {
            disableStreaming("JavaSound output for " + voice.sourceName + " did not accept PCM within 2 seconds");
        }
    }

    private void disableStreaming(String reason) {
        if (streamingDisabled) {
            return;
        }
        streamingDisabled = true;
        AdvancedSoundRuntime.setReplacementAvailable(false);
        SoundSystem current = soundSystem;
        if (current != null) {
            for (VehicleVoice voice : voices.values()) {
                if (voice.attached) {
                    detach(current, voice);
                }
            }
        }
        closePcmOutput();
        soundSystem = null;
        LOGGER.error("Procedural replacement failed open: {}. DynamX engine samples are enabled again.", reason);
    }

    private SoundSystem findSoundSystem() {
        try {
            SoundSystem direct = MinecraftSoundSystemAccess.get();
            if (direct != null) {
                return direct;
            }
        } catch (ReflectiveOperationException exception) {
            if (!soundReflectionFailureLogged) {
                soundReflectionFailureLogged = true;
                LOGGER.error("Cannot access Minecraft SoundSystem directly; falling back to DynamX", exception);
            }
        }
        return ClientProxy.SOUND_HANDLER == null ? null : ClientProxy.SOUND_HANDLER.getMcSoundSystem();
    }

    private void removeVoice(UUID id, VehicleVoice voice) {
        if (voices.remove(id, voice)) {
            SoundSystem current = soundSystem;
            if (current != null && voice.attached) {
                detach(current, voice);
            }
        }
    }

    private static float estimateLoad(EngineTelemetry previous, float rpm, float maxRpm, float throttle,
                                      float speed, int gear, boolean engineOn) {
        if (!engineOn) {
            return 0.0F;
        }
        float rpmFraction = clamp(rpm / maxRpm, 0.0F, 1.0F);
        float engaged = gear == 0 ? 0.0F : 1.0F;
        float roadLoad = engaged * clamp(Math.abs(speed) / 120.0F, 0.0F, 0.18F);
        float accelerationLoad = 0.0F;
        if (previous != null) {
            accelerationLoad = clamp(Math.abs(rpm - previous.rpm) / Math.max(300.0F, maxRpm * 0.08F), 0.0F, 0.20F);
        }
        float base = throttle > 0.0F
                ? 0.48F + throttle * 0.25F + (1.0F - rpmFraction) * 0.12F
                : 0.07F + engaged * 0.11F;
        return clamp(base + roadLoad + accelerationLoad, 0.0F, 1.0F);
    }

    private static boolean isLocalDriver(BaseVehicleEntity<?> entity) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft.player != null && entity.getControllingPassenger() == minecraft.player;
    }

    private static float[] readSkidInfos(BaseVehicleEntity<?> entity) {
        WheelsModule wheelsModule = entity.getModuleByType(WheelsModule.class);
        return wheelsModule == null ? null : wheelsModule.getSkidInfos();
    }

    private static float calculateDoppler(BaseVehicleEntity<?> entity, EntityPlayer player) {
        if (entity.isRidingOrBeingRiddenBy(player)) {
            return 1.0F;
        }
        double dx = entity.posX - player.posX;
        double dy = entity.posY - player.posY;
        double dz = entity.posZ - player.posZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.001) {
            return 1.0F;
        }
        double radialBlocksPerTick = ((entity.motionX - player.motionX) * dx
                + (entity.motionY - player.motionY) * dy
                + (entity.motionZ - player.motionZ) * dz) / distance;
        double radialMetersPerSecond = radialBlocksPerTick * 20.0;
        return (float) clamp(343.0 / (343.0 + radialMetersPerSecond), 0.82, 1.22);
    }

    private static OccupantAcoustics occupantAcoustics(BaseVehicleEntity<?> entity) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        boolean occupant = player != null && entity.isRidingOrBeingRiddenBy(player);
        if (!occupant) {
            return OccupantAcoustics.OUTSIDE;
        }

        float seatGain = 1.0F;
        SeatsModule seats = entity.getModuleByType(SeatsModule.class);
        if (seats != null) {
            BasePartSeat<?, ?> listenerSeat = seats.getRidingSeat(player);
            BasePartSeat<?, ?> driverSeat = null;
            for (PartEntitySeat seat : entity.getPackInfo().getPartsByType(PartEntitySeat.class)) {
                if (seat.isDriver()) {
                    driverSeat = seat;
                    break;
                }
            }
            if (listenerSeat != null && driverSeat != null
                    && listenerSeat.getPosition() != null && driverSeat.getPosition() != null) {
                float dx = listenerSeat.getPosition().x - driverSeat.getPosition().x;
                float dy = listenerSeat.getPosition().y - driverSeat.getPosition().y;
                float dz = listenerSeat.getPosition().z - driverSeat.getPosition().z;
                double seatDistance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                double distanceBeyondFrontCabin = Math.max(0.0, seatDistance - 0.60);
                seatGain = (float) Math.exp(-distanceBeyondFrontCabin * 1.15);
                if (seatGain < 0.01F) {
                    seatGain = 0.0F;
                }
            }
        }
        boolean interior = Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
        return new OccupantAcoustics(true, interior, seatGain);
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isNaN(value) || Float.isInfinite(value) ? fallback : value;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class OccupantAcoustics {
        private static final OccupantAcoustics OUTSIDE =
                new OccupantAcoustics(false, false, 0.0F);

        private final boolean occupant;
        private final boolean interior;
        private final float seatGain;

        private OccupantAcoustics(boolean occupant, boolean interior, float seatGain) {
            this.occupant = occupant;
            this.interior = interior;
            this.seatGain = seatGain;
        }
    }

    private static final class VehicleVoice {
        private final UUID id;
        private final String engineName;
        private final String sourceName;
        private final EngineVoice synthesizer;
        private final boolean tireSquealEnabled;
        private final boolean hornEnabled;
        private final boolean sirenEnabled;
        private final boolean indicatorEnabled;
        private final float hornDistance;
        private final float sirenDistance;
        private final TireSlipTracker tireSlipTracker;
        private final ClientLocalSignalState localSignals;
        private volatile BaseVehicleEntity<?> entity;
        private volatile EngineTelemetry telemetry;
        private volatile boolean attached;
        private volatile SoundSystem attachedSystem;
        private volatile boolean confirmedPlaying;
        private volatile long fedChunks;
        private volatile float leftGain;
        private volatile float rightGain;
        private volatile float dopplerFactor = 1.0F;
        private boolean lastEngineOn;
        private boolean everAttached;
        private long attachedAtTick;
        private long transitionTick = Long.MIN_VALUE / 2;
        private long lastSeenTick;
        private double distanceSquared;
        private float audibleDistance = AdvancedSoundSettings.AUDIBLE_DISTANCE;

        private VehicleVoice(BaseVehicleEntity<?> entity, String engineName, EngineProfile profile,
                             RotorProfile rotorProfile, AirBrakeProfile airBrakeProfile,
                             ReverseWarningProfile reverseWarningProfile,
                             BrakeSquealProfile brakeSquealProfile, AfterfireProfile afterfireProfile,
                             TireSquealProfile tireSquealProfile,
                             HornProfile hornProfile, SirenProfile sirenProfile,
                             IndicatorProfile indicatorProfile,
                             TireSlipTracker tireSlipTracker,
                             ClientLocalSignalState localSignals, EngineTelemetry telemetry, long tick) {
            this.id = entity.getUniqueID();
            this.engineName = engineName;
            this.sourceName = "advancedsoundaddon_" + id.toString().replace("-", "");
            this.entity = entity;
            this.telemetry = telemetry;
            this.tireSquealEnabled = tireSquealProfile != null;
            this.hornEnabled = hornProfile != null;
            this.sirenEnabled = sirenProfile != null;
            this.indicatorEnabled = indicatorProfile != null;
            this.hornDistance = hornProfile == null ? 0.0F : hornProfile.getAudibleDistance();
            this.sirenDistance = sirenProfile == null ? 0.0F : sirenProfile.getAudibleDistance();
            this.tireSlipTracker = tireSlipTracker;
            this.localSignals = localSignals;
            this.synthesizer = new EngineVoice(profile, rotorProfile, airBrakeProfile,
                    brakeSquealProfile, afterfireProfile, tireSquealProfile,
                    hornProfile, sirenProfile, reverseWarningProfile, indicatorProfile,
                    AdvancedSoundSettings.SAMPLE_RATE, telemetry);
            this.lastEngineOn = telemetry.engineOn;
            this.lastSeenTick = tick;
        }
    }

    private static final class RenderedVoice {
        private final VehicleVoice voice;
        private final byte[] pcm;
        private final float leftGain;
        private final float rightGain;

        private RenderedVoice(VehicleVoice voice, byte[] pcm, float leftGain, float rightGain) {
            this.voice = voice;
            this.pcm = pcm;
            this.leftGain = leftGain;
            this.rightGain = rightGain;
        }
    }

    private static final class AudioThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "AdvancedSoundAddon PCM producer");
            thread.setDaemon(true);
            thread.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 2));
            thread.setUncaughtExceptionHandler((ignored, throwable) ->
                    LOGGER.error("Procedural audio worker stopped unexpectedly", throwable));
            return thread;
        }
    }

    private static final class SynthesisThreadFactory implements ThreadFactory {
        private final AtomicInteger number = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable,
                    "AdvancedSoundAddon synthesizer " + number.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Math.min(Thread.MAX_PRIORITY, Thread.NORM_PRIORITY + 1));
            thread.setUncaughtExceptionHandler((ignored, throwable) ->
                    LOGGER.error("Procedural synthesis worker stopped unexpectedly", throwable));
            return thread;
        }
    }
}
