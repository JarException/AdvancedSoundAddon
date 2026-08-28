package de.jarexception.advancedsoundaddon.sound;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

/** Manual procedural-siren preview renderer; not included in the production mod JAR. */
public final class SirenPreview {
    private static final int SAMPLE_RATE = 48_000;
    private static final int CHUNK = 1_024;

    private SirenPreview() {
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException("Expected output WAV and siren preset");
        }
        long origin = 1_000_000_000L;
        SirenVoice voice = new SirenVoice(SirenProfile.forPreset(arguments[1]),
                1.0F, SAMPLE_RATE, telemetry(false, origin));
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        int chunks = (int) Math.ceil(8.0 * SAMPLE_RATE / CHUNK);
        for (int chunk = 0; chunk < chunks; chunk++) {
            double seconds = chunk * CHUNK / (double) SAMPLE_RATE;
            pcm.write(voice.render(telemetry(true,
                    origin + Math.round(seconds * 1_000_000_000.0)), CHUNK));
        }

        byte[] samples = pcm.toByteArray();
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(samples),
                format, samples.length / format.getFrameSize())) {
            AudioSystem.write(audio, AudioFileFormat.Type.WAVE, new File(arguments[0]));
        }
    }

    private static EngineTelemetry telemetry(boolean siren, long timestamp) {
        return new EngineTelemetry(0, 7_000, 0, 0, 0, 0,
                false, false, false, 0, false, siren, false, timestamp);
    }
}
