package de.jarexception.advancedsoundaddon.sound;

import java.util.Arrays;

/** Simulates combustion-chamber and gas-flow pressure at audio rate. */
final class CylinderFlowModel {
    private static final double FULL_CYCLE_DEGREES = 720.0;
    private static final double FUEL_MOLAR_MASS = 0.100;
    private static final double FUEL_ENERGY_DENSITY = 48.1E6;
    private static final double MOLECULAR_AFR = 12.5;
    private static final double[] INTAKE_FLOW_K = flowTable(new double[]{
            0, 25, 75, 100, 130, 180, 190, 220, 240, 250, 260, 260, 260, 255, 250
    });
    private static final double[] EXHAUST_FLOW_K = flowTable(new double[]{
            0, 25, 50, 75, 100, 125, 160, 175, 180, 190, 200, 205, 210, 210, 210
    });

    private static final class Geometry {
        final double bore;
        final double stroke;
        final double rodLength;
        final double compressionRatio;
        final double intakeArea;
        final double exhaustArea;
        final double intakeRunnerLength;
        final double exhaustRunnerLength;
        final double collectorVolume;
        final double collectorArea;
        final double maximumIntakeLift;
        final double maximumExhaustLift;
        final double flowScale;

        Geometry(double bore, double stroke, double compressionRatio,
                 double intakeRunnerLength, double exhaustRunnerLength,
                 double collectorVolume, double maximumLift, double flowScale) {
            this.bore = bore;
            this.stroke = stroke;
            rodLength = stroke * 1.72;
            this.compressionRatio = compressionRatio;
            double boreArea = Math.PI * bore * bore * 0.25;
            intakeArea = boreArea * 0.33;
            exhaustArea = boreArea * 0.27;
            this.intakeRunnerLength = intakeRunnerLength;
            this.exhaustRunnerLength = exhaustRunnerLength;
            this.collectorVolume = collectorVolume;
            collectorArea = Math.PI * 0.056 * 0.056 * 0.25;
            maximumIntakeLift = maximumLift;
            maximumExhaustLift = maximumLift * 0.93;
            this.flowScale = flowScale;
        }

        Geometry withExhaustRunnerLength(double length) {
            return new Geometry(bore, stroke, compressionRatio,
                    intakeRunnerLength, length, collectorVolume,
                    maximumIntakeLift, flowScale);
        }

        Geometry withCompressionRatio(double ratio) {
            return new Geometry(bore, stroke, ratio,
                    intakeRunnerLength, exhaustRunnerLength, collectorVolume,
                    maximumIntakeLift, flowScale);
        }
    }

    private static final class FlameEvent {
        boolean lit;
        double lastVolume;
        double travelX;
        double travelY;
        double efficiency;
        double flameSpeed;
        EngineSimGasSystem.Mix eventMix = EngineSimGasSystem.Mix.AIR;
    }

    private static final class Cylinder {
        final EngineSimGasSystem chamber = new EngineSimGasSystem();
        final EngineSimGasSystem intakeRunner = new EngineSimGasSystem();
        final EngineSimGasSystem exhaustRunner = new EngineSimGasSystem();
        final FlameEvent flame = new FlameEvent();
        final Dsp.DelayLine acousticDelay;
        final int bank;
        final double camOffset;
        final double soundAttenuation;
        final double exhaustLength;
        double previousPhase;
        double previousIntakeLift;
        double previousExhaustLift;
        double intakeFlow;

        Cylinder(int bank, double camOffset, double soundAttenuation,
                 double exhaustLength, int delaySamples) {
            this.bank = bank;
            this.camOffset = camOffset;
            this.soundAttenuation = soundAttenuation;
            this.exhaustLength = exhaustLength;
            acousticDelay = new Dsp.DelayLine(delaySamples);
        }
    }

