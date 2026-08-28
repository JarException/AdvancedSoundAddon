package de.jarexception.advancedsoundaddon.sound;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Manual realtime-budget probe; not included in the production mod JAR. */
public final class MultiVoicePerformanceProbe {
    private MultiVoicePerformanceProbe() {
    }

    public static void main(String[] arguments) throws Exception {
        int workerCount = arguments.length == 0 ? 8 : Integer.parseInt(arguments[0]);
        int sampleRate = 48_000;
        int chunk = 1_024;
        int chunks = 120;
        EngineProfile[] profiles = {
                EngineProfile.forPreset("I4"), EngineProfile.forPreset("I6_DIESEL"),
                EngineProfile.forPreset("V8_CROSSPLANE"), EngineProfile.forPreset("V8_FLATPLANE"),
                EngineProfile.forPreset("V12"), EngineProfile.forPreset("W16"),
                EngineProfile.forPreset("ELECTRIC"), EngineProfile.forPreset("TURBOSHAFT")
        };
        List<EngineVoice> voices = new ArrayList<>();
        for (int profileIndex = 0; profileIndex < profiles.length; profileIndex++) {
            EngineProfile profile = profiles[profileIndex];
            EngineTelemetry telemetry = telemetry(3_600);
            RotorProfile rotor = profileIndex == profiles.length - 1
                    ? RotorProfile.forPreset("HELICOPTER_4_BLADE") : null;
            EngineVoice voice = new EngineVoice(profile, rotor, sampleRate, telemetry);
            int index = voices.size();
            voice.setFluidSubsteps(index < 2 ? 8 : (index < 5 ? 6 : 4));
            voices.add(voice);
        }
        ExecutorService workers = Executors.newFixedThreadPool(workerCount);
        long started = System.nanoTime();
        try {
            for (int index = 0; index < chunks; index++) {
                List<Future<byte[]>> futures = new ArrayList<>();
                for (int voiceIndex = 0; voiceIndex < voices.size(); voiceIndex++) {
                    final int captured = voiceIndex;
                    futures.add(workers.submit(() -> voices.get(captured).render(
                            telemetry(3_600 + captured * 90), chunk)));
                }
                for (Future<byte[]> future : futures) future.get();
            }
        } finally {
            workers.shutdownNow();
        }
        double wallSeconds = (System.nanoTime() - started) / 1.0E9;
        double audioSeconds = chunks * chunk / (double) sampleRate;
        System.out.printf(java.util.Locale.ROOT,
                "voices=%d workers=%d wall=%.3fs audio=%.3fs realtimeHeadroom=%.2fx%n",
                voices.size(), workerCount, wallSeconds, audioSeconds, audioSeconds / wallSeconds);
    }

    private static EngineTelemetry telemetry(float rpm) {
        return new EngineTelemetry(rpm, 7_000, 0.72F, 0.82F,
                70, 4, true, false, false, System.nanoTime());
    }
}
