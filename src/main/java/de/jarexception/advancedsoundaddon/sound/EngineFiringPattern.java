package de.jarexception.advancedsoundaddon.sound;

import java.util.Arrays;

/** Stores an immutable four-stroke firing order and exhaust-bank routing. */
public final class EngineFiringPattern {
    private static final int MIN_CYLINDERS = 1;
    private static final int MAX_CYLINDERS = 16;

    private final int[] firingOrder;
    private final int[] firingBanks;
    private final int bankCount;

    private EngineFiringPattern(int[] firingOrder, int[] firingBanks) {
        this.firingOrder = firingOrder;
        this.firingBanks = firingBanks;
        this.bankCount = contains(firingBanks, 1) ? 2 : 1;
    }

    public static EngineFiringPattern forLayout(EngineLayout layout) {
        int[] order = layout.getFiringOrder();
        int[] banks = new int[order.length];
        for (int event = 0; event < banks.length; event++) {
            banks[event] = layout.getBankForEvent(event);
        }
        return new EngineFiringPattern(order, banks);
    }

    public static EngineFiringPattern create(int[] firingOrder, int[] firingBanks) {
        if (firingOrder == null || firingOrder.length < MIN_CYLINDERS
                || firingOrder.length > MAX_CYLINDERS) {
            throw new IllegalArgumentException("FiringOrder must contain between "
                    + MIN_CYLINDERS + " and " + MAX_CYLINDERS + " cylinders");
        }
        if (firingBanks == null || firingBanks.length != firingOrder.length) {
            throw new IllegalArgumentException("FiringBanks must contain exactly one bank for each firing event");
        }

        boolean[] seen = new boolean[firingOrder.length + 1];
        for (int cylinder : firingOrder) {
            if (cylinder < 1 || cylinder > firingOrder.length || seen[cylinder]) {
                throw new IllegalArgumentException("FiringOrder must be a permutation of 1 through "
                        + firingOrder.length);
            }
            seen[cylinder] = true;
        }
        for (int bank : firingBanks) {
            if (bank != 0 && bank != 1) {
                throw new IllegalArgumentException("FiringBanks supports only bank 0 and bank 1");
            }
        }
        if (!contains(firingBanks, 0)) {
            throw new IllegalArgumentException("FiringBanks must use bank 0 before bank 1");
        }
        return new EngineFiringPattern(Arrays.copyOf(firingOrder, firingOrder.length),
                Arrays.copyOf(firingBanks, firingBanks.length));
    }

    public int getCylinderCount() {
        return firingOrder.length;
    }

    public int getBankCount() {
        return bankCount;
    }

    public int[] getFiringOrder() {
        return Arrays.copyOf(firingOrder, firingOrder.length);
    }

    public int[] getFiringBanks() {
        return Arrays.copyOf(firingBanks, firingBanks.length);
    }

    public int getBankForEvent(int eventIndex) {
        return firingBanks[eventIndex % firingBanks.length];
    }

    private static boolean contains(int[] values, int expected) {
        for (int value : values) {
            if (value == expected) {
                return true;
            }
        }
        return false;
    }
}