    private final EngineFiringPattern firingPattern;
    private final Geometry geometry;
    private final int cylinderCount;
    private final int bankCount;
    private final int physicsStride;
    private final double physicsDt;
    private final Cylinder[] cylinders;
    private final EngineSimGasSystem intakePlenum = new EngineSimGasSystem();
    private final EngineSimGasSystem[] collectors;
    private final EngineSimGasSystem atmosphere = new EngineSimGasSystem();
    private final EngineSimGasSystem crankcase = new EngineSimGasSystem();
    private final double[] previousBankSignal;
    private final double[] currentBankSignal;
    private final double[] bankSignal;
    private final double carburetorFlowK;
    private final double idleFlowK;
    private final double runnerFlowK;
    private final double primaryFlowK;
    private final double outletFlowK;
    private final double blowbyFlowK;
    private final double rotationalInertia;
    private final EngineSimGasSystem.Mix fuelAirMix;
    private final boolean compressionIgnition;

    private int audioSubstep;
    private volatile int fluidSubsteps = 8;
    private double cycleDegrees = 17.0;
    private double intakeFlow;
    private double previousIntakeFlow;
    private double currentIntakeFlow;
    private double valveActivity;
    private double previousValveActivity;
    private double currentValveActivity;
    private double torqueRipple;
    private double previousTorqueRipple;
    private double currentTorqueRipple;
    private double compressionRipple;
    private double previousCompressionRipple;
    private double currentCompressionRipple;
    private int lastFiredCylinder;
    private double meanGasTorque;
    private double crankSpeedDeviation;
    private int randomState = 0x614A93C7;

    CylinderFlowModel(EngineLayout layout, int sampleRate) {
        this(layout, sampleRate, 0.0);
    }

    CylinderFlowModel(EngineLayout layout, int sampleRate, double exhaustResonanceHz) {
        this(EngineProfile.forLayout(layout), sampleRate, exhaustResonanceHz);
    }

