package de.jarexception.advancedsoundaddon.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.InflaterInputStream;

/** Provides Engine Sim's MIT-licensed mild-exhaust transfer kernel. */
final class EngineSimImpulseResponse {
    private static final int SOURCE_SAMPLE_RATE = 44_100;
    private static final double ENGINE_SIM_VOLUME = 0.01;
    private static final String MILD_EXHAUST_PCM_ZLIB_BASE64 =
            "eNp1VAtcTVkX3+fce889963SrfRA9CTyTilSDSZv45nHlNeQEKl7Q9xbKWHMmPlIDE1R8iiPTNG4CRkzPq+Ud8IoKpW6j+4959yz" +
            "vx1mxMy3/79zzt5rr/Vfa/33/p2KC5Qq7gNiP3zVqltpzEzbhJSErQipCO/f72d/IRXtqxPiE6IRlAmbExITtiB0WFPe7f7ll9Yp" +
            "pvPqU7bO+/9m78yX+i/Wv2LT/iX2o1/qZxV8zpL6GeenPXfmT/2MPfX/zrckJCVsSohJ+DphaMLLjdM2Rm0QrQ9STotzjiGjWhcM" +
            "HT/HqxpeutO38FzOy8zIDPr7yG3jU64npSSa1MfUS9TualxdpypXZat2oHNJUG1Qrf+AeJUSPYrP0HF+ir9P9P3qvV3x9+qjhwJx" +
            "KP9+x7/jfL96j/h3WK/aiDJvViWqUlXfqnap0lU/qTJVP6Oa8lXXVXqVkzpAPVetVKepd6nz1NXqPomKRE0iSPJJikmqSBqbfDbZ" +
            "ckv0lsYt6hSH1DupIM1hu+fOol3JuwdmdP/p2sEfs+V5g/NbTjKnR/5ypiT0yrc3jz5wflH9emazd/tS3EmSaNPS062veEiQ356A" +
            "7iNJ//O+Mn9OiOVE01eBs8vDVsy/H3500bAl3kvtvmn8JmT51BUBK39cdXuVZmX3lYdX/RkdsXZUzLmYHus8Yi/FeSit4hfFc+N9" +
            "lLcUVsoU5TXlBeV+ZYZyndJbOUNJxJ+Pl66vj18QX6gMUY5WLlSGKv9QVMbVxu6MbYstjZusWKUIU/grZigyFbsVIxR2ClKRGncl" +
            "tlusw7rgtY2rX60siwqLgismrVgUmbp8+3J15Ncr9q4YvCIwUrPs0tL0xd0XUguK5orDCsPmz3025+LM0K8sp0RNdB0/ZNyAkMjA" +
            "7QFz/RTDugwS9Wt1/7WXjdNquzCbffIdXeO6KEQexGMQxO4CDrxJZAT5FB/VrmhMrR3wMvllee2CWrcX2TV7anJrCquTHnrd7XP9" +
            "/mWDhl+yqCjz7JNC/enTJ28VuBZMOTHv+LT8Aac3n91ztvnkrKODckIOw8PzctV5vY5bnog+Nifvau65w9bZ5kxNpnv2bzkJR2pz" +
            "yOzUgxMOXDwYl903d9OR3kfm5RbnZOUU5+zKyc8Jz608cuZYYEHUSc/8lmOXjk8vkJ2ccqLfkYYs68yxmWt/9s6qy3LP1mb9nt16" +
            "eMiRdUcnn5h58sCZ6KKyYvJs+MlTBezpIcUzNIWXOeX1ZdM1/S84akJKnTUbz7cXVRQ/LzmniSmt1JwrLbt8/I/lFQX3oqtS7oTd" +
            "bLjhentDxYK7z+/mV7ZX9X/APLKuca811d9onNEMdC50KFbFPce7wr2It2LlnNG8YE4QGI0dI06K+3Z5IgsXb+Kf55RxfuALJKGW" +
            "F62fyvfLL8oldk4Odx2K7O26rbcZK0+y+6l7mMs9966eBzwn9vXw3jG4YeixoQ8G/zbIeqiPX8PILaOjgiYHDQiKDhoR9DjwwMiA" +
            "AHyUU8jSL2Xjc8auGJ3iP9RvjW+0n3VA3ciigN/9zvte8p8RZBxzeNzysVUh/sFWwauDA0JehySG2AQXjJ46+kTg3YAzvmnD6obu" +
            "94n1tfAz+Mr9okekB4waafRP9L81cnJQUUhRCB7Ud+S0Edt9n/msGhY/7Pvh5X5P/Zb5yoYH+jQMG+hj8tnm893gOq+BHpc8Xnkl" +
            "Dug9cG1/+z7jPNg+xf3PeR/oN7xPvfsT9zseru5C54P2t+yiHAb27OZyyuVlr0HOm529XBw9enpN6X/Oa6b79z2u2M2Rx1iZJcH8" +
            "tfgh7A7nCq+ZBwgvMlK0WlIr2SKeKZhORPEc+UlCiWSvKIpowvKxKJ6N8L5oicCJtxgbzsYwU+ExIlRyVJYsGUHGc2N4S/nPeA3Y" +
            "JlgIxnMqOQQWTG/W+2sHaPNbbzUNr59S5/FK+ObbVgvt7tbNbSXtGbCe/0DsITaQnkQk5ytMCxpAISTZqeaHzGtqq9HTuM+YY5it" +
            "vdCWrb9qKmHO09f0Kc2TG07Vj2pYU2/xqvZl9MvUupYGTUtvbb3urH6qwbZ9ZPsQw9e6263cluImXgvb2qDD2++2922vNoSYBKwd" +
            "Pg9fzWYZ12g1b9+0ZuuzjN+aoDHDWElVQAV3DOHN0+Eh+G+ck7x4oopbCNYy3tQmKpHxNPtSFfpgbR/dIyMJdbg3ZzC2H96HXlgO" +
            "dgO+YMz0bHYHvpDQkE8FsYIQohhk0q6mK8bhpnSTyLTBON+UQSVT20yFRtJ4xfBI96PWqK0ylDJdOHqkeg3vHCEir5KL+Y7cZdht" +
            "EIFdwUzYJbyGE8Vdz1VxPXk3iCT+c14MFyPmCPSiZ2JMHCE4zxPhKdAaNkB7cJtNNpMwm1NK7hOXSN9K1VKt1Fp2SuxKpnNyQTz4" +
            "D2cSeUiEiV2FVfznxCzyO1GtVGDhb9Em85NVy+JkJ8RuAoxQc0ScNO5eXhfeVU4e7ob74CymBJfZY3AE10MUbVkuD5Uv7EpbJVm9" +
            "tNDI0qXhsnmyPtJWmcaqxvqqnJSXWRSKivlPiBYiligjCgRbpdEWpMXDLm8sBBYvJDdFjuJy8WtxmfSgVaVtg91xebEVblVpudJS" +
            "ZKGTMMJc4UpJk8V6a6O1nZVU+lwwTzBFNF0SJZtqWS8Pd2xxPu6idvVykfYstU+UX7SokYVamewqndqdSu11tq9sr9kdtp1pvc3i" +
            "rjRM1q3rVjuh/Ze2AuvbXfPkjE2UXZi9xCHY8eeeO1zvuHu43e5hYRtm8b00SjpEcouM4X1B3pM9ky/vtrTbNpsIq8EWcVZ7bTS2" +
            "UTaDbPPsE53SnCrsz9iEdx1j9Z1lqSxddJ5MJh8J7gtPiBwko6UK6WTpPclT8STxVfFNiUb6ROokHSWpEHkIVhFePAGvhhvO20aW" +
            "Sh9ZS+xdHfc4BHcDNk2WX0g1wseCZqGf2FesEXzBs8c2s+7mUeYbrDP2JXcNuUsslR2Slop4gnUkJkwXl0s2iObz53IJjj/eHXOF" +
            "BAOMOu0fb/e13Go1GbyZUnYie5hZT8kMd1r2NK5p3NVcrJWaMukjpnv62YYWZiBnGqcb69Lep41oTWgL0wn19vpyw1BTgEltKGo7" +
            "3fKiOa0l4u24VllbrLZKb9+u15e0PWme0OTUwmu7qfXTbdCl6VcZmvRzdGmtPzTZN+Y1Fjc5vg1p89NWtDm2jWtbonXTb29PMRFU" +
            "smmRcZbBTvelNl2300AZrukv6MboqrUOurUo4ytDm36cvlp/x5DR3mRcTVnQgXQBfZk+Qo01+hp47S50GcwG4eYd9O9sFl6H5Zvl" +
            "VJhpAH3XHA4rWEuz3MwD0fhe4MPMow4yg4EOK8DOY1pcwq3h9iDW8D3JXmQGfzuRw32IP8WXcHfzMnnt3G+4w7lajhV3Bi+SuMmL" +
            "52Zy6wiVIEeYJ5wlfCVoFeQLdwnjyYlEHuEoyBRGCJ4TG3gzefN4Cp4NkUv0JiD3HpdDjOc/408jtWQDGUxKSQkZwu9ClPHc+PMF" +
            "vwjvI4YS4U2Rq2SoZJJIQe4g3IhSnoArx49ix/CJnO74bdjA7gBVeC7HkjMHV+HVnP5EAdkm4AgyiJXczZwEzm8cT24sdxXXzJnE" +
            "uYJH4AwWjo0Bb9kJ7CE2BKbBCDgGFsMokAk4gIChrJwdw2azOFyI/gBPQX+8HyeCI+SswiuwJMwZ6w66wiw2mY1jd7M32RLWjU00" +
            "J5kHsq9ZD8iBD9gHbC2rY9+w6WwPttZsNI9gY1hr9ikTSptMvUyNxhkmKXWN2k5PYJ4xY8xbzPvNJ8wJ5jxGTZdQauo76jR1lSqk" +
            "NlIzqMXUNiod2RZR8dR1iqUuUQ7UStNAk9LUZvJH80umK6YwahD9nHZi/qQV9Dg6nXZlUph1DINWgfQq+gJ9k95JD6e59CsqnxpG" +
            "pZhqjWeNDqZ7pi2UgeLSD6gLVAUlpCfQ4XRf+gXK8JByoKfRnnQBNZ2KoiiqnFYyj5hW5hoTyYiYP+jjdA59hq6hhzIHGQPjbg4y" +
            "rzNXmseyJ1DXdejtxxaYe5uzmb7MDTqTLqJFjIKpZSaYz5llrDOrNf9sjjDHmevMCqTvcpgNf4d8sAhcBI3gKlgGLEE90pyBdiAE" +
            "rAP7QT5IBzMAhL/CDJgKV6NzsoT17FNWy7rCZfAIfADboRA4g34o8jrcCEdDdzgE7RRBCzALKEEciABjgBsQAxzIgAcIQLwjwQDQ" +
            "DbDwT/gYNkESyIENIMEbWAUr4WvENgBMAQvBahS9AawEE0BvdCuaYTW8C6/BU3AbqvorOB7OhNHwB3gW3oI1sBGaoRVwAX2BPdCi" +
            "WpPgVOiG7kIdex+h4zbQLIBdUG2B6K7tRD2z0An0AtaAgY8QRzpMhuvhOsS4An6DEAVjkWU3Uuc4/AWWwTuo0lr4FnJRpT0R5EAA" +
            "2uFz+F/UZybcCmPgYhgGZ8HpKO9UOA3OgPMQ02a4C2ahin9F+R7DVihG/Y8CE5Ges0EYmAOmg3FgOOpOBoxIiwp4CRbCXHgA7oP7" +
            "4UF4CObBkyh7Ccp/Fd5A6jyBL5A+jbD5HVoQWmEb1EEDNEIKKQCQTnxUmRCpLUWQIUjQXISsBNKfRV7tUA+1KKoFKd8A6xFfPew4" +
            "8Ua07uDTIy4GQoijCAGK7GDp8o5JipiEiImHsmAAIDYzAgXpD2DeocPaMZAD8uoA/gHY33g/Ps4+DqzTFwMfOT5Gfvr9fAVA57z/" +
            "9Pw0Bu/kCf7FG++Ur3Nm8EmezrHgs2r+WSXo1Bn4jA980vX/j8b+odCnVXWMjhP4J/NHjf46oY4Hwv8BvE3QeA==";

