package de.jarexception.advancedsoundaddon.sound;

/** Manual numerical probe, excluded from the production JAR. */
public final class FlowModelProbe {
    private FlowModelProbe() {
    }

    public static void main(String[] args) {
        EngineLayout layout = EngineLayout.valueOf(args[0]);
        double rpm = Double.parseDouble(args[1]);
        CylinderFlowModel model = new CylinderFlowModel(layout, 48_000);
        double minimum = Double.POSITIVE_INFINITY;
        double maximum = Double.NEGATIVE_INFINITY;
        double sum = 0.0;
        double square = 0.0;
        for (int sample = 0; sample < 48_000 * 3; sample++) {
            model.step(rpm, 0.72, 0.78, 1.0, false, false);
            double value = model.getBankFlow(0);
            if (sample >= 48_000) {
                minimum = Math.min(minimum, value);
                maximum = Math.max(maximum, value);
                sum += value;
                square += value * value;
            }
        }
        int count = 48_000 * 2;
        System.out.println("min=" + minimum + " max=" + maximum
                + " mean=" + sum / count + " rms=" + Math.sqrt(square / count));
    }
}