    CylinderFlowModel(EngineProfile profile, int sampleRate, double exhaustResonanceHz) {
        firingPattern = profile.getFiringPattern();
        EngineLayout layout = profile.getLayout();
        Geometry baseGeometry = geometry(layout);
        compressionIgnition = profile.isCompressionIgnition();
        if (compressionIgnition) {
            baseGeometry = baseGeometry.withCompressionRatio(dieselCompressionRatio(layout));
        }
        geometry = exhaustResonanceHz > 20.0
                ? baseGeometry.withExhaustRunnerLength(Dsp.clamp(
                343.0 / (4.0 * exhaustResonanceHz), 0.42, 0.86))
                : baseGeometry;
        cylinderCount = firingPattern.getCylinderCount();
        bankCount = firingPattern.getBankCount();
        int targetPhysicsRate = cylinderCount >= 16 ? 8_000
                : (cylinderCount >= 12 ? 9_600 : 12_000);
        physicsStride = Math.max(1, (int) Math.round(sampleRate / (double) targetPhysicsRate));
        physicsDt = physicsStride / (double) sampleRate;
        collectors = new EngineSimGasSystem[bankCount];
        previousBankSignal = new double[bankCount];
        currentBankSignal = new double[bankCount];
        bankSignal = new double[bankCount];

        double idealAfr = 0.8 * MOLECULAR_AFR * 4.0;
        double airFraction = idealAfr / (1.0 + idealAfr);
        fuelAirMix = new EngineSimGasSystem.Mix(1.0 - airFraction,
                airFraction * 0.75, airFraction * 0.25);
        carburetorFlowK = EngineSimGasSystem.kCarb(82.0 * cylinderCount);
        idleFlowK = EngineSimGasSystem.kCarb(Math.max(1.0, cylinderCount * 0.22));
        runnerFlowK = EngineSimGasSystem.kCarb(280.0 * geometry.flowScale);
        primaryFlowK = EngineSimGasSystem.kCarb(125.0 * geometry.flowScale);
        outletFlowK = EngineSimGasSystem.kCarb(1_000.0 * Math.max(1.0, bankCount));
        blowbyFlowK = EngineSimGasSystem.k28InH2O(0.16);
        rotationalInertia = (0.070 + cylinderCount * 0.014)
                * (compressionIgnition ? 1.22 : 1.0);

        double plenumVolume = 0.00145 + cylinderCount * 0.00011;
        double plenumArea = 0.0085;
        intakePlenum.initialize(EngineSimGasSystem.ATMOSPHERE, plenumVolume,
                EngineSimGasSystem.ROOM_TEMPERATURE, fuelAirMix);
        intakePlenum.setGeometry(plenumVolume / plenumArea, Math.sqrt(plenumArea));
        atmosphere.initialize(EngineSimGasSystem.ATMOSPHERE, 1_000.0,
                EngineSimGasSystem.ROOM_TEMPERATURE, fuelAirMix);
        atmosphere.setGeometry(100.0, 100.0);
        crankcase.initialize(EngineSimGasSystem.ATMOSPHERE, 1_000.0,
                EngineSimGasSystem.ROOM_TEMPERATURE, EngineSimGasSystem.Mix.AIR);
        crankcase.setGeometry(100.0, 100.0);
        for (int bank = 0; bank < bankCount; bank++) {
            collectors[bank] = new EngineSimGasSystem();
            collectors[bank].initialize(EngineSimGasSystem.ATMOSPHERE,
                    geometry.collectorVolume / bankCount,
                    EngineSimGasSystem.ROOM_TEMPERATURE, EngineSimGasSystem.Mix.EXHAUST);
            collectors[bank].setGeometry(geometry.collectorVolume / bankCount / geometry.collectorArea,
                    Math.sqrt(geometry.collectorArea));
        }

        cylinders = new Cylinder[cylinderCount];
        double area = pistonArea();
        double swept = area * geometry.stroke;
        double clearance = swept / (geometry.compressionRatio - 1.0);
        for (int cylinderIndex = 0; cylinderIndex < cylinderCount; cylinderIndex++) {
            int bank = firingPattern.getBankForEvent(cylinderIndex);
            double tolerance = deterministicUnit(cylinderIndex * 97L + 47L) - 0.5;
            double exhaustLength = geometry.exhaustRunnerLength + tolerance * 0.055;
            int delaySamples = Math.max(1, (int) Math.round(exhaustLength / 343.0
                    * sampleRate / physicsStride));
            Cylinder cylinder = new Cylinder(bank, tolerance * 3.2,
                    0.965 + deterministicUnit(cylinderIndex * 43L + 11L) * 0.07,
                    exhaustLength, delaySamples);
            double phase = phaseForCylinder(cylinderIndex);
            cylinder.previousPhase = phase;
            double chamberVolume = clearance + swept * pistonTravelFraction(phase);
            cylinder.chamber.initialize(EngineSimGasSystem.ATMOSPHERE, chamberVolume,
                    EngineSimGasSystem.ROOM_TEMPERATURE, fuelAirMix);
            cylinder.chamber.setGeometry(chamberVolume / area, Math.sqrt(area));

            double intakeVolume = geometry.intakeArea * geometry.intakeRunnerLength + 0.000105;
            cylinder.intakeRunner.initialize(EngineSimGasSystem.ATMOSPHERE, intakeVolume,
                    EngineSimGasSystem.ROOM_TEMPERATURE, fuelAirMix);
            cylinder.intakeRunner.setGeometry(intakeVolume / geometry.intakeArea,
                    Math.sqrt(geometry.intakeArea));

            double exhaustVolume = geometry.exhaustArea * exhaustLength + 0.000095;
            cylinder.exhaustRunner.initialize(EngineSimGasSystem.ATMOSPHERE, exhaustVolume,
                    EngineSimGasSystem.ROOM_TEMPERATURE, EngineSimGasSystem.Mix.EXHAUST);
            cylinder.exhaustRunner.setGeometry(exhaustVolume / geometry.exhaustArea,
                    Math.sqrt(geometry.exhaustArea));
            cylinders[cylinderIndex] = cylinder;
        }
    }

