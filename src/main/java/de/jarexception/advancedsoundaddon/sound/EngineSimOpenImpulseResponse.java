package de.jarexception.advancedsoundaddon.sound;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.InflaterInputStream;

/** Engine Sim's MIT-licensed minimal_muffling_03 I4/sport exhaust kernel. */
final class EngineSimOpenImpulseResponse {
    private static final int SOURCE_SAMPLE_RATE = 44_100;
    private static final String PCM_ZLIB_BASE64 =
            "eNodVwdYVMcWvssWkCYCInaxEuJTwK4oil1jVCyo0YgaC8/yJGoEG2HtEiuWYI9oErtGsYAFG6KiIIItRhQVUFjYcvfemblT3gnf/50zd2ZOm9nZ7zv/FGmK" +
            "FCNNkIbXIkrqI0VK/QH9YOwpDQQdAWNnKRx2eoLuIIVKIVIbqbXUUmomtZICpSYwBkhBUkPQTSQfyV/ylfyk+vBVR6pbq90ALpJJ0ktEuEg60IqwC1VUg64W" +
            "nwBfAJWiQrwSJeIt6GLxBsYXMD4FnQc6TzwBPBSPAQXiuXgGyBG54g7oe+Ju7dfNWrkqMsQpcVYcE+niD8DvgENiL8g+0GkgO8VusR/wm/gVvnbXrqXVjqli" +
            "i9gKWC9WixTQa4VZLBeJYilIovhJLBJxgGliCmCcGAMYKYaLwWKgiBJ9RW/RVYSL9iJUtBCtRSvRXAQJP+Ev6gl34Sr0gnNJKNzJK3kF/wzygb/hz/nfvIAX" +
            "gjzgD/ldfhvkDkg2v8Gv8izQ1/m1WrkBuAlykV8AuQy4xDP4Wf4X6Eywzahdu8ZvAbJg7yp4XYZ4WbB7hZ/np2DtX4sM2LkEcW7xe5DnLs/hj/kzqKQU8I5/" +
            "hMpkrnAH1wkomLsJN+EtGtee6Gs4WwSctL8YJcaLGWKWWCaS4I42A/aKw+K0uAa/xX3xUpQKizBKHvA62sKLiZK+lSZJc6Rl0gZpm3REOiFlSgVSqUQlN52P" +
            "roOuq26Y7gfdQt1K3X7dMd0VXZ7upc6q07sEunR0GeQS65LkssflrEuBi83FXR+ij9BP0P+sP6q/rS/VGw0hhtGGxYZdhquGD4YAY3/jIuN+4zOjhynatNWU" +
            "YzK5DnXd4vrOtbNbottNt0Z1ltZ5Xae7+2F3X491HnpPs6er10GvQd4W7x11h/s0rueo99r3pV+5vy5gYIMjgU0a5Te+0vSf5t1aktbjgte3jw+b2OV+15xu" +
            "DXukhXu2iQ0KDNa1VOq/Carffvt/AnoWRoX0o73SOweHtGjexcvqVNRrdXuF7unXtr/bgE1DzMN+6zU++H3DLh7ZzhnWCFuvsun53xwTuxffDMudd35qprXI" +
            "qFb4FXlnyd8WLt/fZo3n7gGpo1PMV8fqY0NF65mO2JIhtuEvT+38j5mah5ob/NIxnd7qv1QkWabGTCx/dNQctG6Z+c3qRLPXao9DX1aKpNWrMleIpEnOh+a4" +
            "LenmTxvsZlt7+KlqoXRrf+iVecaaCvOtNdLq51gkdYPVw0k4aYjv+xPJKSXmzDWvzCVnhj4run3uxafgE7MLlj6ZF9si9Xr+6o/mKnPpOpr9oF5kxIVe10IG" +
            "o1GZQ1Pvp33Ie+uxt3VZyN4O2c1iPiWeLNlRsavmUPjhJ5n3dd3GPE4cNfNS90MeO18+fexXXeq2sOX5Tj7hn5vZ/Pf6mDy2BPq3ym0+v+nTti2DG4c9C9da" +
            "nvQodr4oLX49sOhOcWppYun7vBP3puV0v/wu45vbfQtN8v6GDcPCIxK7T2rv1VQL2Of1FG+p+lBeZv+dR+B+FWnvO30ML3N8OvqBVtl4mueDhn2a3vR/4HnR" +
            "zUrVLx0+trXo6Wwj9jjplebRsO5jn6h6ro2XNG3m7+0+SHrIPxkj6j4JCG+wsN4aTx/TWKmp7qIwGG54ZXrNcrXpJ9I98iwaYjpl+JPoLKWlfh8XVc6xtK0+" +
            "IZuRAb3FN6QxhmTjE5OPW2tTOt1styhmPlq33TXKY7y+u9rcfr3m5Jfhn9tVeyuDtNeCuUfWz/I77r7KZT4P1NbKyY6TeLK02JTuuduHeowwMNrYlvDlneWC" +
            "Y4X2h97PfaOPUr+nb1fTce2QelpZrHyvNlIr7HE1l2U/nqMvMRaaxhpni+PIbt3wOars6OdWtjjsz3uLKqmOLoHP0U6hKDSUxZv6ek70fuNd5j5A30c7L8+1" +
            "5le3rZlkn4Aq6WTWT/sOt8HXULkzU96uttf6iKP69YbtBovhrnSX9HRutx2wVdiCbcftu5xT0HmSSuuzJ1oI7qrWwenkb3KTjCDtSCkpw3OVD3Kl7Ot0UcNw" +
            "B+0EManpzllKO6e346I11BbsiJFvEartIme0xShdke1pNfEWl6op9kXoPuuoy5d+crlMO6g77JMtQ6vHVY+3PLCdVxO0ODqEnqa/Y5PS2XHJVuwYqN5DY1Gp" +
            "ssR5x55snWedaC2zFtom2Csc3HlffY+M6LTSyZnhaONo7DjouC2XO0z2/1pDq8urFMsPVjfbVutf1krrW/tyZSVO06K12WQ40at7HAZlGI7EQ1Ag2qV2Qa/Q" +
            "VjxLjXUMskZWu9X4Wo/Zf1MK1B7ID/dHGfI4W5jtnnV6jc3yvHqLvcTxxp5v3+IolUsc12znbVZrmnW/bZBjgzNCHYy8cDS+gCVyFb1y+jjibT9bnVaL/U/5" +
            "f84FzhC5meOMfYNts/WltcIWb+/h+FVe4kxQZiEXfBx1V9fJux2BcopM5OvyacdF+2K7w/5f2UsJUSqdPzsHyCXyPmWvukRdqIx3LpYj5DZynr2dbaltp32N" +
            "4468VU6WY50Bzji5yPHKbnIkO7PQYfKeLCPxeA3aq9qdb+WHssWZq/rjEHwZVakfFOEMdh6Q68jrHX3lTcotVISP41jUQp2mxDuZHOyco2xDG0kovUyLNYbt" +
            "aqjqDV7Barw6XZXVGvQU3sRPRENpapayV/modFYz1BL1meqJ1qAGOBcHkRX4FBqMKtRolSjJ6kNUiIvxIpyHhqFj6hQ1Qj2oFCkBal31V/Ws2l1VlItqAv6K" +
            "xOF01B/1QE+QG56JnOo5dZk6Ug1SO6pP1EjUCT1CpWgc8kVcHYVuIDvqjdfjjXgYnoDjcCA+gPajj2gUTsFLsDc+ghLgf2FCrVFXyDofdUCyGoN64N/wd9gF" +
            "98b78Va8HFfgiSSebCR5xELuwgu/gmPwQxSN2qMGyKrehryJqBLeyWm0Fs1B/0OTUBiqhx6rW+E/ul45qExVLaovmoeKURf8Cw7CzdBm9bDaARlwPh5HzpI7" +
            "ZD1pDufahm6pBaoZOdEGHEU00lwzaD+SKbgVIopR2eC0OE2qUHuh9+oxNU1NUfPgvP1RG+SKYlE8ngyR8sgZEknu4AAchJLU6+o6NA3ryEqSQy6SMaQUH8FJ" +
            "2A3yOFE5ziEuWjbpTYw4Q3VXmdISXmgbEqN91BStnzaK/IXH4DK0GP2KBmEjySUjte81SUsmVfAOviH1tIHaGi1He6P9pfXWdJqFWIm/FqL5aBr5SNKgCi+i" +
            "4DeQPZV0055pjWi2FqQdIF8TN+JLGsN5J5Ns4iBVxAQeWWQz+YFMIhNIGIkiJ6EKI+1INS1XWw1ez8guMp8MJtFw+yO0M5pT07QL2ggtF/6vYSSFhGlV2gTa" +
            "k1Zod7QUzUm6kse4HH4zRNK10bSMNmXv6TTqS5vQyXQlXUG30XS6lval3rRGK9IKtbawkg9WU1g8m8mimBvLpWdoNY1kc9lC1pqdoYOpVSvRGtKfaTntzr5n" +
            "Q1gQq6SP6EuaBfFi6Dz6J5VYOBvGlrNZTNBsepAmgLjDbD7rwwhdQD9of2td6Vw6iaZRAlEWsHUsiXmzG/Q5bcc2s9fMwAfyqbwOnwsZ/ehDTdVm0gLajE1l" +
            "h1g6G8M+06MQM5v+Q7/QBiwCKu3DOkF9XhCf0g9Qw2zajNrg7kZCXV1YAtvETrAMthgsF7DtbA4bCBjEeoHfYNaPhbG6LJ8uoeE0hMbTzzQastxmV9lpdoql" +
            "wl3MYdMBC9hKkGg2GuqIYaGsEevIItmPLIeVswr2AqyXQ/zWrDlrxfqyFPacWdgbQBE7D7fXhL2gD2gN7cBWsZvsFXvJLrOlcNMJbCc7zK7A/AtEKQEfxmzw" +
            "VcT2QJ5JbC3Usgn8p7CxbBnLZKWsEE5yAG5sMhsOt7oKsB3qPAaV7mL72Xo2m42EKuey3WBdyTrzVTyN/8Dr8xLIeIgdZbcYYYP4Hv4I+t+feBtYT4XTJbDH" +
            "rCNPgR6YQP97hA/mBl7AHjAjH8WX8g18PY/no/lXXMfLoMZAPoav5Qeg834E3fM2Hsdn8ImQJQ1mT6CrPsE38xV8Do/mvXkU/44nws556MEv8XM8ne/gqZAh" +
            "C/r+Ku4OPXULYRAFEC2Rz+bLwDcXcIcXQxf+D/Tp+/nvEPEVMIVi6NFt3Es4gSf8AVkT+SLAAvDZBz39fejh/+E1wCwKavv+p8AuMDeKugCdsADHeAjM4ARk" +
            "PgdfhRAjG6yuwnwLPwj5XtbiHfhbeRnUdhvsToNc5Geg9izgD38CLoPvM54PtXyC6r/wcrgtC3AanbDCtxXYggtQBoVXQ/UWzrheMKghAFhQCxEoPIQkENyw" +
            "XviIlqKHGCD6iC7Al8IAocAtvhYdYWwPvKkleDQSTUQ70Qn4Rn/ACGBb0eJbYFvfANfqDvv1gV2FA+OKFJ3BvxegswgRbSDGADFWTAB+FismixhgKv/ys6Fi" +
            "NHC1cWKqmCmmw94MGKeKiWA5Fmy+A8vJMI8DhrcCOM2PYk4tv4sGmS6WiGSxUiyE3ZkQ83uIPQZkvlgntgNf3CcOiG3QVi8Ss8F2PliuFxvEJmBEW2B9G8yW" +
            "Q9RlwJJSYO0X4JY7ANthtglm/3LMNYDVIBvBOhX2toHdRlhJhgybgavuE0fEUWCzx2DcJ3aB7x74OinOiTPiOHDcA7C2DVb3iv8DFbl+GQ==";

