package com.example.hubmod.util;

public final class PortDebounce {

    private final int stableTicksNeeded;

    private int lastRaw = Integer.MIN_VALUE;
    private int stable = 0;
    private int sameCount = 0;
    private boolean changed = false;

    public PortDebounce(int stableTicksNeeded) {
        this.stableTicksNeeded = Math.max(1, stableTicksNeeded);
    }

    /**
     * @return true if stable value changed
     */
    public boolean update(int raw) {
        changed = false;

        if (raw != lastRaw) {
            lastRaw = raw;
            sameCount = 1;
            return false;
        }

        sameCount++;

        if (sameCount >= stableTicksNeeded && stable != raw) {
            stable = raw;
            changed = true;
        }

        return changed;
    }

    public int getStableValue() {
        return stable;
    }
}