    void step(double rpm, double throttle, double load, double ignitionBlend,
              boolean limiterCut, boolean starting) {
        if (audioSubstep == 0) {
            System.arraycopy(currentBankSignal, 0, previousBankSignal, 0, bankCount);
            previousIntakeFlow = currentIntakeFlow;
            previousValveActivity = currentValveActivity;
            previousTorqueRipple = currentTorqueRipple;
            previousCompressionRipple = currentCompressionRipple;
            simulatePhysics(rpm, throttle, load, ignitionBlend, limiterCut, starting);
        }
        double interpolation = (audioSubstep + 1.0) / physicsStride;
        for (int bank = 0; bank < bankCount; bank++) {
            bankSignal[bank] = previousBankSignal[bank]
                    + (currentBankSignal[bank] - previousBankSignal[bank]) * interpolation;
        }
        intakeFlow = lerp(previousIntakeFlow, currentIntakeFlow, interpolation);
        valveActivity = lerp(previousValveActivity, currentValveActivity, interpolation);
        torqueRipple = lerp(previousTorqueRipple, currentTorqueRipple, interpolation);
        compressionRipple = lerp(previousCompressionRipple, currentCompressionRipple, interpolation);
        audioSubstep = (audioSubstep + 1) % physicsStride;
    }

    private void simulatePhysics(double rpm, double throttle, double load,
                                 double ignitionBlend, boolean limiterCut, boolean starting) {
        for (Cylinder cylinder : cylinders) {
            cylinder.intakeFlow = 0.0;
        }
        double targetAngularVelocity = Math.max(0.0, rpm) * Math.PI / 30.0;
        double instantaneousAngularVelocity = Math.max(0.0,
                targetAngularVelocity + crankSpeedDeviation);
        double degreesPerStep = instantaneousAngularVelocity
                * (180.0 / Math.PI) * physicsDt;
        cycleDegrees = wrapDegrees(cycleDegrees + degreesPerStep);
        double ignitionAdvance = compressionIgnition
                ? 6.0 + Dsp.clamp(rpm / 4_800.0, 0.0, 1.0) * 13.0
                : 9.0 + Dsp.clamp(rpm / 7_000.0, 0.0, 1.0) * 24.0;
        double sparkAngle = FULL_CYCLE_DEGREES - ignitionAdvance;
        double area = pistonArea();
        double swept = area * geometry.stroke;
        double clearance = swept / (geometry.compressionRatio - 1.0);
        currentValveActivity = 0.0;
        currentTorqueRipple = 0.0;
        currentCompressionRipple = 0.0;
        double gasTorque = 0.0;

        for (int index = 0; index < cylinderCount; index++) {
            Cylinder cylinder = cylinders[index];
            double phase = phaseForCylinder(index);
            cylinder.chamber.setVolume(clearance + swept * pistonTravelFraction(phase));
            if (crossedAngle(cylinder.previousPhase, phase, sparkAngle, degreesPerStep)) {
                boolean fires = ignitionBlend > 0.001 && !limiterCut;
                if (fires && starting) {
                    fires = randomUnit() < 0.10 + 0.90 * Dsp.smoothStep(ignitionBlend);
                }
                if (fires) {
                    ignite(cylinder, rpm, load, ignitionBlend, starting);
                    lastFiredCylinder = index;
                }
            }

            double valvePhase = wrapDegrees(phase + cylinder.camOffset);
            double intakeLift = camLift(valvePhase, 342.0, 596.0, geometry.maximumIntakeLift);
            double exhaustLift = camLift(valvePhase, 128.0, 390.0, geometry.maximumExhaustLift);
            currentValveActivity += Math.abs(intakeLift - cylinder.previousIntakeLift)
                    / geometry.maximumIntakeLift
                    + Math.abs(exhaustLift - cylinder.previousExhaustLift)
                    / geometry.maximumExhaustLift;
            cylinder.previousIntakeLift = intakeLift;
            cylinder.previousExhaustLift = exhaustLift;
            cylinder.previousPhase = phase;

            double pressureDelta = cylinder.chamber.pressure() - EngineSimGasSystem.ATMOSPHERE;
            double crankAngle = Math.toRadians(phase % 360.0);
            gasTorque += pressureDelta * area * geometry.stroke * 0.5 * Math.sin(crankAngle);
            if (phase < 180.0) {
                currentTorqueRipple += pressureDelta / 1.0E6
                        * Math.sin(Math.PI * phase / 180.0);
            } else if (phase > 540.0) {
                currentCompressionRipple += Math.max(0.0, pressureDelta) / 1.0E6
                        * Math.sin(Math.PI * (phase - 540.0) / 180.0);
            }
        }

        double meanTorqueAlpha = 1.0 - Math.exp(-physicsDt / 0.18);
        meanGasTorque += (gasTorque - meanGasTorque) * meanTorqueAlpha;
        crankSpeedDeviation += (gasTorque - meanGasTorque) / rotationalInertia * physicsDt;
        crankSpeedDeviation *= Math.exp(-physicsDt / 0.070);
        double rpmFraction = Dsp.clamp(rpm / 7_000.0, 0.0, 1.0);
        double maximumDeviation = targetAngularVelocity * (0.085 - rpmFraction * 0.055);
        crankSpeedDeviation = Dsp.clamp(crankSpeedDeviation,
                -maximumDeviation, maximumDeviation);

        int activeFluidSubsteps = fluidSubsteps;
        double fluidDt = physicsDt / activeFluidSubsteps;
        double effectiveThrottle = Dsp.clamp(Math.max(throttle, load * 0.035), 0.0, 1.0);
        for (int substep = 0; substep < activeFluidSubsteps; substep++) {
            processIntake(fluidDt, effectiveThrottle);
            processCollectors(fluidDt);
            for (int index = 0; index < cylinderCount; index++) {
                processCylinder(cylinders[index], index, fluidDt);
            }
        }

        Arrays.fill(currentBankSignal, 0.0);
        double attenuation = Math.min(Math.abs(rpm * Math.PI / 30.0), 40.0) / 40.0;
        attenuation = attenuation * attenuation * attenuation;
        double totalIntake = 0.0;
        for (Cylinder cylinder : cylinders) {
            double pressureSignal = attenuation * 1_600.0 * (
                    cylinder.exhaustRunner.pressure() - EngineSimGasSystem.ATMOSPHERE
                            + 0.1 * cylinder.exhaustRunner.dynamicPressure(1.0)
                            + 0.1 * cylinder.exhaustRunner.dynamicPressure(-1.0));
            double delayed = cylinder.acousticDelay.process(pressureSignal);
            currentBankSignal[cylinder.bank] += cylinder.soundAttenuation
                    * delayed / cylinderCount / (cylinder.exhaustLength * cylinder.exhaustLength);
            totalIntake += cylinder.intakeFlow / physicsDt;
        }
        for (int bank = 0; bank < bankCount; bank++) {
            currentBankSignal[bank] /= 32_768.0;
        }
        currentIntakeFlow = Dsp.clamp(totalIntake * 0.022 / cylinderCount, -1.5, 1.5);
        currentValveActivity /= cylinderCount;
        currentTorqueRipple /= cylinderCount;
        currentCompressionRipple /= cylinderCount;
    }

