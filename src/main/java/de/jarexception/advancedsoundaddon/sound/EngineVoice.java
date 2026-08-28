package de.jarexception.advancedsoundaddon.sound;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/** Synthesizes realtime engine audio from combustion and exhaust pressure. */
public final class EngineVoice {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final double AFTERFIRE_OUTPUT_SCALE = 2.00;

    private enum Lifecycle {
        OFF, STARTING, RUNNING, STOPPING
    }

    private final EngineProfile profile;
    private final int sampleRate;
    private final AlternativePowertrainVoice alternativePowertrain;
    private final TurbochargerVoice turbochargerVoice;
    private final RotorVoice rotorVoice;
    private final AirBrakeVoice airBrakeVoice;
    private final BrakeSquealVoice brakeSquealVoice;
    private final TireSquealVoice tireSquealVoice;
    private final HornVoice hornVoice;
    private final SirenVoice sirenVoice;
    private final AfterfireController afterfireController;
    private final double rpmRiseSmoothing;
    private final double rpmFallSmoothing;
    private final Dsp.ButterworthLowPass[] flowAntialiasFilters;
    private final Dsp.JitterFilter[] flowJitterFilters;
    private final Dsp.OnePoleLowPass[] flowDcFilters;
    private final Dsp.DelayLine[] bankAcousticDelays;
    private final double[] previousFlow;
    private final Dsp.PartitionedConvolver exhaustConvolver;
    private final Dsp.Leveler exhaustLeveler;
    private final Dsp.OnePoleLowPass exhaustAirNoiseLowPass;
    private final CylinderFlowModel cylinderFlowModel;
    private final Dsp.Resonator intakeResonator;
    private final Dsp.Resonator intakeHarmonicResonator;
    private final Dsp.OnePoleLowPass intakeNoiseLowPass;
    private final Dsp.OnePoleHighPass intakeNoiseHighPass;
    private final Dsp.OnePoleLowPass mechanicalNoiseLowPass;
    private final Dsp.OnePoleHighPass mechanicalNoiseHighPass;
    private final Dsp.OnePoleLowPass starterNoiseLowPass;
    private final Dsp.OnePoleHighPass starterNoiseHighPass;
    private final Dsp.Resonator starterHousingResonator;
    private final Dsp.Resonator starterGearResonator;
    private final Dsp.OnePoleLowPass afterfireNoiseLowPass;
    private final Dsp.OnePoleHighPass afterfireNoiseHighPass;
    private final Dsp.OnePoleLowPass afterfireRoarLowPass;
    private final Dsp.OnePoleHighPass afterfireRoarHighPass;
    private final Dsp.Resonator afterfireBodyResonator;
    private final Dsp.Resonator afterfireCrackResonator;
    private final Dsp.DelayLine afterfireEarlyReflection;
    private final Dsp.DelayLine afterfireLateReflection;
    private final Dsp.ButterworthLowPass outputLowPass;
    private final Dsp.OnePoleHighPass outputSubsonicHighPass1;
    private final Dsp.OnePoleHighPass outputSubsonicHighPass2;
    private final DieselCombustionVoice dieselCombustionVoice;
    private final Dsp.ButterworthLowPass dieselExhaustLowPass;
    private final AtomicReference<EngineTelemetry> pendingResync = new AtomicReference<>();

    private Lifecycle lifecycle;
    private boolean previousEngineOn;
    private volatile boolean silent;
    private double lifecycleTime;
    private double offTailTime;
    private double currentRpm;
    private double stopOriginRpm;
    private double smoothedThrottle;
    private double smoothedLoad;
    private double lastTargetRpm;
    private double crankPhase;
    private double gearPhase;
    private double starterMeshPhase;
    private double starterMotorPhase;
    private double starterEngagementEnergy;
    private double idleWanderPhase;
    private double idleWanderPhase2;
    private double lastTorqueRipple;
    private double lastCompressionRipple;
    private double limiterClock;
    private double transientEnergy;
    private double afterfireCrackEnvelope;
    private double afterfirePressureEnvelope;
    private double afterfireRoarEnvelope;
    private double afterfireImpulse;
    private double afterfirePressurePhase;
    private double afterfirePressureFrequency;
    private double streamFade;
    private double[] exhaustSynthInput = new double[0];
    private double[] dryMix = new double[0];
    private double[] afterfireMix = new double[0];
    private double[] convolvedExhaust = new double[0];
    private int randomState = 0x51F15EED;

    public EngineVoice(EngineProfile profile, int sampleRate, EngineTelemetry initialTelemetry) {
        this(profile, null, null, null, null, null, sampleRate, initialTelemetry);
    }

    public EngineVoice(EngineProfile profile, RotorProfile rotorProfile, int sampleRate,
                       EngineTelemetry initialTelemetry) {
        this(profile, rotorProfile, null, null, null, null, sampleRate, initialTelemetry);
    }

