package de.jarexception.advancedsoundaddon.sound;

import java.util.Arrays;

final class Dsp {
    private Dsp() {
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static double smoothStep(double value) {
        double x = clamp(value, 0.0, 1.0);
        return x * x * (3.0 - 2.0 * x);
    }

    static final class Resonator {
        private final double coefficient;
        private final double radiusSquared;
        private final double outputScale;
        private double y1;
        private double y2;

        Resonator(double sampleRate, double frequency, double bandwidth) {
            double clampedFrequency = clamp(frequency, 20.0, sampleRate * 0.44);
            double radius = Math.exp(-Math.PI * Math.max(8.0, bandwidth) / sampleRate);
            coefficient = 2.0 * radius * Math.cos(2.0 * Math.PI * clampedFrequency / sampleRate);
            radiusSquared = radius * radius;
            outputScale = 1.0 - radius;
        }

        double process(double input) {
            double output = input + coefficient * y1 - radiusSquared * y2;
            y2 = y1;
            y1 = output;
            return output * outputScale;
        }

        void reset() {
            y1 = 0;
            y2 = 0;
        }
    }

    static final class OnePoleLowPass {
        private final double sampleRate;
        private double alpha;
        private double state;

        OnePoleLowPass(double sampleRate, double cutoff) {
            this.sampleRate = sampleRate;
            setCutoff(cutoff);
        }

        void setCutoff(double cutoff) {
            double clamped = clamp(cutoff, 20.0, sampleRate * 0.45);
            alpha = 1.0 - Math.exp(-2.0 * Math.PI * clamped / sampleRate);
        }

        double process(double input) {
            state += alpha * (input - state);
            return state;
        }

        void reset() {
            state = 0;
        }
    }

    /** One-pole high-pass implemented with a complementary low-pass. */
    static final class OnePoleHighPass {
        private final OnePoleLowPass lowPass;

        OnePoleHighPass(double sampleRate, double cutoff) {
            lowPass = new OnePoleLowPass(sampleRate, cutoff);
        }

        void setCutoff(double cutoff) {
            lowPass.setCutoff(cutoff);
        }

        double process(double input) {
            return input - lowPass.process(input);
        }

        void reset() {
            lowPass.reset();
        }
    }

    static final class DelayLine {
        private final double[] samples;
        private int index;

        DelayLine(int delaySamples) {
            samples = new double[Math.max(1, delaySamples)];
        }

        double process(double input) {
            double output = samples[index];
            samples[index] = input;
            index++;
            if (index == samples.length) {
                index = 0;
            }
            return output;
        }

        void reset() {
            Arrays.fill(samples, 0.0);
            index = 0;
        }
    }

    /** Fourth-order Butterworth low-pass, ported from Engine Sim's MIT filter. */
    static final class ButterworthLowPass {
        private final double[] previousInput = new double[4];
        private final double[] previousOutput = new double[4];
        private final double sampleRate;
        private final double[] a = new double[5];
        private double f4;

        ButterworthLowPass(double sampleRate, double cutoff) {
            this.sampleRate = sampleRate;
            setCutoff(cutoff);
        }

        void setCutoff(double cutoff) {
            double clamped = clamp(cutoff, 20.0, sampleRate * 0.45);
            double f = Math.tan(Math.PI * clamped / sampleRate);
            double f2 = f * f;
            double f3 = f2 * f;
            f4 = f2 * f2;
            double m = -2.0 * Math.cos(5.0 * Math.PI / 8.0);
            double n = -2.0 * Math.cos(7.0 * Math.PI / 8.0);

            a[0] = 1.0 + (m + n) * f + (2.0 + n * m) * f2 + (m + n) * f3 + f4;
            a[1] = (-4.0 - 2.0 * (n + m) * f + 2.0 * (m + n) * f3 + 4.0 * f4) / a[0];
            a[2] = (6.0 - 2.0 * (2.0 + m * n) * f2 + 6.0 * f4) / a[0];
            a[3] = (-4.0 + 2.0 * (m + n) * f - 2.0 * (m + n) * f3 + 4.0 * f4) / a[0];
            a[4] = (1.0 - (n + m) * f + (2.0 + m * n) * f2 - (m + n) * f3 + f4) / a[0];
        }

        double process(double input) {
            double numerator = f4 / a[0] * (input + 4.0 * previousInput[0]
                    + 6.0 * previousInput[1] + 4.0 * previousInput[2] + previousInput[3]);
            double denominator = -a[1] * previousOutput[0] - a[2] * previousOutput[1]
                    - a[3] * previousOutput[2] - a[4] * previousOutput[3];
            double output = numerator + denominator;

            previousInput[3] = previousInput[2];
            previousInput[2] = previousInput[1];
            previousInput[1] = previousInput[0];
            previousInput[0] = input;
            previousOutput[3] = previousOutput[2];
            previousOutput[2] = previousOutput[1];
            previousOutput[1] = previousOutput[0];
            previousOutput[0] = output;
            return output;
        }

        void reset() {
            Arrays.fill(previousInput, 0.0);
            Arrays.fill(previousOutput, 0.0);
        }
    }

    /** Engine Sim's short, noise-controlled fractional delay jitter stage. */
    static final class JitterFilter {
        private final double[] history;
        private final OnePoleLowPass offsetNoise;
        private int offset;

        JitterFilter(double sampleRate, int maximumDelay, double noiseCutoff) {
            history = new double[Math.max(2, maximumDelay)];
            offsetNoise = new OnePoleLowPass(sampleRate, noiseCutoff);
        }

        double process(double input, double randomUnit, double scale) {
            history[offset] = input;
            offset++;
            if (offset == history.length) {
                offset = 0;
            }

            double delayedSamples = clamp(offsetNoise.process(randomUnit * (history.length - 1)) * scale,
                    0.0, history.length - 1.0);
            int lowerDelay = (int) Math.floor(delayedSamples);
            int upperDelay = (int) Math.ceil(delayedSamples);
            double fraction = delayedSamples - lowerDelay;
            double lower = history[wrap(offset + lowerDelay, history.length)];
            double upper = history[wrap(offset + upperDelay, history.length)];
            return lower * (1.0 - fraction) + upper * fraction;
        }

        private static int wrap(int value, int length) {
            return value >= length ? value - length : value;
        }
    }

    /** Peak-following automatic leveler matching Engine Sim's topology. */
    static final class Leveler {
        private final double peakDecay;
        private final double target;
        private final double minimumGain;
        private final double maximumGain;
        private double peak;
        private double gain = 1.0;

        Leveler(double sampleRate, double target, double minimumGain, double maximumGain) {
            this.peakDecay = Math.pow(0.999, 44_100.0 / sampleRate);
            this.target = target;
            this.minimumGain = minimumGain;
            this.maximumGain = maximumGain;
            peak = target;
        }

        double process(double input) {
            peak *= peakDecay;
            peak = Math.max(peak, Math.abs(input));
            if (peak < 1.0E-12) {
                return 0.0;
            }
            double desired = clamp(target / peak, minimumGain, maximumGain);
            gain = gain * 0.9 + desired * 0.1;
            return input * gain;
        }
    }

    /** Stateful overlap-add FFT convolution. */
    static final class FftConvolver {
        private static final int MAXIMUM_BLOCK = 1_024;

        private final int impulseLength;
        private final int fftSize;
        private final double[] impulseReal;
        private final double[] impulseImaginary;
        private final double[] real;
        private final double[] imaginary;
        private double[] overlap;
        private double[] nextOverlap;

        FftConvolver(double[] impulseResponse) {
            if (impulseResponse.length == 0) {
                throw new IllegalArgumentException("Impulse response must not be empty");
            }
            impulseLength = impulseResponse.length;
            int required = MAXIMUM_BLOCK + impulseLength - 1;
            int size = 1;
            while (size < required) {
                size <<= 1;
            }
            fftSize = size;
            impulseReal = new double[fftSize];
            impulseImaginary = new double[fftSize];
            System.arraycopy(impulseResponse, 0, impulseReal, 0, impulseLength);
            fft(impulseReal, impulseImaginary, false);

            real = new double[fftSize];
            imaginary = new double[fftSize];
            overlap = new double[Math.max(0, impulseLength - 1)];
            nextOverlap = new double[overlap.length];
        }

        double[] process(double[] input) {
            double[] output = new double[input.length];
            int offset = 0;
            while (offset < input.length) {
                int blockLength = Math.min(MAXIMUM_BLOCK, input.length - offset);
                processBlock(input, offset, output, offset, blockLength);
                offset += blockLength;
            }
            return output;
        }

        private void processBlock(double[] input, int inputOffset, double[] output,
                                  int outputOffset, int blockLength) {
            Arrays.fill(real, 0.0);
            Arrays.fill(imaginary, 0.0);
            System.arraycopy(input, inputOffset, real, 0, blockLength);
            fft(real, imaginary, false);
            for (int i = 0; i < fftSize; i++) {
                double productReal = real[i] * impulseReal[i] - imaginary[i] * impulseImaginary[i];
                double productImaginary = real[i] * impulseImaginary[i] + imaginary[i] * impulseReal[i];
                real[i] = productReal;
                imaginary[i] = productImaginary;
            }
            fft(real, imaginary, true);

            for (int i = 0; i < blockLength; i++) {
                output[outputOffset + i] = real[i] + (i < overlap.length ? overlap[i] : 0.0);
            }
            for (int i = 0; i < nextOverlap.length; i++) {
                int convolutionIndex = blockLength + i;
                double current = convolutionIndex < blockLength + impulseLength - 1
                        ? real[convolutionIndex] : 0.0;
                double previous = convolutionIndex < overlap.length ? overlap[convolutionIndex] : 0.0;
                nextOverlap[i] = current + previous;
            }
            double[] swap = overlap;
            overlap = nextOverlap;
            nextOverlap = swap;
        }

        private static void fft(double[] real, double[] imaginary, boolean inverse) {
            int length = real.length;
            for (int i = 1, j = 0; i < length; i++) {
                int bit = length >>> 1;
                while ((j & bit) != 0) {
                    j ^= bit;
                    bit >>>= 1;
                }
                j ^= bit;
                if (i < j) {
                    double realSwap = real[i];
                    real[i] = real[j];
                    real[j] = realSwap;
                    double imaginarySwap = imaginary[i];
                    imaginary[i] = imaginary[j];
                    imaginary[j] = imaginarySwap;
                }
            }

            for (int size = 2; size <= length; size <<= 1) {
                double angle = (inverse ? 2.0 : -2.0) * Math.PI / size;
                double stepReal = Math.cos(angle);
                double stepImaginary = Math.sin(angle);
                int half = size >>> 1;
                for (int start = 0; start < length; start += size) {
                    double phaseReal = 1.0;
                    double phaseImaginary = 0.0;
                    for (int i = 0; i < half; i++) {
                        int even = start + i;
                        int odd = even + half;
                        double oddReal = real[odd] * phaseReal - imaginary[odd] * phaseImaginary;
                        double oddImaginary = real[odd] * phaseImaginary + imaginary[odd] * phaseReal;
                        real[odd] = real[even] - oddReal;
                        imaginary[odd] = imaginary[even] - oddImaginary;
                        real[even] += oddReal;
                        imaginary[even] += oddImaginary;

                        double nextReal = phaseReal * stepReal - phaseImaginary * stepImaginary;
                        phaseImaginary = phaseReal * stepImaginary + phaseImaginary * stepReal;
                        phaseReal = nextReal;
                    }
                }
            }

            if (inverse) {
                for (int i = 0; i < length; i++) {
                    real[i] /= length;
                    imaginary[i] /= length;
                }
            }
        }
    }

    /** Uniformly partitioned overlap-add convolution for long exhaust kernels. */
    static final class PartitionedConvolver {
        private static final int BLOCK_SIZE = 256;
        private static final int FFT_SIZE = BLOCK_SIZE * 2;

        private final int partitionCount;
        private final double[][] impulseReal;
        private final double[][] impulseImaginary;
        private final double[][] historyReal;
        private final double[][] historyImaginary;
        private final double[] accumulatedReal = new double[FFT_SIZE];
        private final double[] accumulatedImaginary = new double[FFT_SIZE];
        private final double[] overlap = new double[BLOCK_SIZE];
        private int historyPosition = -1;

        PartitionedConvolver(double[] impulseResponse) {
            if (impulseResponse.length == 0) {
                throw new IllegalArgumentException("Impulse response must not be empty");
            }
            partitionCount = (impulseResponse.length + BLOCK_SIZE - 1) / BLOCK_SIZE;
            impulseReal = new double[partitionCount][FFT_SIZE];
            impulseImaginary = new double[partitionCount][FFT_SIZE];
            historyReal = new double[partitionCount][FFT_SIZE];
            historyImaginary = new double[partitionCount][FFT_SIZE];
            for (int partition = 0; partition < partitionCount; partition++) {
                int sourceOffset = partition * BLOCK_SIZE;
                int length = Math.min(BLOCK_SIZE, impulseResponse.length - sourceOffset);
                System.arraycopy(impulseResponse, sourceOffset,
                        impulseReal[partition], 0, length);
                FftConvolver.fft(impulseReal[partition], impulseImaginary[partition], false);
            }
        }

        double[] process(double[] input) {
            double[] output = new double[input.length];
            process(input, output);
            return output;
        }

        void process(double[] input, double[] output) {
            if (input.length != output.length || input.length % BLOCK_SIZE != 0) {
                throw new IllegalArgumentException("Equal PCM blocks must be divisible by " + BLOCK_SIZE);
            }
            for (int offset = 0; offset < input.length; offset += BLOCK_SIZE) {
                processBlock(input, output, offset);
            }
        }

        private void processBlock(double[] input, double[] output, int offset) {
            historyPosition = (historyPosition + 1) % partitionCount;
            double[] newestReal = historyReal[historyPosition];
            double[] newestImaginary = historyImaginary[historyPosition];
            Arrays.fill(newestReal, 0.0);
            Arrays.fill(newestImaginary, 0.0);
            System.arraycopy(input, offset, newestReal, 0, BLOCK_SIZE);
            FftConvolver.fft(newestReal, newestImaginary, false);

            Arrays.fill(accumulatedReal, 0.0);
            Arrays.fill(accumulatedImaginary, 0.0);
            for (int partition = 0; partition < partitionCount; partition++) {
                int historyIndex = historyPosition - partition;
                if (historyIndex < 0) historyIndex += partitionCount;
                double[] inputReal = historyReal[historyIndex];
                double[] inputImaginary = historyImaginary[historyIndex];
                double[] filterReal = impulseReal[partition];
                double[] filterImaginary = impulseImaginary[partition];
                for (int bin = 0; bin < FFT_SIZE; bin++) {
                    accumulatedReal[bin] += inputReal[bin] * filterReal[bin]
                            - inputImaginary[bin] * filterImaginary[bin];
                    accumulatedImaginary[bin] += inputReal[bin] * filterImaginary[bin]
                            + inputImaginary[bin] * filterReal[bin];
                }
            }
            FftConvolver.fft(accumulatedReal, accumulatedImaginary, true);
            for (int sample = 0; sample < BLOCK_SIZE; sample++) {
                output[offset + sample] = accumulatedReal[sample] + overlap[sample];
                overlap[sample] = accumulatedReal[sample + BLOCK_SIZE];
            }
        }
    }
}