    private void processIntake(double dt, double throttle) {
        double platePosition = 0.975 * (1.0 - throttle);
        double flowAttenuation = Math.cos(platePosition * Math.PI * 0.5);
        atmosphere.reset(EngineSimGasSystem.ATMOSPHERE,
                EngineSimGasSystem.ROOM_TEMPERATURE, fuelAirMix);
        atmosphere.flowTo(intakePlenum, carburetorFlowK * flowAttenuation, dt, 10.0, 0.0085);
        atmosphere.reset(EngineSimGasSystem.ATMOSPHERE,
                EngineSimGasSystem.ROOM_TEMPERATURE, fuelAirMix);
        atmosphere.flowTo(intakePlenum, idleFlowK, dt, 10.0, 0.0085);
        intakePlenum.dissipateExcessVelocity();
        intakePlenum.updateVelocity(dt, 0.18);
    }

    private void processCollectors(double dt) {
        for (EngineSimGasSystem collector : collectors) {
            atmosphere.reset(EngineSimGasSystem.ATMOSPHERE,
                    EngineSimGasSystem.ROOM_TEMPERATURE, EngineSimGasSystem.Mix.EXHAUST);
            collector.flowTo(atmosphere, outletFlowK, dt, geometry.collectorArea, 10.0);
            collector.dissipateExcessVelocity();
            collector.updateVelocity(dt, 0.72);
        }
    }