    public EngineVoice(EngineProfile profile, RotorProfile rotorProfile,
                       AirBrakeProfile airBrakeProfile, int sampleRate,
                       EngineTelemetry initialTelemetry) {
        this(profile, rotorProfile, airBrakeProfile, null, null, null,
                sampleRate, initialTelemetry);
    }

    public EngineVoice(EngineProfile profile, RotorProfile rotorProfile,
                       AirBrakeProfile airBrakeProfile, BrakeSquealProfile brakeSquealProfile,
                       int sampleRate, EngineTelemetry initialTelemetry) {
        this(profile, rotorProfile, airBrakeProfile, brakeSquealProfile, null, null,
                sampleRate, initialTelemetry);
    }

    public EngineVoice(EngineProfile profile, RotorProfile rotorProfile,
                       AirBrakeProfile airBrakeProfile, BrakeSquealProfile brakeSquealProfile,
                       AfterfireProfile afterfireProfile,
                       int sampleRate, EngineTelemetry initialTelemetry) {
        this(profile, rotorProfile, airBrakeProfile, brakeSquealProfile, afterfireProfile,
                null, sampleRate, initialTelemetry);
    }

    public EngineVoice(EngineProfile profile, RotorProfile rotorProfile,
                       AirBrakeProfile airBrakeProfile, BrakeSquealProfile brakeSquealProfile,
                       AfterfireProfile afterfireProfile, TireSquealProfile tireSquealProfile,
                       int sampleRate, EngineTelemetry initialTelemetry) {
        this(profile, rotorProfile, airBrakeProfile, brakeSquealProfile, afterfireProfile,
                tireSquealProfile, null, null, sampleRate, initialTelemetry);
    }

