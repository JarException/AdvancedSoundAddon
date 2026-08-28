package de.jarexception.advancedsoundaddon.sound;

/** Simulates one-dimensional compressible gas flow in SI units. */
final class EngineSimGasSystem {
    static final double ATMOSPHERE = 101_325.0;
    static final double ROOM_TEMPERATURE = 298.15;

    private static final double GAS_CONSTANT = 8.31446261815324;
    private static final double AIR_MOLAR_MASS = 0.02897;
    private static final int DEGREES_OF_FREEDOM = 5;
    private static final double HEAT_CAPACITY_RATIO = 1.4;
    private static final double CHOKED_FLOW_LIMIT = Math.pow(2.0 / 2.4, 3.5);
    private static final double CHOKED_FLOW_RATE = Math.sqrt(HEAT_CAPACITY_RATIO)
            * Math.pow(2.0 / 2.4, 3.0);

    static final class Mix {
        static final Mix AIR = new Mix(0.0, 0.75, 0.25);
        static final Mix EXHAUST = new Mix(0.0, 1.0, 0.0);

        final double fuel;
        final double inert;
        final double oxygen;

        Mix(double fuel, double inert, double oxygen) {
            this.fuel = fuel;
            this.inert = inert;
            this.oxygen = oxygen;
        }
    }

    private double moles;
    private double thermalEnergy;
    private double volume;
    private double momentum;
    private double fuelFraction;
    private double inertFraction;
    private double oxygenFraction;
    private double length = 0.1;
    private double width = 0.03;

    void initialize(double pressure, double newVolume, double temperature, Mix mix) {
        volume = Math.max(1.0E-9, newVolume);
        moles = pressure * volume / (GAS_CONSTANT * temperature);
        thermalEnergy = temperature * 0.5 * DEGREES_OF_FREEDOM * moles * GAS_CONSTANT;
        fuelFraction = mix.fuel;
        inertFraction = mix.inert;
        oxygenFraction = mix.oxygen;
        momentum = 0.0;
    }

    void reset(double pressure, double temperature, Mix mix) {
        initialize(pressure, volume, temperature, mix);
    }

    void setGeometry(double newLength, double newWidth) {
        length = Math.max(1.0E-5, newLength);
        width = Math.max(1.0E-5, newWidth);
    }

    void setVolume(double newVolume) {
        newVolume = Math.max(1.0E-9, newVolume);
        double deltaVolume = newVolume - volume;
        thermalEnergy -= pressure() * deltaVolume;
        volume = newVolume;
        sanitize();
    }

    void changeEnergy(double energy) {
        thermalEnergy += energy;
        sanitize();
    }

    double flowTo(EngineSimGasSystem other, double flowConstant, double dt,
                  double ownCrossSection, double otherCrossSection) {
        if (flowConstant <= 0.0 || dt <= 0.0) {
            return 0.0;
        }

        double pressure0 = pressure() + dynamicPressure(1.0);
        double pressure1 = other.pressure() + other.dynamicPressure(-1.0);
        EngineSimGasSystem source;
        EngineSimGasSystem sink;
        double sourcePressure;
        double sinkPressure;
        double sourceArea;
        double sinkArea;
        double direction;
        if (pressure0 > pressure1) {
            source = this;
            sink = other;
            sourcePressure = pressure0;
            sinkPressure = pressure1;
            sourceArea = ownCrossSection;
            sinkArea = otherCrossSection;
            direction = 1.0;
        } else {
            source = other;
            sink = this;
            sourcePressure = pressure1;
            sinkPressure = pressure0;
            sourceArea = otherCrossSection;
            sinkArea = ownCrossSection;
            direction = -1.0;
        }

        if (source.moles <= 1.0E-14 || sourcePressure <= 1.0 || sinkPressure <= 0.0) {
            return 0.0;
        }
        double transferred = dt * flowRate(flowConstant, sourcePressure, sinkPressure,
                source.temperature(), HEAT_CAPACITY_RATIO);
        transferred = Dsp.clamp(transferred, 0.0, 0.9 * source.moles);
        if (transferred <= 1.0E-16) {
            return 0.0;
        }

        double sourceMoles0 = source.moles;
        double fraction = transferred / sourceMoles0;
        double fractionVolume = fraction * source.volume;
        double fractionMass = fraction * source.mass();

        double sourceBulk0 = source.bulkKineticEnergy();
        double sinkBulk0 = sink.bulkKineticEnergy();
        double energyPerMole = source.thermalEnergyPerMole();
        Mix sourceMix = source.mix();
        sink.gainMoles(transferred, energyPerMole, sourceMix);
        source.loseMoles(transferred, energyPerMole);

        double carriedMomentum = source.momentum * fraction;
        source.momentum -= carriedMomentum;
        sink.momentum += carriedMomentum;
        sink.thermalEnergy -= (source.bulkKineticEnergy() + sink.bulkKineticEnergy())
                - (sourceBulk0 + sinkBulk0);

        double sourceMomentum0 = source.momentum;
        double sinkMomentum0 = sink.momentum;
        if (sinkArea > 0.0) {
            double velocity = Dsp.clamp(fractionVolume / sinkArea / dt, 0.0, sink.speedOfSound());
            sink.momentum += velocity * direction * fractionMass;
        }
        if (sourceArea > 0.0 && source.mass() > 0.0) {
            double velocity = Dsp.clamp(fractionVolume / sourceArea / dt, 0.0, source.speedOfSound());
            source.momentum += velocity * direction * fractionMass;
        }
        source.conserveBulkEnergy(sourceMomentum0);
        sink.conserveBulkEnergy(sinkMomentum0);
        source.sanitize();
        sink.sanitize();
        return transferred * direction;
    }

