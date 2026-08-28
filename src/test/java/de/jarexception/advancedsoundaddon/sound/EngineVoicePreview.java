package de.jarexception.advancedsoundaddon.sound;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

/** Manual preview renderer; not included in the production mod JAR. */
public final class EngineVoicePreview {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    private EngineVoicePreview() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length < 1 || arguments.length > 3) {
            throw new IllegalArgumentException(
                    "Expected output WAV path, optional engine preset and optional afterfire preset");
        }
        EngineProfile profile = arguments.length >= 2
                ? EngineProfile.forPreset(arguments[1]) : EngineProfile.forLayout(EngineLayout.I6);
        AfterfireProfile afterfire = arguments.length == 3
                ? AfterfireProfile.forPreset(arguments[2]) : null;
        float maximumRpm = profile.resolveAcousticMaxRpm(5_500);
        EngineTelemetry off = telemetry(0, maximumRpm, false, 0, 0, false);
        EngineVoice voice = new EngineVoice(profile, null, null, null, afterfire,
                SAMPLE_RATE, off);
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        int chunks = (int) Math.ceil(9.0 * SAMPLE_RATE / CHUNK);
        float idleRpm = Math.min(profile.getIdleRpm(), maximumRpm);
        for (int chunk = 0; chunk < chunks; chunk++) {
            double seconds = chunk * CHUNK / (double) SAMPLE_RATE;
            EngineTelemetry telemetry;
            if (seconds < 0.5) {
                telemetry = off;
            } else if (seconds < 2.1) {
                telemetry = telemetry(idleRpm, maximumRpm,
                        true, 0.12F, 0.30F, false);
            } else if (seconds < 3.2) {
                telemetry = telemetry(idleRpm, maximumRpm,
                        true, 0.0F, 0.10F, false);
            } else if (seconds < 6.4) {
                float progress = (float) ((seconds - 3.2) / 3.2);
                telemetry = telemetry(900 + progress * (maximumRpm - 900), maximumRpm,
                        true, 1.0F, 0.92F, progress > 0.97F);
            } else if (seconds < 8.0) {
                float progress = (float) ((seconds - 6.4) / 1.6);
                telemetry = telemetry(maximumRpm - progress * (maximumRpm - 1_000), maximumRpm,
                        true, 0.0F, 0.12F, false);
            } else {
                telemetry = off;
            }
            pcm.write(voice.render(telemetry, CHUNK));
        }

        byte[] samples = pcm.toByteArray();
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(samples), format,
                samples.length / format.getFrameSize())) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, new File(arguments[0]));
        }
    }

    private static EngineTelemetry telemetry(float rpm, float maximumRpm,
                                             boolean engineOn, float throttle,
                                             float load, boolean limiter) {
        return new EngineTelemetry(rpm, maximumRpm, throttle, load, 40, 3,
                engineOn, limiter, false, System.nanoTime());
    }
}