    public EngineVoice(EngineProfile profile, RotorProfile rotorProfile,
                       AirBrakeProfile airBrakeProfile, BrakeSquealProfile brakeSquealProfile,
                       AfterfireProfile afterfireProfile, TireSquealProfile tireSquealProfile,
                       HornProfile hornProfile, SirenProfile sirenProfile,
                       int sampleRate, EngineTelemetry initialTelemetry) {
        this.profile = profile;
        this.sampleRate = sampleRate;
        TurbochargerProfile turbochargerProfile = TurbochargerProfile.forEngineProfile(profile);
        turbochargerVoice = turbochargerProfile == null ? null
                : new TurbochargerVoice(turbochargerProfile, sampleRate, initialTelemetry);
        rotorVoice = rotorProfile == null ? null
                : new RotorVoice(rotorProfile, sampleRate, initialTelemetry);
        airBrakeVoice = airBrakeProfile == null ? null
                : new AirBrakeVoice(airBrakeProfile, sampleRate, initialTelemetry);
        brakeSquealVoice = brakeSquealProfile == null ? null
                : new BrakeSquealVoice(brakeSquealProfile, sampleRate, initialTelemetry);
        tireSquealVoice = tireSquealProfile == null ? null
                : new TireSquealVoice(tireSquealProfile, sampleRate, initialTelemetry);
        hornVoice = hornProfile == null ? null
                : new HornVoice(hornProfile, profile.getOutputGain(), sampleRate, initialTelemetry);
        sirenVoice = sirenProfile == null ? null
                : new SirenVoice(sirenProfile, profile.getOutputGain(), sampleRate, initialTelemetry);
        afterfireController = afterfireProfile == null
                || profile.getPowertrain() != EnginePowertrain.COMBUSTION
                || profile.isCompressionIgnition() ? null
                : new AfterfireController(afterfireProfile, initialTelemetry);
        if (afterfireController == null) {
            afterfireNoiseLowPass = null;
            afterfireNoiseHighPass = null;
            afterfireRoarLowPass = null;
            afterfireRoarHighPass = null;
            afterfireBodyResonator = null;
            afterfireCrackResonator = null;
            afterfireEarlyReflection = null;
            afterfireLateReflection = null;
        } else {
            afterfireNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 5_600);
            afterfireNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 120);
            afterfireRoarLowPass = new Dsp.OnePoleLowPass(sampleRate, 2_350);
            afterfireRoarHighPass = new Dsp.OnePoleHighPass(sampleRate, 42);
            afterfireBodyResonator = new Dsp.Resonator(sampleRate,
                    Math.max(52.0, profile.getExhaustResonanceHz() * 0.82), 18);
            afterfireCrackResonator = new Dsp.Resonator(sampleRate, 470, 360);
            afterfireEarlyReflection = new Dsp.DelayLine((int) Math.round(sampleRate * 0.019));
            afterfireLateReflection = new Dsp.DelayLine((int) Math.round(sampleRate * 0.047));
        }
        rpmRiseSmoothing = 1.0 - Math.exp(-1.0 / sampleRate / 0.060);
        rpmFallSmoothing = 1.0 - Math.exp(-1.0 / sampleRate / 0.095);
        if (profile.getPowertrain() != EnginePowertrain.COMBUSTION) {
            alternativePowertrain = new AlternativePowertrainVoice(
                    profile, sampleRate, initialTelemetry);
            flowAntialiasFilters = null;
            flowJitterFilters = null;
            flowDcFilters = null;
            bankAcousticDelays = null;
            previousFlow = null;
            exhaustConvolver = null;
            exhaustLeveler = null;
            exhaustAirNoiseLowPass = null;
            cylinderFlowModel = null;
            intakeResonator = null;
            intakeHarmonicResonator = null;
            intakeNoiseLowPass = null;
            intakeNoiseHighPass = null;
            mechanicalNoiseLowPass = null;
            mechanicalNoiseHighPass = null;
            starterNoiseLowPass = null;
            starterNoiseHighPass = null;
            starterHousingResonator = null;
            starterGearResonator = null;
            outputLowPass = null;
            outputSubsonicHighPass1 = null;
            outputSubsonicHighPass2 = null;
            dieselCombustionVoice = null;
            dieselExhaustLowPass = null;
            return;
        }
        alternativePowertrain = null;
        int bankCount = profile.getFiringPattern().getBankCount();
        flowAntialiasFilters = new Dsp.ButterworthLowPass[bankCount];
        flowJitterFilters = new Dsp.JitterFilter[bankCount];
        flowDcFilters = new Dsp.OnePoleLowPass[bankCount];
        bankAcousticDelays = new Dsp.DelayLine[bankCount];
        previousFlow = new double[bankCount];
        for (int bank = 0; bank < bankCount; bank++) {
            flowAntialiasFilters[bank] = new Dsp.ButterworthLowPass(sampleRate, 1_900.0);
            flowJitterFilters[bank] = new Dsp.JitterFilter(sampleRate, 10, 10_000);
            flowDcFilters[bank] = new Dsp.OnePoleLowPass(sampleRate, 10);
            int delaySamples = Math.max(1, Math.round(
                    profile.getBankDelayMillis(bank) * sampleRate / 1_000.0F));
            bankAcousticDelays[bank] = new Dsp.DelayLine(delaySamples);
        }
        double[] exhaustImpulse = EngineSimImpulseResponses.forProfile(profile, sampleRate);
        exhaustConvolver = new Dsp.PartitionedConvolver(exhaustImpulse);
        exhaustLeveler = new Dsp.Leveler(sampleRate, 0.72, 0.00001, 1.9);
        exhaustAirNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 2_000);
        cylinderFlowModel = new CylinderFlowModel(profile, sampleRate,
                profile.getExhaustResonanceHz());
        intakeResonator = new Dsp.Resonator(sampleRate, profile.getIntakeResonanceHz(), profile.getIntakeResonanceHz() * 0.72);
        intakeHarmonicResonator = new Dsp.Resonator(sampleRate, profile.getIntakeResonanceHz() * 2.38,
                profile.getIntakeResonanceHz() * 1.45);
        intakeNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 12_500);
        intakeNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 680);
        mechanicalNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 15_000);
        mechanicalNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 2_100);
        starterNoiseLowPass = new Dsp.OnePoleLowPass(sampleRate, 4_200);
        starterNoiseHighPass = new Dsp.OnePoleHighPass(sampleRate, 95);
        starterHousingResonator = new Dsp.Resonator(sampleRate, 310, 260);
        starterGearResonator = new Dsp.Resonator(sampleRate, 940, 720);
        outputLowPass = new Dsp.ButterworthLowPass(sampleRate, 15_500);
        double subsonicCutoff = profile.isCompressionIgnition() ? 48.0 : 32.0;
        outputSubsonicHighPass1 = new Dsp.OnePoleHighPass(sampleRate, subsonicCutoff);
        outputSubsonicHighPass2 = new Dsp.OnePoleHighPass(sampleRate, subsonicCutoff);
        dieselCombustionVoice = profile.isCompressionIgnition()
                ? new DieselCombustionVoice(profile, sampleRate) : null;
        dieselExhaustLowPass = profile.isCompressionIgnition()
                ? new Dsp.ButterworthLowPass(sampleRate, 270) : null;

        previousEngineOn = initialTelemetry.engineOn;
        lifecycle = initialTelemetry.engineOn ? Lifecycle.RUNNING : Lifecycle.OFF;
        currentRpm = initialTelemetry.engineOn
                ? Math.max(initialTelemetry.rpm, profile.getIdleRpm()) : 0.0;
        lastTargetRpm = initialTelemetry.rpm;
        smoothedThrottle = initialTelemetry.throttle;
        smoothedLoad = initialTelemetry.load;
        silent = !initialTelemetry.engineOn;
    }

    public void requestResync(EngineTelemetry telemetry) {
        if (turbochargerVoice != null) {
            turbochargerVoice.requestResync(telemetry);
        }
        if (rotorVoice != null) {
            rotorVoice.requestResync(telemetry);
        }
        if (airBrakeVoice != null) {
            airBrakeVoice.requestResync(telemetry);
        }
        if (brakeSquealVoice != null) {
            brakeSquealVoice.requestResync(telemetry);
        }
        if (tireSquealVoice != null) {
            tireSquealVoice.requestResync(telemetry);
        }
        if (hornVoice != null) {
            hornVoice.requestResync(telemetry);
        }
        if (sirenVoice != null) {
            sirenVoice.requestResync(telemetry);
        }
        if (alternativePowertrain != null) {
            alternativePowertrain.requestResync(telemetry);
            return;
        }
        pendingResync.set(telemetry);
    }

    public byte[] render(EngineTelemetry telemetry, int sampleCount) {
        byte[] powertrain = renderPowertrain(telemetry, sampleCount);
        byte[] mixed = powertrain;
        if (turbochargerVoice != null) {
            mixed = mixPcm(mixed, turbochargerVoice.render(telemetry, sampleCount));
        }
        if (rotorVoice != null) {
            mixed = mixPcm(mixed, rotorVoice.render(telemetry, sampleCount));
        }
        if (airBrakeVoice != null) {
            mixed = mixPcm(mixed, airBrakeVoice.render(telemetry, sampleCount));
        }
        if (brakeSquealVoice != null) {
            mixed = mixPcm(mixed, brakeSquealVoice.render(telemetry, sampleCount));
        }
        if (tireSquealVoice != null) {
            mixed = mixPcm(mixed, tireSquealVoice.render(telemetry, sampleCount));
        }
        if (hornVoice != null) {
            mixed = mixPcm(mixed, hornVoice.render(telemetry, sampleCount));
        }
        if (sirenVoice != null) {
            mixed = mixPcm(mixed, sirenVoice.render(telemetry, sampleCount));
        }
        return mixed;
    }

    private byte[] renderPowertrain(EngineTelemetry telemetry, int sampleCount) {
        if (alternativePowertrain != null) {
            return alternativePowertrain.render(telemetry, sampleCount);
        }
        EngineTelemetry resync = pendingResync.getAndSet(null);
        if (resync != null) {
            synchronize(resync);
            telemetry = resync;
        }

        handleLifecycleTransition(telemetry);
        if (afterfireController != null) {
            afterfireController.observe(telemetry, currentRpm);
        }
        double targetDelta = telemetry.rpm - lastTargetRpm;
        transientEnergy = Math.min(1.5, transientEnergy + Math.abs(targetDelta) / Math.max(500.0, telemetry.maxRpm * 0.20));
        lastTargetRpm = telemetry.rpm;
        outputLowPass.setCutoff(telemetry.interior ? 9_500 : 18_000);

        byte[] pcm = new byte[sampleCount * 2];
        ensureScratchCapacity(sampleCount);
        Arrays.fill(exhaustSynthInput, 0.0);
        Arrays.fill(dryMix, 0.0);
        Arrays.fill(afterfireMix, 0.0);
        final double dt = 1.0 / sampleRate;
        final double throttleAlpha = 1.0 - Math.exp(-dt / 0.055);
        final double loadAlpha = 1.0 - Math.exp(-dt / 0.090);
        final double transientDecay = Math.exp(-dt / 0.16);
        final double sharpness = profile.getPulseSharpness();
        for (int sample = 0; sample < sampleCount; sample++) {
            double ignitionBlend = advanceLifecycle(telemetry, dt);
            smoothedThrottle += (telemetry.throttle - smoothedThrottle) * throttleAlpha;
            smoothedLoad += (telemetry.load - smoothedLoad) * loadAlpha;

            double crankRadiansPerSample = currentRpm / 60.0 * TWO_PI / sampleRate;
            crankPhase = wrapPhase(crankPhase + crankRadiansPerSample);
            gearPhase = wrapPhase(gearPhase + crankRadiansPerSample * (5.25 + 0.18 * Math.abs(telemetry.gear)));

            boolean limiterCut = false;
            if (telemetry.revLimiter && lifecycle == Lifecycle.RUNNING) {
                limiterClock += dt;
                if (limiterClock >= 0.112) {
                    limiterClock -= 0.112;
                }
                limiterCut = limiterClock < 0.050;
            } else {
                limiterClock = 0.0;
            }
            if (afterfireController != null) {
                double eventEnergy = afterfireController.step(currentRpm, telemetry.maxRpm, dt);
                if (eventEnergy > 0.0) {
                    cylinderFlowModel.injectAfterfire(eventEnergy);
                    triggerAfterfireTransient(eventEnergy);
                }
            }
            transientEnergy *= transientDecay;

            cylinderFlowModel.step(currentRpm, smoothedThrottle,
                    Dsp.clamp(smoothedLoad + transientEnergy * 0.06, 0.0, 1.0),
                    ignitionBlend, limiterCut, lifecycle == Lifecycle.STARTING);
            lastTorqueRipple = cylinderFlowModel.getTorqueRipple();
            lastCompressionRipple = cylinderFlowModel.getCompressionRipple();

            double rpmFraction = Dsp.clamp(currentRpm / telemetry.maxRpm, 0.0, 1.15);
            double noise = randomUnit() * 2.0 - 1.0;
            double afterfire = synthesizeAfterfireTransient(noise, dt);
            double exhaustAirNoise = exhaustAirNoiseLowPass.process(noise);
            for (int bank = 0; bank < flowAntialiasFilters.length; bank++) {
                double antialiased = flowAntialiasFilters[bank]
                        .process(cylinderFlowModel.getBankFlow(bank));
                double delayed = bankAcousticDelays[bank].process(antialiased);
                double jitterScale = profile.isCompressionIgnition()
                        ? 0.10 : 0.32 + sharpness * 0.20;
                double jittered = flowJitterFilters[bank].process(delayed, randomUnit(),
                        jitterScale);
                double dcEstimate = flowDcFilters[bank].process(jittered);
                double acFlow = jittered - dcEstimate;
                double derivative = (jittered - previousFlow[bank]) * sampleRate;
                previousFlow[bank] = jittered;
                double airNoiseMix = exhaustAirNoise;
                double synthesizerInput = derivative * 0.01 + acFlow * airNoiseMix * 0.99;
                exhaustSynthInput[sample] += synthesizerInput;
            }

            double intakeFlow = cylinderFlowModel.getIntakeFlow();
            double intakeAir = intakeNoiseHighPass.process(intakeNoiseLowPass.process(noise));
            double airflowGain = (0.005 + 0.082 * smoothedThrottle)
                    * (0.20 + 0.80 * rpmFraction)
                    * (0.45 + 0.55 * smoothedLoad)
                    * profile.getInductionCharacter() * ignitionBlend;
            double intake = intakeResonator.process(intakeFlow) * 2.40
                    + intakeHarmonicResonator.process(intakeFlow) * 1.05
                    + intakeFlow * (0.055 + smoothedThrottle * 0.060)
                    + intakeAir * airflowGain;

            double valveTick = cylinderFlowModel.getValveActivity() * (4.0 + rpmFraction * 4.5);
            double mechanicalNoise = mechanicalNoiseHighPass.process(mechanicalNoiseLowPass.process(noise));
            double crankMechanics = (lastTorqueRipple * 0.62 - lastCompressionRipple * 0.38)
                    + (Math.sin(crankPhase * 2.0 + 0.31) * 0.055
                    + Math.sin(crankPhase * 4.0 + 1.17) * 0.022) * rpmFraction;
            double gearWhine = (Math.sin(gearPhase) * 0.075
                    + Math.sin(gearPhase * 2.01 + 0.42) * 0.028)
                    * rpmFraction * (0.25 + 0.75 * smoothedLoad);
            double valveTickGain = dieselCombustionVoice == null ? 0.11 : 0.062;
            double mechanical = crankMechanics * 0.16
                    + gearWhine * 0.12
                    + valveTick * valveTickGain * profile.getMechanicalBrightness()
                    + mechanicalNoise * (0.012 + rpmFraction * 0.038)
                    * profile.getMechanicalBrightness() * ignitionBlend;
            double dieselCombustion = dieselCombustionVoice == null ? 0.0
                    : dieselCombustionVoice.process(currentRpm, telemetry.maxRpm,
                    smoothedThrottle, smoothedLoad, ignitionBlend);

            double starter = synthesizeStarter(ignitionBlend, dt, mechanicalNoise, lastCompressionRipple);
            if (telemetry.interior) {
                dryMix[sample] = intake * profile.getIntakeGain() * 0.12
                        + mechanical * profile.getMechanicalGain() * 0.30
                        + dieselCombustion * 0.52
                        + starter * 0.86;
                afterfireMix[sample] = afterfire * 0.52 * AFTERFIRE_OUTPUT_SCALE;
            } else {
                dryMix[sample] = intake * profile.getIntakeGain() * 0.19
                        + mechanical * profile.getMechanicalGain() * 0.24
                        + dieselCombustion * 0.92
                        + starter * 0.82;
                afterfireMix[sample] = afterfire * AFTERFIRE_OUTPUT_SCALE;
            }
        }

        exhaustConvolver.process(exhaustSynthInput, convolvedExhaust);
        double loadGain = 0.70 + 0.30 * Dsp.clamp(smoothedLoad, 0.0, 1.0);
        double exhaustGain = profile.getExhaustGain() * loadGain
                * (telemetry.interior ? 0.48 : 0.70);
        if (dieselExhaustLowPass != null) {
            double aboveIdle = Dsp.clamp((currentRpm - profile.getIdleRpm())
                    / Math.max(500.0, telemetry.maxRpm - profile.getIdleRpm()), 0.0, 1.0);
            dieselExhaustLowPass.setCutoff(270.0 + 2_300.0 * Math.pow(aboveIdle, 0.70));
            exhaustGain *= 0.68 + aboveIdle * 0.20;
        }
        for (int sample = 0; sample < sampleCount; sample++) {
            double exhaust = exhaustLeveler.process(convolvedExhaust[sample]) * exhaustGain;
            if (dieselExhaustLowPass != null) {
                exhaust = dieselExhaustLowPass.process(exhaust);
            }
            double mix = exhaust + dryMix[sample] + afterfireMix[sample];
            if (dieselCombustionVoice != null) {
                mix = Math.tanh(mix * 3.2) * 0.49;
            }
            mix = outputLowPass.process(mix);
            mix = outputSubsonicHighPass2.process(outputSubsonicHighPass1.process(mix));
            streamFade = Math.min(1.0, streamFade + dt / 0.045);
            mix *= streamFade;
            double scaled = mix * 0.78;
            double limited = Math.abs(afterfireMix[sample]) > 1.0e-9
                    ? softLimitAfterfire(scaled)
                    : Dsp.clamp(scaled, -0.96, 0.96);
            short value = (short) Math.round(Dsp.clamp(limited, -1.0, 1.0) * 32767.0);
            pcm[sample * 2] = (byte) (value & 0xFF);
            pcm[sample * 2 + 1] = (byte) ((value >>> 8) & 0xFF);
        }
        return pcm;
    }

    private static byte[] mixPcm(byte[] powertrain, byte[] layer) {
        byte[] mixed = new byte[powertrain.length];
        for (int offset = 0; offset < mixed.length; offset += 2) {
            short engineSample = (short) ((powertrain[offset] & 0xFF) | (powertrain[offset + 1] << 8));
            short layerSample = (short) ((layer[offset] & 0xFF) | (layer[offset + 1] << 8));
            short output;
            if (layerSample == 0) {
                output = engineSample;
            } else if (engineSample == 0) {
                output = layerSample;
            } else {
                double sum = engineSample / 32768.0 + layerSample / 32768.0;
                output = (short) Math.round(Dsp.clamp(sum, -0.96, 0.96) * 32767.0);
            }
            mixed[offset] = (byte) (output & 0xFF);
            mixed[offset + 1] = (byte) ((output >>> 8) & 0xFF);
        }
        return mixed;
    }

    private void ensureScratchCapacity(int sampleCount) {
        if (exhaustSynthInput.length == sampleCount) {
            return;
        }
        exhaustSynthInput = new double[sampleCount];
        dryMix = new double[sampleCount];
        afterfireMix = new double[sampleCount];
        convolvedExhaust = new double[sampleCount];
    }

    private static double softLimitAfterfire(double value) {
        final double knee = 0.70;
        final double ceiling = 0.93;
        double magnitude = Math.abs(value);
        if (magnitude <= knee) {
            return value;
        }
        double compressed = knee + (ceiling - knee)
                * Math.tanh((magnitude - knee) / (ceiling - knee));
        return Math.copySign(compressed, value);
    }

    private void handleLifecycleTransition(EngineTelemetry telemetry) {
        if (telemetry.engineOn == previousEngineOn) {
            return;
        }
        previousEngineOn = telemetry.engineOn;
        lifecycleTime = 0.0;
        silent = false;
        if (telemetry.engineOn) {
            lifecycle = Lifecycle.STARTING;
            currentRpm = Math.min(currentRpm, profile.getStarterRpm() * 0.35);
            starterEngagementEnergy = 1.0;
        } else {
            lifecycle = Lifecycle.STOPPING;
            stopOriginRpm = Math.max(currentRpm, Math.max(telemetry.rpm, profile.getIdleRpm() * 0.8));
        }
    }

    private double advanceLifecycle(EngineTelemetry telemetry, double dt) {
        lifecycleTime += dt;
        switch (lifecycle) {
            case STARTING: {
                double progress = lifecycleTime / profile.getStartDurationSeconds();
                double compressionDrag = Dsp.clamp(Math.abs(lastCompressionRipple), 0.0, 1.0);
                double starterTarget = profile.getStarterRpm() * (0.94 - compressionDrag * 0.055);
                double spinUp = Dsp.smoothStep(progress / 0.14);
                double starterRpm = 32.0 + (starterTarget - 32.0) * spinUp;
                double ignition = Dsp.smoothStep((progress - 0.28) / 0.46);
                double catchBlend = Dsp.smoothStep((progress - 0.48) / 0.42);
                double runTarget = Math.max(profile.getIdleRpm(), telemetry.rpm);
                double catchFlare = profile.getIdleRpm() * 0.38 * Math.sin(Math.PI * catchBlend);
                currentRpm = starterRpm + (runTarget + catchFlare - starterRpm) * catchBlend;
                if (progress >= 1.0) {
                    lifecycle = Lifecycle.RUNNING;
                    lifecycleTime = 0.0;
                }
                return ignition;
            }
            case RUNNING: {
                idleWanderPhase = wrapPhase(idleWanderPhase + TWO_PI * 0.63 * dt);
                idleWanderPhase2 = wrapPhase(idleWanderPhase2 + TWO_PI * 1.17 * dt);
                double idleWander = Math.sin(idleWanderPhase) * 9.0
                        + Math.sin(idleWanderPhase2 + 0.71) * 4.5;
                double target = Math.max(profile.getIdleRpm(), telemetry.rpm) + idleWander;
                double smoothing = target > currentRpm ? rpmRiseSmoothing : rpmFallSmoothing;
                currentRpm += (target - currentRpm) * smoothing;
                return 1.0;
            }
            case STOPPING: {
                double progress = Dsp.clamp(lifecycleTime / profile.getStopDurationSeconds(), 0.0, 1.0);
                currentRpm = stopOriginRpm * Math.pow(1.0 - progress, 1.65);
                double ignition = Dsp.clamp(1.0 - progress * 5.5, 0.0, 1.0);
                if (progress >= 1.0) {
                    lifecycle = Lifecycle.OFF;
                    lifecycleTime = 0.0;
                    offTailTime = 0.0;
                    currentRpm = 0.0;
                }
                return ignition;
            }
            case OFF:
            default:
                currentRpm = 0.0;
                offTailTime += dt;
                if (offTailTime > 0.32) {
                    silent = true;
                }
                return 0.0;
        }
    }

    private double synthesizeStarter(double ignitionBlend, double dt, double gearNoise,
                                     double compressionRipple) {
        if (lifecycle != Lifecycle.STARTING) {
            return 0.0;
        }
        double progress = lifecycleTime / profile.getStartDurationSeconds();
        double engage = Dsp.smoothStep(progress / 0.055);
        double disengage = Dsp.smoothStep((progress - 0.66) / 0.24);
        double envelope = engage * (1.0 - disengage);
        double crankHz = Math.max(0.1, currentRpm / 60.0);
        double motorShaftHz = Math.max(18.0, crankHz * 12.4);
        double ringGearFrequency = Math.max(34.0, crankHz * 112.0);
        double motorFrequency = motorShaftHz * (10.0 + profile.getFiringPattern().getCylinderCount() * 0.18);
        starterMeshPhase = wrapPhase(starterMeshPhase + TWO_PI * ringGearFrequency * dt);
        starterMotorPhase = wrapPhase(starterMotorPhase + TWO_PI * motorFrequency * dt);

        double starterNoise = randomUnit() * 2.0 - 1.0;
        starterNoise = starterNoiseHighPass.process(starterNoiseLowPass.process(starterNoise));
        double housing = starterHousingResonator.process(starterNoise);
        double gearBody = starterGearResonator.process(starterNoise);
        double compressionModulation = 0.58
                + 0.42 * Dsp.clamp(Math.abs(compressionRipple) * 2.2, 0.0, 1.0);
        double electricalRipple = (Math.sin(starterMotorPhase) * 0.0065
                + Math.sin(starterMotorPhase * 2.03 + 0.23) * 0.0018)
                * compressionModulation;
        double gearMesh = (Math.sin(starterMeshPhase) * 0.0045
                + Math.sin(starterMeshPhase * 2.97 + 0.37) * 0.0014)
                * compressionModulation;
        double brushes = (starterNoise * 0.020 + housing * 0.055 + gearBody * 0.022)
                * (1.0 - ignitionBlend * 0.36);
        double crankLoading = -compressionRipple * 0.010 + gearNoise * 0.004;
        double engagement = (starterNoise * 0.055 + housing * 0.12)
                * starterEngagementEnergy;
        starterEngagementEnergy *= Math.exp(-dt / 0.055);
        return (electricalRipple + gearMesh + brushes + crankLoading) * envelope + engagement;
    }

    private void triggerAfterfireTransient(double eventEnergy) {
        afterfireCrackEnvelope = Math.min(2.2,
                afterfireCrackEnvelope + eventEnergy * 0.92);
        afterfirePressureEnvelope = Math.min(2.5,
                afterfirePressureEnvelope + eventEnergy * 1.18);
        afterfireRoarEnvelope = Math.min(2.3,
                afterfireRoarEnvelope + eventEnergy * 0.96);
        double polarity = randomUnit() < 0.5 ? -1.0 : 1.0;
        afterfireImpulse += polarity * eventEnergy;
        afterfirePressurePhase = polarity > 0.0 ? Math.PI * 0.5 : Math.PI * 1.5;
        afterfirePressureFrequency = 88.0 + randomUnit() * 38.0;
    }

    private double synthesizeAfterfireTransient(double noise, double dt) {
        if (afterfireController == null) {
            return 0.0;
        }
        double crackEnvelope = afterfireCrackEnvelope;
        double pressureEnvelope = afterfirePressureEnvelope;
        double roarEnvelope = afterfireRoarEnvelope;
        double impulse = afterfireImpulse;
        afterfireImpulse = 0.0;

        double crackNoise = afterfireNoiseHighPass.process(
                afterfireNoiseLowPass.process(noise));
        double roarNoise = afterfireRoarHighPass.process(
                afterfireRoarLowPass.process(noise));
        double pressureTargetHz = 48.0;
        afterfirePressureFrequency += (pressureTargetHz - afterfirePressureFrequency)
                * (1.0 - Math.exp(-dt / 0.070));
        afterfirePressurePhase = wrapPhase(afterfirePressurePhase
                + TWO_PI * afterfirePressureFrequency * dt);
        double pressureWave = (Math.sin(afterfirePressurePhase) * 0.72
                + Math.sin(afterfirePressurePhase * 2.0 + 0.63) * 0.19
                + Math.sin(afterfirePressurePhase * 3.0 + 1.28) * 0.09)
                * pressureEnvelope;
        double crack = afterfireCrackResonator.process(
                impulse * 18.0 + crackNoise * crackEnvelope * 1.15);
        double body = afterfireBodyResonator.process(
                impulse * 72.0 + roarNoise * pressureEnvelope * 1.6);
        afterfireCrackEnvelope *= Math.exp(-dt / 0.018);
        afterfirePressureEnvelope *= Math.exp(-dt / 0.145);
        afterfireRoarEnvelope *= Math.exp(-dt / 0.215);

        double core = impulse * 0.09
                + crackNoise * crackEnvelope * 6.50
                + crack * 1.60
                + body * 0.55
                + pressureWave * 0.40
                + roarNoise * roarEnvelope * 1.80;
        double earlyReflection = afterfireEarlyReflection.process(core);
        double lateReflection = afterfireLateReflection.process(core);
        return (core + earlyReflection * 0.30 + lateReflection * 0.18) * 0.18;
    }

    private void synchronize(EngineTelemetry telemetry) {
        previousEngineOn = telemetry.engineOn;
        lifecycle = telemetry.engineOn ? Lifecycle.RUNNING : Lifecycle.OFF;
        lifecycleTime = 0.0;
        offTailTime = 0.0;
        currentRpm = telemetry.engineOn ? Math.max(profile.getIdleRpm(), telemetry.rpm) : 0.0;
        lastTargetRpm = telemetry.rpm;
        smoothedThrottle = telemetry.throttle;
        smoothedLoad = telemetry.load;
        if (afterfireController != null) {
            afterfireController.synchronize(telemetry);
            afterfireCrackEnvelope = 0.0;
            afterfirePressureEnvelope = 0.0;
            afterfireRoarEnvelope = 0.0;
            afterfireImpulse = 0.0;
            afterfirePressurePhase = 0.0;
            afterfirePressureFrequency = 0.0;
            afterfireNoiseLowPass.reset();
            afterfireNoiseHighPass.reset();
            afterfireRoarLowPass.reset();
            afterfireRoarHighPass.reset();
            afterfireBodyResonator.reset();
            afterfireCrackResonator.reset();
            afterfireEarlyReflection.reset();
            afterfireLateReflection.reset();
        }
        if (dieselCombustionVoice != null) {
            dieselCombustionVoice.reset();
            dieselExhaustLowPass.reset();
        }
        streamFade = 0.0;
        silent = !telemetry.engineOn;
    }

    private double randomUnit() {
        int x = randomState;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        randomState = x;
        return (x & 0x7FFFFFFF) / (double) Integer.MAX_VALUE;
    }

    private static double wrapPhase(double phase) {
        if (phase >= TWO_PI) {
            phase %= TWO_PI;
        }
        return phase;
    }

    public boolean isSilent() {
        boolean powertrainSilent = alternativePowertrain == null ? silent : alternativePowertrain.isSilent();
        return powertrainSilent
                && (turbochargerVoice == null || turbochargerVoice.isSilent())
                && (rotorVoice == null || rotorVoice.isSilent())
                && (airBrakeVoice == null || airBrakeVoice.isSilent())
                && (brakeSquealVoice == null || brakeSquealVoice.isSilent())
                && (tireSquealVoice == null || tireSquealVoice.isSilent())
                && (hornVoice == null || hornVoice.isSilent())
                && (sirenVoice == null || sirenVoice.isSilent());
    }

    public float getActiveSignalDistance() {
        float distance = 0.0F;
        if (hornVoice != null && !hornVoice.isSilent()) {
            distance = Math.max(distance, hornVoice.getAudibleDistance());
        }
        if (sirenVoice != null && !sirenVoice.isSilent()) {
            distance = Math.max(distance, sirenVoice.getAudibleDistance());
        }
        return distance;
    }

    public EngineProfile getProfile() {
        return profile;
    }

    public void setFluidSubsteps(int substeps) {
        if (cylinderFlowModel != null) {
            cylinderFlowModel.setFluidSubsteps(substeps);
        }
    }
}