    void dissipateExcessVelocity() {
        double speed = Math.abs(velocity());
        double sound = speedOfSound();
        if (speed <= sound || speed == 0.0) {
            return;
        }
        double oldBulk = bulkKineticEnergy();
        momentum *= sound / speed;
        thermalEnergy += oldBulk - bulkKineticEnergy();
        sanitize();
    }

    void updateVelocity(double dt, double beta) {
        if (moles <= 1.0E-14) {
            return;
        }
        double depth = volume / (length * width);
        double pressureSurfaceForward = dynamicPressure(1.0) * width * depth;
        double pressureSurfaceBackward = dynamicPressure(-1.0) * width * depth;
        double oldBulk = bulkKineticEnergy();
        momentum -= (pressureSurfaceForward - pressureSurfaceBackward) * dt * beta;
        thermalEnergy += oldBulk - bulkKineticEnergy();
        sanitize();
    }

    void dissipateVelocity(double dt, double timeConstant) {
        if (moles <= 1.0E-14) {
            return;
        }
        double oldBulk = bulkKineticEnergy();
        double scale = 1.0 - dt / (dt + Math.max(1.0E-8, timeConstant));
        momentum *= scale;
        thermalEnergy += oldBulk - bulkKineticEnergy();
        sanitize();
    }

    double react(double requestedMoles, Mix eventMix) {
        requestedMoles = Math.max(0.0, requestedMoles);
        double requestedFuel = eventMix.fuel * requestedMoles;
        double requestedOxygen = eventMix.oxygen * requestedMoles;
        double systemFuel = fuelFraction * moles;
        double systemOxygen = oxygenFraction * moles;
        double systemInert = inertFraction * moles;
        double actualFuel = Math.min(Math.min(systemFuel, requestedFuel), (2.0 / 25.0) * requestedOxygen);
        double actualOxygen = Math.min(Math.min(systemOxygen, requestedOxygen), (25.0 / 2.0) * requestedFuel);
        double reactants = actualFuel + actualOxygen;
        double products = (34.0 / 27.0) * reactants;
        double nextMoles = moles + products - reactants;
        if (nextMoles > 1.0E-14) {
            fuelFraction = (systemFuel - actualFuel) / nextMoles;
            oxygenFraction = (systemOxygen - actualOxygen) / nextMoles;
            inertFraction = (systemInert + products) / nextMoles;
            moles = nextMoles;
        }
        return actualFuel;
    }

    double pressure() {
        return volume > 0.0 ? thermalEnergy / (0.5 * DEGREES_OF_FREEDOM * volume) : 0.0;
    }

    double temperature() {
        return moles > 0.0
                ? thermalEnergy / (0.5 * DEGREES_OF_FREEDOM * moles * GAS_CONSTANT) : 0.0;
    }

    double dynamicPressure(double direction) {
        if (moles <= 1.0E-14 || thermalEnergy <= 0.0) {
            return 0.0;
        }
        double directedVelocity = direction * velocity();
        if (directedVelocity <= 0.0) {
            return 0.0;
        }
        double soundSquared = pressure() * HEAT_CAPACITY_RATIO / approximateDensity();
        if (soundSquared <= 0.0) {
            return 0.0;
        }
        double x = 1.0 + 0.2 * directedVelocity * directedVelocity / soundSquared;
        double x2 = x * x;
        double x3 = x2 * x;
        return pressure() * (Math.sqrt(x3 * x3 * x) - 1.0);
    }

    double moles() {
        return moles;
    }