    private void processCylinder(Cylinder cylinder, int index, double dt) {
        double phase = phaseForCylinder(index);
        double valvePhase = wrapDegrees(phase + cylinder.camOffset);
        double intakeLift = camLift(valvePhase, 342.0, 596.0, geometry.maximumIntakeLift);
        double exhaustLift = camLift(valvePhase, 128.0, 390.0, geometry.maximumExhaustLift);
        double intakePortK = interpolateFlowK(INTAKE_FLOW_K,
                intakeLift / geometry.maximumIntakeLift) * geometry.flowScale;
        double exhaustPortK = interpolateFlowK(EXHAUST_FLOW_K,
                exhaustLift / geometry.maximumExhaustLift) * geometry.flowScale;

        double chamberHeight = Math.max(1.0E-5, cylinder.chamber.volume() / pistonArea());
        double cylinderSurface = chamberHeight * Math.PI * geometry.bore + pistonArea() * 2.0;
        cylinder.chamber.changeEnergy((363.15 - cylinder.chamber.temperature())
                * cylinderSurface * 100.0 * dt);
        crankcase.reset(EngineSimGasSystem.ATMOSPHERE,
                EngineSimGasSystem.ROOM_TEMPERATURE, EngineSimGasSystem.Mix.AIR);
        cylinder.chamber.flowTo(crankcase, blowbyFlowK, dt, pistonArea(), 10.0);

        intakePlenum.flowTo(cylinder.intakeRunner, runnerFlowK, dt, 0.0085, geometry.intakeArea);
        cylinder.intakeRunner.dissipateExcessVelocity();
        double intakeTransfer = cylinder.intakeRunner.flowTo(cylinder.chamber,
                intakePortK, dt, geometry.intakeArea, pistonArea());
        cylinder.intakeRunner.dissipateExcessVelocity();
        cylinder.chamber.dissipateExcessVelocity();
        cylinder.chamber.flowTo(cylinder.exhaustRunner, exhaustPortK, dt,
                pistonArea(), geometry.exhaustArea);
        cylinder.chamber.dissipateExcessVelocity();
        cylinder.exhaustRunner.dissipateExcessVelocity();
        cylinder.exhaustRunner.flowTo(collectors[cylinder.bank], primaryFlowK, dt,
                geometry.exhaustArea, geometry.collectorArea);

        cylinder.intakeRunner.updateVelocity(dt, 0.20);
        cylinder.chamber.updateVelocity(dt, 0.5);
        cylinder.exhaustRunner.updateVelocity(dt, 0.70);
        cylinder.intakeFlow += intakeTransfer;
        if (Math.abs(intakeTransfer) > 1.0E-9 && cylinder.flame.lit) {
            cylinder.flame.lit = false;
        }
        propagateFlame(cylinder, dt);
    }

    private void ignite(Cylinder cylinder, double rpm, double load,
                        double ignitionBlend, boolean starting) {
        EngineSimGasSystem.Mix mix = cylinder.chamber.mix();
        if (mix.fuel <= 1.0E-8) {
            return;
        }
        double afr = mix.oxygen / mix.fuel;
        double equivalenceRatio = afr / MOLECULAR_AFR;
        if (equivalenceRatio < 0.5 || equivalenceRatio > 1.9) {
            return;
        }
        double turbulence = geometry.stroke * rpm / 60.0;
        double dilution = Math.max(0.0, mix.inert / Math.max(1.0E-8, mix.oxygen / 0.7) - 1.0);
        double mixingFactor = 1.0 - Dsp.clamp(turbulence / 4.0, 0.0, 1.0)
                * Dsp.clamp(1.0 - dilution / 10.0, 0.0, 1.0);
        double randomEfficiency = 0.6 * (0.5 + 0.5 * randomUnit());
        double efficiency = (mixingFactor * randomEfficiency + 1.0 - mixingFactor) * 0.8;
        efficiency *= 0.90 + 0.10 * load;
        if (compressionIgnition) {
            efficiency *= 0.96 + 0.04 * load;
        }
        if (starting) {
            efficiency *= 0.55 + 0.45 * ignitionBlend;
        }

        double laminarSpeed = laminarBurningVelocity(afr,
                cylinder.chamber.temperature(), cylinder.chamber.pressure());
        double flameRatio = turbulence < 5.0 ? 3.0 + turbulence * 0.9 : turbulence * 1.5;
        if (compressionIgnition) {
            flameRatio *= 1.55;
        }
        FlameEvent flame = cylinder.flame;
        flame.lit = true;
        flame.lastVolume = cylinder.chamber.volume();
        flame.travelX = 0.0;
        flame.travelY = 0.0;
        flame.efficiency = Dsp.clamp(efficiency, 0.15, 0.88);
        flame.flameSpeed = Math.max(0.25, laminarSpeed * flameRatio);
        flame.eventMix = mix;
    }

