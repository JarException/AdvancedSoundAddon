package de.jarexception.advancedsoundaddon.sound;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

/** Manual wheel-slip preview renderer; not included in the production mod JAR. */
public final class TireSquealPreview {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    private TireSquealPreview() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length < 1 || arguments.length > 2) {
            throw new IllegalArgumentException("Expected output WAV and optional tyre preset");
        }
        String preset = arguments.length == 2 ? arguments[1] : "PERFORMANCE_TIRE";
        long origin = 1_000_000_000L;
        TireSquealVoice voice = new TireSquealVoice(TireSquealProfile.forPreset(preset),
                SAMPLE_RATE, telemetry(50, 0, 0.15F, false, origin));
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        int chunks = (int) Math.ceil(8.0 * SAMPLE_RATE / CHUNK);
        for (int chunk = 0; chunk < chunks; chunk++) {
            double seconds = chunk * CHUNK / (double) SAMPLE_RATE;
            float speed = 50;
            float slip = 0.04F;
            float throttle = 0.15F;
            boolean braking = false;
            if (seconds >= 0.75 && seconds < 1.75) {
                slip = 0.78F;
            } else if (seconds >= 2.0 && seconds < 3.0) {
                slip = 0.99F;
            } else if (seconds >= 3.5 && seconds < 4.5) {
                speed = 0.8F;
                slip = 0.99F;
                throttle = 1.0F;
            } else if (seconds >= 5.0 && seconds < 6.5) {
                speed = (float) (72.0 - (seconds - 5.0) * 34.0);
                slip = 0.97F;
                throttle = 0.0F;
                braking = true;
            }
            long timestamp = origin + Math.round(seconds * 1_000_000_000.0);
            pcm.write(voice.render(telemetry(speed, slip, throttle, braking, timestamp), CHUNK));
        }

        byte[] samples = pcm.toByteArray();
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(samples),
                format, samples.length / format.getFrameSize())) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, new File(arguments[0]));
        }
    }

    private static EngineTelemetry telemetry(float speed, float slip, float throttle,
                                             boolean braking, long timestamp) {
        return new EngineTelemetry(2_800, 7_000, throttle, 0.5F, speed, 3,
                true, false, braking, slip, false, timestamp);
    }
}
