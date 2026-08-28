package de.jarexception.advancedsoundaddon.client;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AudioBufferPolicyTest {
    private static final int CHUNK_BYTES = 1_024 * 4;

    @Test
    public void configuredBufferKeepsTenChunksQueued() {
        int bufferBytes = CHUNK_BYTES * AdvancedSoundSettings.OUTPUT_BUFFER_CHUNKS;
        int target = AudioBufferPolicy.targetQueuedBytes(bufferBytes, CHUNK_BYTES,
                AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS);

        assertEquals(CHUNK_BYTES * 10, target);
        assertTrue(AudioBufferPolicy.needsRefill(bufferBytes, bufferBytes, CHUNK_BYTES,
                AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS));
        assertFalse(AudioBufferPolicy.needsRefill(bufferBytes, bufferBytes - target, CHUNK_BYTES,
                AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS));
    }

    @Test
    public void configuredTargetCoversTwoHundredMilliseconds() {
        int bufferBytes = CHUNK_BYTES * AdvancedSoundSettings.OUTPUT_BUFFER_CHUNKS;
        int targetBytes = AudioBufferPolicy.targetQueuedBytes(bufferBytes, CHUNK_BYTES,
                AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS);
        double queuedMilliseconds = targetBytes / 4.0
                / AdvancedSoundSettings.SAMPLE_RATE * 1_000.0;

        assertTrue(queuedMilliseconds >= 200.0);
    }

    @Test
    public void backgroundTargetCoversFourHundredMilliseconds() {
        int bufferBytes = CHUNK_BYTES * AdvancedSoundSettings.OUTPUT_BUFFER_CHUNKS;
        int targetBytes = AudioBufferPolicy.targetQueuedBytes(bufferBytes, CHUNK_BYTES,
                AdvancedSoundSettings.BACKGROUND_OUTPUT_TARGET_CHUNKS);
        double queuedMilliseconds = targetBytes / 4.0
                / AdvancedSoundSettings.SAMPLE_RATE * 1_000.0;

        assertTrue(queuedMilliseconds >= 400.0);
        assertTrue(targetBytes <= bufferBytes - CHUNK_BYTES);
    }

    @Test
    public void focusLossAddsTenChunksToAnExistingForegroundReserve() {
        int bufferBytes = CHUNK_BYTES * AdvancedSoundSettings.OUTPUT_BUFFER_CHUNKS;
        int foregroundQueuedBytes = CHUNK_BYTES * AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS;
        int availableBytes = bufferBytes - foregroundQueuedBytes;

        assertEquals(10, AudioBufferPolicy.chunksToRefill(bufferBytes, availableBytes,
                CHUNK_BYTES, AdvancedSoundSettings.BACKGROUND_OUTPUT_TARGET_CHUNKS));
        assertFalse(AudioBufferPolicy.needsRefill(bufferBytes, availableBytes, CHUNK_BYTES,
                AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS));
        assertTrue(AudioBufferPolicy.needsRefill(bufferBytes, availableBytes, CHUNK_BYTES,
                AdvancedSoundSettings.BACKGROUND_OUTPUT_TARGET_CHUNKS));
    }

    @Test
    public void smallerDriverBufferRetainsOneChunkOfWriteHeadroom() {
        int bufferBytes = CHUNK_BYTES * 6;

        assertEquals(CHUNK_BYTES * 5,
                AudioBufferPolicy.targetQueuedBytes(bufferBytes, CHUNK_BYTES, 10));
    }

    @Test
    public void queuedByteCountClampsDriverValues() {
        assertEquals(0, AudioBufferPolicy.queuedBytes(CHUNK_BYTES, CHUNK_BYTES * 2));
        assertEquals(CHUNK_BYTES, AudioBufferPolicy.queuedBytes(CHUNK_BYTES, -1));
    }

    @Test
    public void refillCountCatchesUpToTargetInsteadOfWritingOnlyOneChunk() {
        int bufferBytes = CHUNK_BYTES * AdvancedSoundSettings.OUTPUT_BUFFER_CHUNKS;

        assertEquals(AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS,
                AudioBufferPolicy.chunksToRefill(bufferBytes, bufferBytes, CHUNK_BYTES,
                        AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS));
        assertEquals(3, AudioBufferPolicy.chunksToRefill(bufferBytes,
                bufferBytes - CHUNK_BYTES * 7, CHUNK_BYTES,
                AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS));
        assertEquals(0, AudioBufferPolicy.chunksToRefill(bufferBytes,
                bufferBytes - CHUNK_BYTES * 10, CHUNK_BYTES,
                AdvancedSoundSettings.OUTPUT_TARGET_CHUNKS));
    }
}
