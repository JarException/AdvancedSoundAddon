package de.jarexception.advancedsoundaddon.client;

final class AudioBufferPolicy {
    private AudioBufferPolicy() {
    }

    static int queuedBytes(int bufferBytes, int availableBytes) {
        return Math.max(0, bufferBytes - Math.max(0, Math.min(bufferBytes, availableBytes)));
    }

    static int targetQueuedBytes(int bufferBytes, int chunkBytes, int targetChunks) {
        if (bufferBytes <= 0 || chunkBytes <= 0 || targetChunks <= 0) {
            return 0;
        }
        long requested = (long) chunkBytes * targetChunks;
        int maximumWithoutBlocking = bufferBytes > chunkBytes
                ? bufferBytes - chunkBytes : bufferBytes;
        return (int) Math.min(requested, maximumWithoutBlocking);
    }

    static boolean needsRefill(int bufferBytes, int availableBytes,
                               int chunkBytes, int targetChunks) {
        return queuedBytes(bufferBytes, availableBytes)
                < targetQueuedBytes(bufferBytes, chunkBytes, targetChunks);
    }

    static int chunksToRefill(int bufferBytes, int availableBytes,
                              int chunkBytes, int targetChunks) {
        if (chunkBytes <= 0) {
            return 0;
        }
        int missingBytes = targetQueuedBytes(bufferBytes, chunkBytes, targetChunks)
                - queuedBytes(bufferBytes, availableBytes);
        return Math.max(0, (missingBytes + chunkBytes - 1) / chunkBytes);
    }
}