    private void propagateFlame(Cylinder cylinder, double dt) {
        FlameEvent flame = cylinder.flame;
        if (!flame.lit) {
            return;
        }
        double volume = cylinder.chamber.volume();
        double totalTravelX = geometry.bore * 0.5;
        double totalTravelY = volume / pistonArea();
        double expansion = volume / Math.max(1.0E-12, flame.lastVolume);
        double lastX = flame.travelX;
        double lastY = flame.travelY * expansion;
        flame.travelX = Math.min(lastX + dt * flame.flameSpeed, totalTravelX);
        flame.travelY = Math.min(lastY + dt * flame.flameSpeed, totalTravelY);
        if (lastX < flame.travelX || lastY < flame.travelY) {
            double burnedVolume = flame.travelX * flame.travelX * Math.PI * flame.travelY;
            double previousBurnedVolume = lastX * lastX * Math.PI * lastY;
            double litVolume = Math.max(0.0, burnedVolume - previousBurnedVolume);
            double reactingMoles = litVolume / volume * cylinder.chamber.moles();
            double fuelBurned = cylinder.chamber.react(reactingMoles * flame.efficiency, flame.eventMix);
            cylinder.chamber.changeEnergy(fuelBurned * FUEL_MOLAR_MASS * FUEL_ENERGY_DENSITY);
        } else {
            flame.lit = false;
        }
        flame.lastVolume = volume;
    }

    void injectAfterfire(double energy) {
        cylinders[lastFiredCylinder].exhaustRunner.changeEnergy(Math.max(0.0, energy) * 72.0);
    }

    double getBankFlow(int bank) { return bankSignal[bank]; }
    double getIntakeFlow() { return intakeFlow; }
    double getValveActivity() { return valveActivity; }
    double getTorqueRipple() { return torqueRipple; }
    double getCompressionRipple() { return compressionRipple; }

    void setFluidSubsteps(int substeps) {
        fluidSubsteps = Math.max(4, Math.min(8, substeps));
    }

    private double phaseForCylinder(int firingEvent) {
        return wrapDegrees(cycleDegrees - firingEvent * FULL_CYCLE_DEGREES / cylinderCount);
    }

    private double pistonArea() {
        return Math.PI * geometry.bore * geometry.bore * 0.25;
    }

    private double pistonTravelFraction(double phase) {
        double angle = Math.toRadians(phase % 360.0);
        double radius = geometry.stroke * 0.5;
        double sin = Math.sin(angle);
        double travel = radius * (1.0 - Math.cos(angle)) + geometry.rodLength
                - Math.sqrt(Math.max(1.0E-12, geometry.rodLength * geometry.rodLength
                - radius * radius * sin * sin));
        return Dsp.clamp(travel / geometry.stroke, 0.0, 1.0);
    }

    private static double camLift(double phase, double open, double close, double maximumLift) {
        if (phase < open || phase > close) return 0.0;
        double sine = Math.sin(Math.PI * (phase - open) / (close - open));
        return maximumLift * sine * sine;
    }