    private EngineSimImpulseResponse() {
    }

    static double[] mildExhaust(int targetSampleRate) {
        byte[] sourcePcm = inflate(Base64.getDecoder().decode(MILD_EXHAUST_PCM_ZLIB_BASE64));
        int sourceFrames = clippedLength(sourcePcm);
        double[] source = new double[sourceFrames];
        for (int i = 0; i < sourceFrames; i++) {
            int offset = i * 2;
            short value = (short) ((sourcePcm[offset] & 0xFF) | (sourcePcm[offset + 1] << 8));
            source[i] = value / 32768.0 * ENGINE_SIM_VOLUME;
        }

        int targetFrames = Math.max(1, (int) Math.round(sourceFrames
                * targetSampleRate / (double) SOURCE_SAMPLE_RATE));
        double[] result = new double[targetFrames];
        for (int i = 0; i < targetFrames; i++) {
            double sourcePosition = i * SOURCE_SAMPLE_RATE / (double) targetSampleRate;
            int lower = Math.min(sourceFrames - 1, (int) sourcePosition);
            int upper = Math.min(sourceFrames - 1, lower + 1);
            double fraction = sourcePosition - lower;
            result[i] = source[lower] * (1.0 - fraction) + source[upper] * fraction;
        }

        return result;
    }

    private static int clippedLength(byte[] pcm) {
        int frames = Math.min(10_000, pcm.length / 2);
        int clipped = 1;
        for (int i = 0; i < frames; i++) {
            int offset = i * 2;
            short value = (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
            if (Math.abs((int) value) > 100) clipped = i + 1;
        }
        return clipped;
    }

    private static byte[] inflate(byte[] compressed) {
        try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1_024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot decode embedded Engine Sim exhaust response", exception);
        }
    }
}
