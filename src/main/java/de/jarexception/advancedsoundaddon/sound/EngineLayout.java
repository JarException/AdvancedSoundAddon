package de.jarexception.advancedsoundaddon.sound;

import java.util.Arrays;

/** Defines four-stroke firing orders and exhaust-bank routing. */
public enum EngineLayout {
    I1(new int[]{1}, new int[]{0}),
    I3(new int[]{1, 2, 3}, new int[]{0, 0, 0}),
    I4(new int[]{1, 3, 4, 2}, new int[]{0, 0, 0, 0}),
    I5(new int[]{1, 2, 4, 5, 3}, new int[]{0, 0, 0, 0, 0}),
    I6(new int[]{1, 5, 3, 6, 2, 4}, new int[]{0, 0, 0, 0, 0, 0}),
    V6(new int[]{1, 4, 2, 5, 3, 6}, new int[]{0, 1, 0, 1, 0, 1}),
    FLAT6(new int[]{1, 6, 2, 4, 3, 5}, new int[]{0, 1, 0, 1, 0, 1}),
    V8_CROSSPLANE(new int[]{1, 8, 4, 3, 6, 5, 7, 2}, new int[]{0, 1, 1, 0, 1, 0, 0, 1}),
    V8_FLATPLANE(new int[]{1, 5, 3, 7, 4, 8, 2, 6}, new int[]{0, 1, 0, 1, 0, 1, 0, 1}),
    V10(new int[]{1, 6, 5, 10, 2, 7, 3, 8, 4, 9}, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1}),
    V12(new int[]{1, 7, 5, 11, 3, 9, 6, 12, 2, 8, 4, 10}, new int[]{0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1}),
    W16(new int[]{1, 14, 9, 4, 7, 12, 15, 6, 13, 8, 3, 16, 11, 2, 5, 10},
            new int[]{0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0});

    private final int[] firingOrder;
    private final int[] firingBanks;

    EngineLayout(int[] firingOrder, int[] firingBanks) {
        this.firingOrder = firingOrder;
        this.firingBanks = firingBanks;
    }

    public int getCylinderCount() {
        return firingOrder.length;
    }

    public int getBankCount() {
        for (int bank : firingBanks) {
            if (bank != 0) {
                return 2;
            }
        }
        return 1;
    }

    public int[] getFiringOrder() {
        return Arrays.copyOf(firingOrder, firingOrder.length);
    }

    public int getBankForEvent(int eventIndex) {
        return firingBanks[eventIndex % firingBanks.length];
    }
}