    double volume() {
        return volume;
    }

    double velocity() {
        double mass = mass();
        return mass > 0.0 ? momentum / mass : 0.0;
    }

    Mix mix() {
        return new Mix(fuelFraction, inertFraction, oxygenFraction);
    }

    static double kCarb(double flowRateScfm) {
        return flowConstant(flowRateScfm * (0.002641 * 453.59237 / 60.0),
                ATMOSPHERE, 1.5 * 3386.3886666666713, ROOM_TEMPERATURE);
    }

    static double k28InH2O(double flowRateScfm) {
        return flowConstant(flowRateScfm * (0.002641 * 453.59237 / 60.0),
                ATMOSPHERE, 28.0 * 3386.3886666666713 * 0.0734824, ROOM_TEMPERATURE);
    }

    private static double flowConstant(double targetFlow, double pressure,
                                       double pressureDrop, double temperature) {
        double terminal = pressure - pressureDrop;
        double ratio = terminal / pressure;
        double flow;
        if (ratio <= CHOKED_FLOW_LIMIT) {
            flow = CHOKED_FLOW_RATE;
        } else {
            flow = (2.0 * HEAT_CAPACITY_RATIO) / (HEAT_CAPACITY_RATIO - 1.0);
            flow *= 1.0 - Math.pow(ratio, (HEAT_CAPACITY_RATIO - 1.0) / HEAT_CAPACITY_RATIO);
            flow = Math.sqrt(flow) * Math.pow(ratio, 1.0 / HEAT_CAPACITY_RATIO);
        }
        flow *= pressure / Math.sqrt(GAS_CONSTANT * temperature);
        return targetFlow / flow;
    }

    private static double flowRate(double flowConstant, double upstreamPressure,
                                   double downstreamPressure, double upstreamTemperature,
                                   double heatCapacityRatio) {
        upstreamTemperature = Math.max(1.0, upstreamTemperature);
        double ratio = Dsp.clamp(downstreamPressure / upstreamPressure, 0.0, 1.0);
        double flow;
        if (ratio <= CHOKED_FLOW_LIMIT) {
            flow = CHOKED_FLOW_RATE / Math.sqrt(GAS_CONSTANT * upstreamTemperature);
        } else {
            double s = Math.pow(ratio, 1.0 / heatCapacityRatio);
            flow = (2.0 * heatCapacityRatio) / (heatCapacityRatio - 1.0);
            flow *= s * (s - ratio);
            flow = Math.sqrt(Math.max(flow, 0.0) / (GAS_CONSTANT * upstreamTemperature));
        }
        return flow * upstreamPressure * flowConstant;
    }

    private double thermalEnergyPerMole() {
        return moles > 0.0 ? thermalEnergy / moles : 0.0;
    }

    private void gainMoles(double count, double energyPerMole, Mix mix) {
        double next = moles + count;
        if (next <= 0.0) {
            return;
        }
        fuelFraction = (fuelFraction * moles + count * mix.fuel) / next;
        inertFraction = (inertFraction * moles + count * mix.inert) / next;
        oxygenFraction = (oxygenFraction * moles + count * mix.oxygen) / next;
        thermalEnergy += count * energyPerMole;
        moles = next;
    }

    private void loseMoles(double count, double energyPerMole) {
        thermalEnergy -= count * energyPerMole;
        moles = Math.max(0.0, moles - count);
    }

    private double mass() {
        return AIR_MOLAR_MASS * moles;
    }

    private double approximateDensity() {
        return volume > 0.0 ? mass() / volume : 0.0;
    }

    private double speedOfSound() {
        double density = approximateDensity();
        return density > 0.0 ? Math.sqrt(Math.max(0.0, pressure() * HEAT_CAPACITY_RATIO / density)) : 0.0;
    }

    private double bulkKineticEnergy() {
        double mass = mass();
        return mass > 0.0 ? 0.5 * momentum * momentum / mass : 0.0;
    }

    private void conserveBulkEnergy(double oldMomentum) {
        double mass = mass();
        if (mass <= 0.0) {
            return;
        }
        double before = 0.5 * oldMomentum * oldMomentum / mass;
        double after = bulkKineticEnergy();
        thermalEnergy -= after - before;
    }

    private void sanitize() {
        if (!Double.isFinite(thermalEnergy) || thermalEnergy < 1.0E-12) {
            thermalEnergy = 1.0E-12;
        }
        if (!Double.isFinite(moles) || moles < 1.0E-14) {
            moles = 1.0E-14;
        }
        if (!Double.isFinite(momentum)) {
            momentum = 0.0;
        }
    }
}