    private static double laminarBurningVelocity(double afr, double temperature, double pressure) {
        double equivalence = afr / MOLECULAR_AFR;
        double base = 0.305 - 0.549 * (equivalence - 1.21) * (equivalence - 1.21);
        double alpha = 2.4 - 0.271 * Math.pow(equivalence, 3.51);
        double beta = -0.357 + 0.14 * Math.pow(equivalence, 2.77);
        return Math.max(0.04, base * Math.pow(Math.max(0.2, temperature / 298.0), alpha)
                * Math.pow(Math.max(0.1, pressure / EngineSimGasSystem.ATMOSPHERE), beta));
    }

    private static boolean crossedAngle(double previous, double current, double target, double advance) {
        if (advance <= 0.0) return false;
        double travelled = wrapDegrees(current - previous);
        double toTarget = wrapDegrees(target - previous);
        return toTarget > 0.0 && toTarget <= travelled + 1.0E-9;
    }

    private static double interpolateFlowK(double[] table, double liftFraction) {
        double position = Dsp.clamp(liftFraction, 0.0, 1.0) * (table.length - 1);
        int lower = Math.min(table.length - 1, (int) position);
        int upper = Math.min(table.length - 1, lower + 1);
        return table[lower] + (table[upper] - table[lower]) * (position - lower);
    }

    private static double[] flowTable(double[] cfm) {
        double[] result = new double[cfm.length];
        for (int i = 0; i < cfm.length; i++) result[i] = EngineSimGasSystem.k28InH2O(cfm[i]);
        return result;
    }

    private double randomUnit() {
        int x = randomState;
        x ^= x << 13;
        x ^= x >>> 17;
        x ^= x << 5;
        randomState = x;
        return (x & 0x7FFFFFFF) / (double) Integer.MAX_VALUE;
    }

    private static double deterministicUnit(long seed) {
        long value = seed * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        return (value & 0x7FFFFFFFFFFFFFFFL) / (double) Long.MAX_VALUE;
    }

    private static Geometry geometry(EngineLayout layout) {
        switch (layout) {
            case I1: return new Geometry(0.0740, 0.0580, 10.2, 0.17, 0.49, 0.004, 0.0092, 0.78);
            case I3: return new Geometry(0.0750, 0.0840, 10.5, 0.19, 0.55, 0.008, 0.0098, 0.90);
            case I5: return new Geometry(0.0825, 0.0928, 10.4, 0.20, 0.66, 0.011, 0.0102, 0.98);
            case I6: return new Geometry(0.0840, 0.0896, 10.6, 0.21, 0.70, 0.012, 0.0100, 1.00);
            case V6: return new Geometry(0.0860, 0.0860, 10.8, 0.19, 0.60, 0.012, 0.0105, 1.02);
            case FLAT6: return new Geometry(0.1020, 0.0764, 12.5, 0.18, 0.61, 0.014, 0.0116, 1.08);
            case V8_CROSSPLANE: return new Geometry(0.1016, 0.0920, 10.2, 0.18, 0.76, 0.018, 0.0110, 1.10);
            case V8_FLATPLANE: return new Geometry(0.0940, 0.0720, 11.8, 0.17, 0.57, 0.014, 0.0118, 1.08);
            case V10: return new Geometry(0.0845, 0.0928, 12.0, 0.18, 0.61, 0.016, 0.0115, 1.04);
            case V12: return new Geometry(0.0890, 0.0800, 11.5, 0.20, 0.68, 0.018, 0.0108, 1.00);
            case W16: return new Geometry(0.0860, 0.0860, 9.0, 0.18, 0.64, 0.022, 0.0104, 1.06);
            case I4:
            default: return new Geometry(0.0860, 0.0860, 10.7, 0.18, 0.58, 0.010, 0.0106, 1.00);
        }
    }

    private static double dieselCompressionRatio(EngineLayout layout) {
        switch (layout) {
            case I6:
                return 17.0;
            case V8_CROSSPLANE:
                return 16.2;
            case I4:
            default:
                return 16.5;
        }
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static double wrapDegrees(double degrees) {
        degrees %= FULL_CYCLE_DEGREES;
        return degrees < 0.0 ? degrees + FULL_CYCLE_DEGREES : degrees;
    }
}