    private EngineSimOpenImpulseResponse() {
    }

    static double[] create(int targetSampleRate) {
        byte[] pcm = inflate(Base64.getDecoder().decode(PCM_ZLIB_BASE64));
        int sourceFrames = pcm.length / 2;
        int targetFrames = Math.max(1, (int) Math.round(
                sourceFrames * targetSampleRate / (double) SOURCE_SAMPLE_RATE));
        double[] result = new double[targetFrames];
        for (int i = 0; i < targetFrames; i++) {
            double position = i * SOURCE_SAMPLE_RATE / (double) targetSampleRate;
            int lower = Math.min(sourceFrames - 1, (int) position);
            int upper = Math.min(sourceFrames - 1, lower + 1);
            double fraction = position - lower;
            short a = sample(pcm, lower);
            short b = sample(pcm, upper);
            result[i] = (a * (1.0 - fraction) + b * fraction) / 32768.0 * 0.01;
        }
        return result;
    }

    private static short sample(byte[] pcm, int frame) {
        int offset = frame * 2;
        return (short) ((pcm[offset] & 0xFF) | (pcm[offset + 1] << 8));
    }

    private static byte[] inflate(byte[] compressed) {
        try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1_024];
            int read;
            while ((read = input.read(buffer)) >= 0) output.write(buffer, 0, read);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot decode Engine Sim sport exhaust response", exception);
        }
    }
}

